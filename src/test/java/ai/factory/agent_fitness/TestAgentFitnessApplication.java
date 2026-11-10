package ai.factory.agent_fitness;

import org.springframework.boot.SpringApplication;

public class TestAgentFitnessApplication {

	public static void main(String[] args) {
		SpringApplication.from(AgentFitnessApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
