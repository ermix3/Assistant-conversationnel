package ai.factory.agent_fitness.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded ephemeral history, never persisted or logged.
 */
public class ExpiringChatMemory implements ChatMemory {
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    private record Entry(List<Message> messages, Instant touched) {
    }

    private void purge() {
        entries.entrySet().removeIf(e -> e.getValue().touched().isBefore(Instant.now().minus(Duration.ofMinutes(30))));
    }

    @Override
    public synchronized void add(String id, List<Message> messages) {
        purge();
        var all = new ArrayList<>(get(id));
        all.addAll(messages);
        entries.remove(id);
        entries.put(id, new Entry(List.copyOf(all.subList(Math.max(0, all.size() - 20), all.size())), Instant.now()));
        while (entries.size() > 1000) entries.remove(entries.keySet().iterator().next());
    }

    @Override
    public synchronized List<Message> get(String id) {
        purge();
        var e = entries.get(id);
        return e == null ? List.of() : e.messages();
    }

    @Override
    public synchronized void clear(String id) {
        entries.remove(id);
    }
}
