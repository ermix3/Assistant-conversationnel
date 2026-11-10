package ai.factory.agent_fitness.knowledge;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class IngestionService {
    private final VectorStore store;
    private final KnowledgeLoader loader;
    private final JdbcTemplate jdbc;

    @Transactional
    public int ingest() throws IOException {
        // Serialize replacement across app instances; rollback preserves the previous corpus on embedding failure.
        jdbc.execute("select pg_advisory_xact_lock(73482101)");
        var documents = new ArrayList<Document>();
        for (var resource : new PathMatchingResourcePatternResolver().getResources("classpath*:knowledge/*.md"))
            documents.addAll(loader.load(resource));
        if (documents.isEmpty()) throw new IllegalStateException("Knowledge corpus is empty");
        store.delete("corpus == 'escape'");
        for (int start = 0; start < documents.size(); start += 16)
            store.add(documents.subList(start, Math.min(start + 16, documents.size())));
        return documents.size();
    }
}
