package ai.factory.agent_fitness.leads;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "club.notifications.enabled", havingValue = "true")
public class CallbackNotifier {
    private final JdbcClient jdbc;
    private final JavaMailSender mail;
    private final String recipient;
    private final String sender;

    public CallbackNotifier(JdbcClient jdbc, JavaMailSender mail,
                            @Value("${club.notifications.to}") String recipient,
                            @Value("${club.notifications.from}") String sender) {
        this.jdbc = jdbc;
        this.mail = mail;
        this.recipient = recipient;
        this.sender = sender;
    }

    @Scheduled(fixedDelayString = "${club.notifications.interval-ms:60000}")
    @Transactional
    public void notifyStaff() {
        var ids = jdbc.sql("select id from callback_requests where notified_at is null order by created_at limit 50 for update skip locked")
                .query(java.util.UUID.class).list();
        if (ids.isEmpty()) return;
        var message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject("Escape: callback requests pending");
        // Personal data stays in the database. Staff use their authorized database access.
        message.setText("New callback requests are waiting in the club database. Please review the callback_requests queue.");
        mail.send(message);
        for (var id : ids)
            jdbc.sql("update callback_requests set notified_at=current_timestamp where id=:id").param("id", id).update();
    }
}
