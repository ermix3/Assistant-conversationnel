package ai.factory.agent_fitness.chat;

import ai.factory.agent_fitness.agent.ClubTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatClient client;
    private final ClubTools tools;

    public String reply(String conversationId, String message) {
        String reply = client.prompt()
                .system(s -> s.param("official_data", tools.getOfficialInformation().toString())).user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId)).call().content();
        if (reply == null || reply.isBlank()) throw new IllegalStateException("Empty model response");
        return reply.replaceAll("(?iu)\\b(?:dirhams?|dhs?|mad)\\b|درهم(?:ا|اً)?|دراهم", "dh");
    }
}
