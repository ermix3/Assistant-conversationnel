package ai.factory.agent_fitness.leads;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "callback_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallbackRequest {
    @Getter
    @Id
    private UUID id;
    private String name;
    private String phone;
    private String language;
    private Instant consentAt;
    private Instant createdAt;
    private Instant notifiedAt;

    public CallbackRequest(CallbackInput input) {
        id = UUID.randomUUID();
        name = input.name().strip();
        phone = input.phone();
        language = input.language();
        createdAt = Instant.now();
        consentAt = createdAt;
    }
}
