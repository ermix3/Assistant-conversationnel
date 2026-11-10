package ai.factory.agent_fitness.chat;

import ai.factory.agent_fitness.leads.CallbackInput;
import ai.factory.agent_fitness.leads.CallbackService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chat;
    private final CallbackService callbacks;

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, HttpSession session) {
        // The server cookie owns history; a client-supplied sessionId never grants access.
        synchronized (session) {
            return new ChatResponse(session.getId(), chat.reply(session.getId(), request.message()));
        }
    }

    @PostMapping("/callbacks")
    @ResponseStatus(HttpStatus.CREATED)
    public CallbackReceipt callback(@Valid @RequestBody CallbackInput input) {
        return new CallbackReceipt(callbacks.record(input), "CALLBACK_REQUESTED");
    }

    public record CallbackReceipt(UUID id, String status) {
    }
}
