package ai.factory.agent_fitness.leads;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CallbackInput(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[^\\p{Cntrl}]+") String name,
        @NotBlank @Pattern(regexp = "(?:0[5-7][0-9]{8}|\\+212[5-7][0-9]{8})") String phone,
        @NotBlank @Pattern(regexp = "fr|en|ar") String language,
        @AssertTrue boolean consent) {
}
