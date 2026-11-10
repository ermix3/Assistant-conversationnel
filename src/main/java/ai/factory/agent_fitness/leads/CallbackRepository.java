package ai.factory.agent_fitness.leads;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CallbackRepository extends JpaRepository<CallbackRequest, UUID> {
}
