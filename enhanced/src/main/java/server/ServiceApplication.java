package server;

import io.micrometer.core.instrument.Tags;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsContributor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.SpanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.HandlerMapping;

import java.util.Map;
import java.util.Set;

@Slf4j
@SpringBootApplication
public class ServiceApplication {

    @Bean
    WebFluxTagsContributor consoleTagContributor() {
        return (exchange, ex) -> {
            var console = "UNKNOWN";
            var consolePathVariable = ((Map<String,String>) exchange.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).get("console");
            if (AvailabilityController.validateConsole(consolePathVariable)) {
                console = consolePathVariable;
            }
            return Tags.of("console", console);
        };
    }

    public static void main(String[] args) {
        log.info("starting server");
        SpringApplication.run(ServiceApplication.class, args);
    }
}

@RestController
@AllArgsConstructor
class AvailabilityController {

    private final SpanCustomizer spanCustomizer;

    @GetMapping("/availability/{console}")
    Map<String, Object> getAvailability(@PathVariable String console) {
        Assert.state(validateConsole(console), () -> "the console specified, " + console + ", is not valid.");
        this.spanCustomizer.tag("console", console);
        return Map.of("console", console, "available", checkAvailability(console));
    }

    private boolean checkAvailability(String console) {
        return switch (console) {
            case "ps5" -> throw new RuntimeException("Service exception");
            case "xbox" -> true;
            default -> false;
        };
    }

    static boolean validateConsole(String console) {
        return StringUtils.hasText(console) &&
               Set.of("ps5", "ps4", "switch", "xbox").contains(console);
    }

}
