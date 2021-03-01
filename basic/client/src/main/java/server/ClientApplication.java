package client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        log.info("starting client");
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> ready(AvailabilityClient client) {
        return applicationReadyEvent -> {
            for (var console : "ps5,xbox,ps4,switch".split(",")) {
                Flux.range(0, 20).delayElements(Duration.ofMillis(100)).subscribe(i ->
                        client
                                .checkAvailability(console)
                                .subscribe(availability ->
                                        log.info("console: {}, availability: {} ", console, availability.isAvailable())));
            }
        };
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Availability {
    private boolean available;
    private String console;
}

@Component
@RequiredArgsConstructor
class AvailabilityClient {

    private final WebClient webClient;
    private static final String URI = "http://localhost:8083/availability/{console}";

    Mono<Availability> checkAvailability(String console) {
        return this.webClient
                .get()
                .uri(URI, console)
                .retrieve()
                .bodyToMono(Availability.class)
                .onErrorReturn(new Availability(false, console));
    }

}
