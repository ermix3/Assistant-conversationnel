package ai.factory.agent_fitness.leads;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CallbackServiceTests {
    @Test void rejectsMalformedPersonalDataWithoutWriting() {
        var repository=mock(CallbackRepository.class);
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var service=new CallbackService(repository,factory.getValidator());
            for(var input:java.util.List.of(new CallbackInput("Test","invalid","fr",true),new CallbackInput("Test","0600000000","fr",false),new CallbackInput("x".repeat(101),"0600000000","en",true),new CallbackInput("Test","0600000000","de",true))) {
                assertThatThrownBy(()->service.record(input)).isInstanceOf(IllegalArgumentException.class);
            }
            verifyNoInteractions(repository);
        }
    }
}
