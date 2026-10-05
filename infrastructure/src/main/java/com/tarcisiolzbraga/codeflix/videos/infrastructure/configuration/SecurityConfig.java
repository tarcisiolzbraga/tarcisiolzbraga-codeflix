package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

// O Spring Security entrou no classpath junto com o oauth2-client, que esta API usa como *cliente*
// para falar com a API do admin. Sem esta cadeia, o padrão do Boot trancaria as rotas daqui com
// autenticação básica, o que não é o que se quer: quem precisa de token é a chamada que sai, não a
// que entra.
//
// A autenticação das rotas desta API é assunto separado e entra quando elas existirem.
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(rules -> rules.anyRequest().permitAll())
                .build();
    }
}
