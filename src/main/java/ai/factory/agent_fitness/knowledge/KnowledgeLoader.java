package ai.factory.agent_fitness.knowledge;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class KnowledgeLoader {
    public List<Document> load(Resource resource) throws IOException {
        String text = resource.getContentAsString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        if (!text.startsWith("---\n")) throw new IllegalArgumentException("Missing knowledge front matter");
        int end = text.indexOf("\n---\n", 4);
        if (end < 0) throw new IllegalArgumentException("Invalid knowledge front matter");
        var language = java.util.regex.Pattern.compile("(?m)^language: (fr|en|ar)$").matcher(text.substring(4, end));
        if (!language.find()) throw new IllegalArgumentException("Missing knowledge language");
        var result = new ArrayList<Document>();
        int index = 0;
        for (String part : text.substring(end + 5).split("(?m)(?=^#{1,6} )")) {
            if (part.isBlank()) continue;
            // Split oversized headings too, keeping embedding requests bounded.
            for (int offset = 0; offset < part.length(); offset += 1800) {
                String chunk = part.substring(offset, Math.min(part.length(), offset + 1800)).strip();
                String id = UUID.nameUUIDFromBytes((resource.getFilename() + ":" + index++).getBytes(StandardCharsets.UTF_8)).toString();
                result.add(new Document(id, chunk, Map.of("source", Objects.requireNonNull(resource.getFilename()), "language", language.group(1), "corpus", "escape")));
            }
        }
        return result;
    }
}
