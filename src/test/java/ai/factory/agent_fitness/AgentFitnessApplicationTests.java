package ai.factory.agent_fitness;
import ai.factory.agent_fitness.agent.ClubTools;
import ai.factory.agent_fitness.knowledge.IngestionService;
import ai.factory.agent_fitness.pricing.PriceRepository;
import org.junit.jupiter.api.*;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties={"spring.docker.compose.enabled=false","club.knowledge.ingest=false"})
class AgentFitnessApplicationTests {
    @Autowired ClubTools tools;
    @Autowired PriceRepository prices;
    @Autowired IngestionService ingestion;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean EmbeddingModel embeddings;
    @BeforeEach void fakeEmbeddings() {
        when(embeddings.embed(anyList(),any(),any())).thenAnswer(invocation -> {
            List<Document> docs=invocation.getArgument(0);
            return docs.stream().map(d -> {float[] vector=new float[1024];vector[0]=1;return vector;}).toList();
        });
    }
    @Test void migrationsAndDatabaseTools() {
        assertThat(prices.count()).isEqualTo(22);
        assertThat(tools.getPrice("membership_monthly")).contains("2000.00 dh");
        assertThat(tools.getPrice("missing")).isEqualTo("PRICE_UNAVAILABLE");
        assertThat(tools.getOpeningHours()).isEmpty();
        assertThat(tools.getContact()).hasSize(2);
    }
    @Test void ingestionReplacesCorpusAndRollsBackOnEmbeddingFailure() throws Exception {
        int count=ingestion.ingest();
        assertThat(count).isPositive();
        assertThat(ingestion.ingest()).isEqualTo(count);
        assertThat(jdbc.queryForObject("select count(*) from vector_store",Integer.class)).isEqualTo(count);
        when(embeddings.embed(anyList(),any(),any())).thenThrow(new IllegalStateException("embedding unavailable"));
        assertThatThrownBy(()->ingestion.ingest()).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select count(*) from vector_store",Integer.class)).isEqualTo(count);
    }
}
