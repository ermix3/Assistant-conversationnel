package ai.factory.agent_fitness.config;

import ai.factory.agent_fitness.agent.ClubTools;
import ai.factory.agent_fitness.agent.OfficialInformation;
import ai.factory.agent_fitness.chat.ChatService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatConfigurationTests {
    @Test
    void keepsOriginalQuestionAfterForeignLanguageContextAndStoresOnlyCustomerText() throws Exception {
        var model = mock(ChatModel.class);
        when(model.getOptions()).thenReturn(ChatOptions.builder().build());
        when(model.call(any(Prompt.class))).thenReturn(new org.springframework.ai.chat.model.ChatResponse(
                List.of(new Generation(new AssistantMessage("The club offers a spa.")))));
        var store = mock(VectorStore.class);
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("Le club propose un spa.")));
        var tools = mock(ClubTools.class);
        when(tools.getOfficialInformation()).thenReturn(new OfficialInformation(List.of(), List.of(), List.of()));
        var configuration = new ChatConfiguration();
        var memory = configuration.chatMemory();
        var client = configuration.clubChatClient(ChatClient.builder(model), tools, memory, store,
                new ClassPathResource("prompts/system.txt"));
        String question = "What spa experiences does Escape offer?";

        new ChatService(client, tools).reply("session", question);

        var search = ArgumentCaptor.forClass(SearchRequest.class);
        verify(store).similaritySearch(search.capture());
        assertThat(search.getValue().getQuery()).isEqualTo(question);
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model).call(prompt.capture());
        assertThat(prompt.getValue().getUserMessage().getText())
                .contains("<descriptive_context>Le club propose un spa.</descriptive_context>")
                .endsWith("<customer_question>" + question + "</customer_question>\n");
        assertThat(memory.get("session").getFirst().getText()).isEqualTo(question);
    }
}
