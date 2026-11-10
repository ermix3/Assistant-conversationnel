package ai.factory.agent_fitness.leads;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CallbackService {
    private final CallbackRepository repository;
    private final Validator validator;

    @Transactional
    public UUID record(CallbackInput input) {
        if (input == null || !validator.validate(input).isEmpty())
            throw new IllegalArgumentException("Invalid callback request");
        return repository.save(new CallbackRequest(input)).getId();
    }
}
