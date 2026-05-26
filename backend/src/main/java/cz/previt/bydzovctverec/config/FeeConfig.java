package cz.previt.bydzovctverec.config;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeeConfig {

  public record VariantFee(int baseDo1945, int baseOd1946, int extraPerson) {}

  @Bean
  public Map<String, VariantFee> feeSchedule() {
    return Map.of(
        "JEDNODENNI", new VariantFee(500, 800, 500),
        "DVODENNI_UZAVRENO", new VariantFee(1000, 1200, 1000),
        "DVODENNI_BEZ_UBYTOVANI", new VariantFee(600, 900, 600));
  }
}
