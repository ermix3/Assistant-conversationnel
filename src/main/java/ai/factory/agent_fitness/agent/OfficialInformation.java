package ai.factory.agent_fitness.agent;

import java.util.List;

public record OfficialInformation(List<OfficialPrice> prices, List<Fact> contact, List<Fact> hours) {
}