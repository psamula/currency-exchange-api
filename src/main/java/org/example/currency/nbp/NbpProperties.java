package org.example.currency.nbp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "nbp")
public record NbpProperties(
        @DefaultValue("https://api.nbp.pl/api") String baseUrl,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout
) {
}
