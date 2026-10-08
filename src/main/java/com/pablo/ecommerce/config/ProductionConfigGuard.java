package com.pablo.ecommerce.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("prod")
public class ProductionConfigGuard {
    @Bean
    ApplicationRunner validarProducao(Environment env) {
        return args -> {
            if (env.matchesProfiles("demo")) throw new IllegalStateException("Perfis demo e prod nao podem ser combinados");
            for (String chave : new String[]{"app.stripe.secret-key", "app.stripe.webhook-secret",
                    "app.email.from", "app.resend.api-key",
                    "app.digital.storage-path"}) {
                if (env.getRequiredProperty(chave).isBlank()) {
                    throw new IllegalStateException("Configuracao obrigatoria ausente: " + chave);
                }
            }
            if (!env.getRequiredProperty("app.stripe.secret-key").startsWith("sk_live_")) {
                throw new IllegalStateException("Perfil prod exige chave Stripe live");
            }
            for (String chave : new String[]{"app.stripe.success-url", "app.stripe.cancel-url"}) {
                if (!env.getRequiredProperty(chave).startsWith("https://")) {
                    throw new IllegalStateException("Perfil prod exige URL HTTPS: " + chave);
                }
            }
        };
    }
}
