package br.com.luvi.fideliza.estabelecimento;

import br.com.luvi.fideliza.autenticacao.Telefone;
import br.com.luvi.fideliza.usuario.Usuario;
import br.com.luvi.fideliza.usuario.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AcessoController {
  private final AcessoEstabelecimentoService service;
  private final UsuarioRepository usuarios;

  public AcessoController(AcessoEstabelecimentoService service, UsuarioRepository usuarios) {
    this.service = service;
    this.usuarios = usuarios;
  }

  public record Cadastro(@NotBlank @Size(max = 150) String nome) {}

  public record Convite(@NotBlank @Size(max = 254) String email) {}

  public record Aceite(
      @NotBlank @Size(max = 100) String token,
      @NotBlank @Size(max = 120) String nome,
      @NotBlank
          @Size(min = 12, max = 72)
          @Pattern(regexp = "[\\x20-\\x7E]+", message = "Use caracteres ASCII na senha")
          String senha) {}

  @GetMapping("/api/v1/auth/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @GetMapping("/api/v1/auth/me")
  public Map<String, Object> me(Authentication a) {
    String identificador = a.getName() == null ? "" : a.getName();
    return Map.of(
        "email",
        identificador,
        "nome",
        nomeExibicao(identificador),
        "perfis",
        a.getAuthorities().stream().map(Object::toString).toList());
  }

  private String nomeExibicao(String identificador) {
    Usuario usuario = localizar(identificador);
    if (usuario == null || usuario.getNome() == null || usuario.getNome().isBlank()) {
      return identificador;
    }
    return usuario.getNome().trim();
  }

  private Usuario localizar(String identificador) {
    if (identificador == null || identificador.isBlank()) return null;
    String valor = identificador.trim();
    if (valor.contains("@")) return usuarios.findByEmailIgnoreCase(valor).orElse(null);
    try {
      String telefone = Telefone.normalizar(valor);
      return telefone == null ? null : usuarios.findByTelefone(telefone).orElse(null);
    } catch (ResponseStatusException ex) {
      return null;
    }
  }

  @GetMapping("/api/v1/admin/estabelecimentos")
  public List<AcessoEstabelecimentoService.EstabelecimentoAdmin> listar(Authentication a) {
    return service.listarAdmin(a.getName());
  }

  @PostMapping("/api/v1/admin/estabelecimentos")
  @ResponseStatus(HttpStatus.CREATED)
  public AcessoEstabelecimentoService.Negocio cadastrar(
      @Valid @RequestBody Cadastro c, Authentication a) {
    return service.cadastrar(c.nome(), a.getName());
  }

  @PostMapping("/api/v1/admin/estabelecimentos/{id}/convites")
  @ResponseStatus(HttpStatus.CREATED)
  public AcessoEstabelecimentoService.ConviteCriado convidar(
      @PathVariable UUID id, @Valid @RequestBody Convite c, Authentication a) {
    return service.convidar(id, c.email(), a.getName());
  }

  @PostMapping("/api/v1/auth/convites/aceitar")
  @ResponseStatus(HttpStatus.CREATED)
  public AcessoEstabelecimentoService.AceiteConcluido aceitar(@Valid @RequestBody Aceite c) {
    return service.aceitar(c.token(), c.nome(), c.senha());
  }

  @GetMapping("/api/v1/estabelecimentos/{id}")
  public AcessoEstabelecimentoService.Negocio consultar(@PathVariable UUID id, Authentication a) {
    return service.consultar(id, a.getName());
  }
}
