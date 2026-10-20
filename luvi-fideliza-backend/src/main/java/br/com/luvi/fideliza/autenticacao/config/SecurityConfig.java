package br.com.luvi.fideliza.autenticacao.config;

import br.com.luvi.fideliza.equipe.MembroRepository;
import br.com.luvi.fideliza.autenticacao.Telefone;
import br.com.luvi.fideliza.usuario.UsuarioRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(UsuarioRepository usuarios, MembroRepository membros) {
    return identificador ->
        (identificador.contains("@") ? usuarios.findByEmailIgnoreCase(identificador.trim()) : usuarios.findByTelefone(Telefone.normalizar(identificador)))
            .map(
                u ->
                    User.withUsername(u.getEmail() != null ? u.getEmail() : u.getTelefone())
                        .password(u.getSenhaHash())
                        .disabled(!u.getAtivo())
                        .roles(
                            u.getAdministradorPlataforma()
                                ? "ADMIN"
                                : membros.existsByUsuarioIdAndAtivoTrue(u.getId())
                                    ? "OPERADOR"
                                    : "CLIENTE")
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/admin.html",
                        "/admin.js",
                        "/proprietario-convite.html",
                        "/campanhas.html",
                        "/campanhas.js",
                        "/cliente.html",
                        "/cliente.css",
                        "/cliente.js",
                        "/cartao-personalizacao.js",
                        "/cliente-sw.js",
                        "/cliente.webmanifest",
                        "/cliente-icon-192.png",
                        "/cliente-icon-512.png",
                        "/luvi-logo.png",
                        "/luvi-marca.png",
                        "/luvi-simbolo.png",
                        "/painel.html",
                        "/painel.css",
                        "/painel.js",
                        "/painel-funcoes.js",
                        "/convite.html",
                        "/actuator/health",
                        "/api/v1/auth/csrf",
                        "/api/v1/auth/convites/aceitar",
                        "/api/v1/auth/clientes",
                        "/adesao.html", "/adesao.js", "/convite-whatsapp.js", "/adesao-painel.js", "/api/v1/publico/estabelecimentos/**")
                    .permitAll()
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginProcessingUrl("/api/v1/auth/login")
                    .usernameParameter("email")
                    .successHandler((req, res, auth) -> res.setStatus(204))
                    .failureHandler((req, res, ex) -> res.setStatus(401))
                    .permitAll())
        .logout(
            l ->
                l.logoutUrl("/api/v1/auth/logout")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((req, res, ex) -> res.setStatus(401))
                    .accessDeniedHandler((req, res, ex) -> res.setStatus(403)));
    // Permite incorporar somente páginas da própria origem no painel.
    // Mantém a proteção contra incorporação por sites externos.
    http.headers(h -> h.frameOptions(f -> f.sameOrigin()));
    // CSRF e proteção contra fixação de sessão permanecem habilitados.
    return http.build();
  }
}
