package com.pablo.ecommerce.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile({"prod", "sandbox"})
public class ProductionConfigGuard {
    @Bean
    ApplicationRunner validarProducao(Environment env) {
        return args -> {
            if (env.matchesProfiles("demo") || (env.matchesProfiles("prod") && env.matchesProfiles("sandbox"))) {
                throw new IllegalStateException("Perfis demo, prod e sandbox nao podem ser combinados");
            }
            for (String chave : new String[]{"app.stripe.secret-key", "app.stripe.webhook-secret",
                    "app.email.from", "app.resend.api-key",
                    "app.digital.storage-path"}) {
                if (env.getRequiredProperty(chave).isBlank()) {
                    throw new IllegalStateException("Configuracao obrigatoria ausente: " + chave);
                }
            }
            String prefixo = env.matchesProfiles("sandbox") ? "sk_test_" : "sk_live_";
            if (!env.getRequiredProperty("app.stripe.secret-key").startsWith(prefixo)) {
                throw new IllegalStateException("Chave Stripe incompativel com o perfil ativo");
            }
            for (String chave : new String[]{"app.stripe.success-url", "app.stripe.cancel-url"}) {
                if (!env.getRequiredProperty(chave).startsWith("https://")) {
                    throw new IllegalStateException("Ambiente hospedado exige URL HTTPS: " + chave);
                }
            }
        };
    }
}
