package br.com.luvi.fideliza.autenticacao;

import br.com.luvi.fideliza.usuario.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ClienteController {
  private final UsuarioRepository usuarios;
  private final PasswordEncoder encoder;

  public ClienteController(UsuarioRepository usuarios, PasswordEncoder encoder) {
    this.usuarios = usuarios;
    this.encoder = encoder;
  }

  public record CadastroCliente(
      @NotBlank @Size(max = 120) String nome,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 72) @Pattern(regexp = "[\\x20-\\x7E]+") String senha) {}

  public record ClienteCriado(UUID id, String nome, String email) {}

  @PostMapping("/api/v1/auth/clientes")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public ClienteCriado cadastrar(@Valid @RequestBody CadastroCliente dados) {
    String email = dados.email().trim().toLowerCase(Locale.ROOT);
    if (usuarios.findByEmailIgnoreCase(email).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "E-mail já cadastrado. Use o login existente.");
    }
    Usuario usuario =
        usuarios.save(
            new Usuario(
                UUID.randomUUID(),
                dados.nome().trim(),
                email,
                encoder.encode(dados.senha()),
                false,
                false,
                true,
                Instant.now()));
    return new ClienteCriado(usuario.getId(), usuario.getNome(), usuario.getEmail());
  }
}
