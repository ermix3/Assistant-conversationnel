package ai.factory.agent_fitness.pricing;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "prices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Price {
    @Id
    private String serviceCode;
    private String label;
    private BigDecimal amount;
    private String unit;
}
