package ai.factory.agent_fitness.knowledge;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import static org.assertj.core.api.Assertions.*;
class KnowledgeLoaderTests {
    @Test void assignsStableIdsAndLanguage() throws Exception {
        var loader=new KnowledgeLoader();var resource=new ClassPathResource("knowledge/fitness.md");
        var docs=loader.load(resource);
        assertThat(docs).isNotEmpty();
        assertThat(docs.getFirst().getId()).isEqualTo(loader.load(resource).getFirst().getId());
        assertThat(docs.getFirst().getMetadata()).containsEntry("language","fr").containsEntry("corpus","escape");
    }
}
