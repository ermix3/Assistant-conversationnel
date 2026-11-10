package ai.factory.agent_fitness.config;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import static org.assertj.core.api.Assertions.*;
class ExpiringChatMemoryTests {
    @Test void boundsHistoryAndIsolatesSessions() {
        var memory=new ExpiringChatMemory();
        for(int i=0;i<30;i++)memory.add("one",java.util.List.of(new UserMessage("Message "+i)));
        assertThat(memory.get("one")).hasSize(20);
        assertThat(memory.get("two")).isEmpty();
        memory.clear("one");assertThat(memory.get("one")).isEmpty();
    }
}
