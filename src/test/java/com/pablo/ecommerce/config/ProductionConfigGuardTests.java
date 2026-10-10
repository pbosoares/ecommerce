package com.pablo.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigGuardTests {
    private MockEnvironment environment(String profile, String key) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profile);
        env.setProperty("app.stripe.secret-key", key);
        env.setProperty("app.stripe.webhook-secret", "whsec_test");
        env.setProperty("app.resend.api-key", "re_test");
        env.setProperty("app.email.from", "onboarding@resend.dev");
        env.setProperty("app.digital.storage-path", "/data/digital");
        env.setProperty("app.stripe.success-url", "https://shop.example/success");
        env.setProperty("app.stripe.cancel-url", "https://shop.example/cancel");
        return env;
    }

    @Test void sandboxAcceptsTestKeys() {
        assertDoesNotThrow(() -> new ProductionConfigGuard().validarProducao(environment("sandbox", "sk_test_example")).run(null));
    }

    @Test void sandboxRejectsLiveKeys() {
        assertThrows(IllegalStateException.class, () -> new ProductionConfigGuard().validarProducao(environment("sandbox", "sk_live_example")).run(null));
    }

    @Test void productionRejectsTestKeys() {
        assertThrows(IllegalStateException.class, () -> new ProductionConfigGuard().validarProducao(environment("prod", "sk_test_example")).run(null));
    }

    @Test void rejectsMixedProfiles() {
        MockEnvironment env = environment("sandbox", "sk_test_example");
        env.setActiveProfiles("prod", "sandbox");
        assertThrows(IllegalStateException.class, () -> new ProductionConfigGuard().validarProducao(env).run(null));
    }
}
