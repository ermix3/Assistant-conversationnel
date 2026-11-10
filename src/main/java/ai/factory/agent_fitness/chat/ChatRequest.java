package ai.factory.agent_fitness.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(@Size(max = 100) String sessionId, @NotBlank @Size(max = 2000) String message) {
}
