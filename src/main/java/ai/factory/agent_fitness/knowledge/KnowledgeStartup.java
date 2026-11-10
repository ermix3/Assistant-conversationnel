package ai.factory.agent_fitness.knowledge;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "club.knowledge.ingest", havingValue = "true")
@RequiredArgsConstructor
public class KnowledgeStartup implements ApplicationRunner {
    private final IngestionService ingestion;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ingestion.ingest();
    }
}
