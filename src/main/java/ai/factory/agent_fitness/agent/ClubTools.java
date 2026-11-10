package ai.factory.agent_fitness.agent;

import ai.factory.agent_fitness.pricing.PriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClubTools {
    private final PriceRepository prices;
    private final JdbcClient jdbc;

    @Tool(description = "List official service codes and labels available for price lookup. No prices are inferred.")
    public List<Service> listServices() {
        return prices.findAll().stream().map(p -> new Service(p.getServiceCode(), p.getLabel())).toList();
    }

    @Tool(description = "Get the official database price in dh for an exact service code from listServices. Missing means unavailable; never guess a price.")
    public String getPrice(String serviceCode) {
        if (serviceCode == null || !serviceCode.matches("[a-z0-9_-]{1,80}")) return "PRICE_UNAVAILABLE";
        return prices.findById(serviceCode).map(p -> p.getLabel() + ": " + p.getAmount().toPlainString() + " dh / " + p.getUnit()).orElse("PRICE_UNAVAILABLE");
    }

    @Tool(description = "Get official staff phone and email from the database for human handoff, unknown facts or medical questions.")
    public List<Fact> getContact() {
        return facts("contact");
    }

    @Tool(description = "Get official club opening hours from the database. Empty means hours are unavailable; offer staff contact.")
    public List<Fact> getOpeningHours() {
        return facts("hours");
    }

    @Tool(description = "Get the complete current official database prices, staff contact and opening hours. Missing values are unavailable, never estimated.")
    public OfficialInformation getOfficialInformation() {
        var currentPrices = prices.findAll().stream()
                .map(p -> new OfficialPrice(p.getServiceCode(), p.getLabel(), p.getAmount().toPlainString(), "dh", p.getUnit())).toList();
        return new OfficialInformation(currentPrices, getContact(), getOpeningHours());
    }

    private List<Fact> facts(String category) {
        return jdbc.sql("select fact_key, fact_value from club_facts where category = :category order by fact_key")
                .param("category", category).query((rs, n) -> new Fact(rs.getString(1), rs.getString(2))).list();
    }
}
