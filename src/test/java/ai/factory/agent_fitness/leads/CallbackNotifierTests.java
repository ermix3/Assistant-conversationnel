package ai.factory.agent_fitness.leads;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CallbackNotifierTests {
    @Test
    void injectsConfiguredMailAddressesThroughGeneratedConstructor() {
        var jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        var mail = mock(JavaMailSender.class);
        when(jdbc.sql(anyString()).query(UUID.class).list()).thenReturn(List.of(UUID.randomUUID()));

        new ApplicationContextRunner()
                .withUserConfiguration(CallbackNotifier.class)
                .withBean(JdbcClient.class, () -> jdbc)
                .withBean(JavaMailSender.class, () -> mail)
                .withPropertyValues("club.notifications.enabled=true",
                        "club.notifications.to=staff@example.invalid",
                        "club.notifications.from=assistant@example.invalid")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(CallbackNotifier.class);
                    context.getBean(CallbackNotifier.class).notifyStaff();

                    var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
                    verify(mail).send(message.capture());
                    assertThat(message.getValue().getTo()).containsExactly("staff@example.invalid");
                    assertThat(message.getValue().getFrom()).isEqualTo("assistant@example.invalid");
                });
    }
}
