package ai.factory.agent_fitness.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatServiceTests {
    @Test
    void rendersOfficialDataInProductionPrompt() throws Exception {
        ChatModel model = mock(ChatModel.class);
        when(model.getOptions()).thenReturn(org.springframework.ai.chat.prompt.ChatOptions.builder().build());
        when(model.call(any(Prompt.class))).thenReturn(new org.springframework.ai.chat.model.ChatResponse(
                List.of(new Generation(new AssistantMessage("Response")))));
        var tools = mock(ai.factory.agent_fitness.agent.ClubTools.class);
        var information = new ai.factory.agent_fitness.agent.OfficialInformation(List.of(),
                List.of(new ai.factory.agent_fitness.agent.Fact("email", "staff@example.invalid")), List.of());
        when(tools.getOfficialInformation()).thenReturn(information);
        var promptText = new org.springframework.core.io.ClassPathResource("prompts/system.txt")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var service = new ChatService(ChatClient.builder(model).defaultSystem(promptText).build(), tools);

        service.reply("session", "How can I contact the club?");

        var prompt = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(model).call(prompt.capture());
        assertThat(prompt.getValue().getSystemMessage().getText())
                .contains(information.toString()).doesNotContain("{official_data}");
    }

    @Test
    void sendsUserMessageAndReturnsModelResponse() {
        ChatModel model = mock(ChatModel.class);
        when(model.getOptions()).thenReturn(org.springframework.ai.chat.prompt.ChatOptions.builder().build());
        when(model.call(any(Prompt.class))).thenReturn(new org.springframework.ai.chat.model.ChatResponse(List.of(new Generation(new AssistantMessage("Prix en dirhams / درهم / MAD")))));
        var tools = mock(ai.factory.agent_fitness.agent.ClubTools.class);
        when(tools.getOfficialInformation()).thenReturn(new ai.factory.agent_fitness.agent.OfficialInformation(List.of(), List.of(), List.of()));
        var service = new ChatService(ChatClient.builder(model).defaultSystem("{official_data}").build(), tools);
        assertThat(service.reply("session", "Quels services proposez-vous ?")).isEqualTo("Prix en dh / dh / dh");
        verify(tools).getOfficialInformation();
        var prompt = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(model).call(prompt.capture());
        assertThat(prompt.getValue().getUserMessage().getText()).isEqualTo("Quels services proposez-vous ?");
    }
}
