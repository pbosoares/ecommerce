package com.pablo.ecommerce.config;

import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LojaConfigController {
    private final Environment environment;

    public LojaConfigController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/loja/config")
    public Map<String, Boolean> config() {
        return Map.of("demo", environment.acceptsProfiles(Profiles.of("demo")),
                "pagamentoTeste", environment.acceptsProfiles(Profiles.of("sandbox")));
    }
}
