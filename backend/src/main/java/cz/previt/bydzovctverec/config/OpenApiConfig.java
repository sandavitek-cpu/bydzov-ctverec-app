package cz.previt.bydzovctverec.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Novobydžovský čtverec API")
            .description("API pro správu motoristické soutěže historických vozidel")
            .version("0.4.0")
            .contact(new Contact()
                .name("Vít Šanda")
                .email("info@bydzov-ctverec.cz")
                .url("https://bydzov-ctverec.cz"))
            .license(new License()
                .name("MIT")
                .url("https://opensource.org/licenses/MIT")));
  }
}
