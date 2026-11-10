package ai.factory.agent_fitness.agent;

import ai.factory.agent_fitness.pricing.Price;
import ai.factory.agent_fitness.pricing.PriceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ClubToolsTests {
    @Test
    void invalidCodesCannotReachTheDatabase() {
        var prices = mock(PriceRepository.class);
        var jdbc = mock(JdbcClient.class);
        var tools = new ClubTools(prices, jdbc);
        for (String code : List.of("../internal", "service' OR '1'='1", "x".repeat(81), "")) {
            assertThat(tools.getPrice(code)).isEqualTo("PRICE_UNAVAILABLE");
        }
        assertThat(tools.getPrice(null)).isEqualTo("PRICE_UNAVAILABLE");
        verifyNoInteractions(prices, jdbc);
    }

    @Test
    void snapshotContainsCurrentRepositoryValuesAndDatabaseFacts() {
        var prices = mock(PriceRepository.class);
        var price = mock(Price.class);
        when(price.getServiceCode()).thenReturn("test_service");
        when(price.getLabel()).thenReturn("Synthetic service");
        when(price.getAmount()).thenReturn(new BigDecimal("9.99"));
        when(price.getUnit()).thenReturn("session");
        when(prices.findAll()).thenReturn(List.of(price));
        var tools = spy(new ClubTools(prices, mock(JdbcClient.class)));
        doReturn(List.of(new Fact("email", "staff@example.invalid"))).when(tools).getContact();
        doReturn(List.of()).when(tools).getOpeningHours();
        var information = tools.getOfficialInformation();
        assertThat(information.prices()).containsExactly(new OfficialPrice("test_service", "Synthetic service", "9.99", "dh", "session"));
        assertThat(information.contact()).containsExactly(new Fact("email", "staff@example.invalid"));
        assertThat(information.hours()).isEmpty();
        verify(prices).findAll();
    }
}
