package ai.factory.agent_fitness.chat;
import ai.factory.agent_fitness.leads.CallbackService;
import ai.factory.agent_fitness.config.ApiSecurityFilter;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ChatControllerTests {
    ChatService chat;CallbackService callbacks;MockMvc mvc;
    @BeforeEach void setup() {
        chat=mock(ChatService.class);callbacks=mock(CallbackService.class);
        mvc=MockMvcBuilders.standaloneSetup(new ChatController(chat,callbacks)).setControllerAdvice(new ApiExceptionHandler())
            .addFilters(new ApiSecurityFilter("https://www.escapehealthclub.com")).build();
    }
    @Test void usesCookieSessionRatherThanSuppliedId() throws Exception {
        var session=new MockHttpSession();when(chat.reply(session.getId(),"Bonjour")).thenReturn("Bonjour");
        mvc.perform(post("/api/chat").session(session).contentType("application/json").content("{\"sessionId\":\"victim\",\"message\":\"Bonjour\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sessionId").value(session.getId()));
        verify(chat).reply(session.getId(),"Bonjour");
    }
    @Test void rejectsOversizedAndInvalidInput() throws Exception {
        mvc.perform(post("/api/chat").contentType("application/json").content("{\"message\":\""+"a".repeat(2001)+"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/chat").contentType("application/json").content("{\"message\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/chat").contentType("application/json").content(" ".repeat(17000))).andExpect(status().isPayloadTooLarge());
        verifyNoInteractions(chat,callbacks);
    }
    @Test void blocksUntrustedOriginAndLimitsIp() throws Exception {
        mvc.perform(post("/api/chat").header("Origin","https://evil.example").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        for(int i=0;i<20;i++)mvc.perform(post("/api/chat").contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/chat").contentType("application/json").content("{}")).andExpect(status().isTooManyRequests());
        verifyNoInteractions(chat,callbacks);
    }
    @Test void acceptsSameOriginBrowserPostsOnBothLocalHostnames() throws Exception {
        when(chat.reply(anyString(),eq("Bonjour"))).thenReturn("Bonjour");
        for (String host : java.util.List.of("127.0.0.1", "localhost")) {
            mvc.perform(post("http://" + host + ":8080/api/chat")
                    .header("Origin", "http://" + host + ":8080")
                    .contentType("application/json").content("{\"message\":\"Bonjour\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Bonjour"));
        }
        verify(chat,times(2)).reply(anyString(),eq("Bonjour"));
    }
    @Test void stillRejectsDifferentPortsSchemesHostsAndMalformedOrigins() throws Exception {
        for (String origin : java.util.List.of("http://127.0.0.1:8081", "https://127.0.0.1:8080",
                "http://localhost:8080", "https://evil.example", "null", "http://[invalid")) {
            mvc.perform(post("http://127.0.0.1:8080/api/chat").header("Origin", origin)
                    .contentType("application/json").content("{\"message\":\"Bonjour\"}"))
                .andExpect(status().isForbidden());
        }
        verifyNoInteractions(chat,callbacks);
    }
    @Test void acceptsConfiguredClubOrigin() throws Exception {
        when(chat.reply(anyString(),eq("Bonjour"))).thenReturn("Bonjour");
        mvc.perform(post("http://127.0.0.1:8080/api/chat")
                .header("Origin", "https://www.escapehealthclub.com")
                .contentType("application/json").content("{\"message\":\"Bonjour\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://www.escapehealthclub.com"));
    }
    @Test void doesNotExposeExceptionOrSubmittedData() throws Exception {
        when(chat.reply(anyString(),anyString())).thenThrow(new IllegalStateException("private diagnostic"));
        mvc.perform(post("/api/chat").contentType("application/json").content("{\"message\":\"Bonjour\"}"))
            .andExpect(status().isServiceUnavailable()).andExpect(content().json("{\"code\":\"SERVICE_UNAVAILABLE\"}"));
    }
}
