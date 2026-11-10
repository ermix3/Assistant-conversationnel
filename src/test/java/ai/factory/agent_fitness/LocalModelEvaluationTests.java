package ai.factory.agent_fitness;
import ai.factory.agent_fitness.agent.ClubTools;
import ai.factory.agent_fitness.chat.ChatService;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
/** Opt-in behavioral evaluation using only the installed local models, never a hosted LLM. */
@EnabledIfSystemProperty(named="escape.eval",matches="true")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties={"spring.docker.compose.enabled=false","club.knowledge.ingest=true",
    "spring.ai.ollama.base-url=http://localhost:11434","spring.ai.ollama.chat.model=qwen3.5:latest","spring.ai.ollama.embedding.model=bge-m3"})
class LocalModelEvaluationTests {
    @Autowired ChatService chat;
    @MockitoSpyBean ClubTools tools;
    @ParameterizedTest(name="{0}")
    @CsvFileSource(resources="/eval/questions.csv",delimiter='|',numLinesToSkip=1)
    void evaluatesBehavior(String id,String question,String tool,String expected,String forbidden) {
        clearInvocations(tools);
        String answer=chat.reply(UUID.randomUUID().toString(),question);
        assertThat(answer).as(id).containsPattern("(?isu)"+expected);
        if(forbidden!=null && !forbidden.isBlank()) assertThat(answer).as(id+" unsafe content").doesNotContainPattern("(?isu)"+forbidden);
        switch(tool) {
            case "price" -> verify(tools,atLeastOnce()).getOfficialInformation();
            case "contact" -> verify(tools,atLeastOnce()).getContact();
            case "hours" -> {verify(tools,atLeastOnce()).getOpeningHours();verify(tools,atLeastOnce()).getContact();}
            case "none" -> verify(tools,atLeastOnce()).getOfficialInformation();
            default -> throw new IllegalArgumentException("Unknown evaluation tool");
        }
    }
}
