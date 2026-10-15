package br.com.luvi.fideliza.autenticacao;

import br.com.luvi.fideliza.usuario.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@ConditionalOnProperty(name = "luvi.bootstrap.enabled", havingValue = "true")
public class AdminBootstrap implements CommandLineRunner {
  private final UsuarioRepository usuarios;
  private final PasswordEncoder encoder;

  @Value("${luvi.bootstrap.email}")
  private String email;

  @Value("${luvi.bootstrap.password}")
  private String password;

  public AdminBootstrap(UsuarioRepository usuarios, PasswordEncoder encoder) {
    this.usuarios = usuarios;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (usuarios.existsByAdministradorPlataformaTrue()) return;
    if (password.length() < 12 || password.length() > 72)
      throw new IllegalStateException("Senha inicial deve ter de 12 a 72 caracteres ASCII.");
    if (!password.chars().allMatch(c -> c >= 32 && c <= 126))
      throw new IllegalStateException("Use caracteres ASCII na senha inicial.");
    String normalized = email.trim().toLowerCase(Locale.ROOT);
    if (!normalized.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      throw new IllegalStateException("E-mail inicial inválido.");
    if (usuarios.findByEmailIgnoreCase(normalized).isPresent())
      throw new IllegalStateException("E-mail já cadastrado; não será promovido automaticamente.");
    usuarios.save(
        new Usuario(
            UUID.randomUUID(),
            "Administrador LUVI",
            normalized,
            encoder.encode(password),
            false,
            true,
            true,
            Instant.now()));
  }
}
