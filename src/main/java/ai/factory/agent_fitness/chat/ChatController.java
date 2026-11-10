package ai.factory.agent_fitness.chat;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chat;

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, HttpSession session) {
        // The server cookie owns history; a client-supplied sessionId never grants access.
        synchronized (session) {
            return new ChatResponse(session.getId(), chat.reply(session.getId(), request.message()));
        }
    }

}
