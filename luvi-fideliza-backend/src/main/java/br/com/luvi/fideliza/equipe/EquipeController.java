package br.com.luvi.fideliza.equipe;

import static org.springframework.http.HttpStatus.*;

import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.usuario.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class EquipeController {
  private final PermissaoEstabelecimentoService permissoes;
  private final EstabelecimentoRepository estabelecimentos;
  private final UsuarioRepository usuarios;
  private final MembroRepository membros;
  private final ConviteEquipeRepository convites;

  public EquipeController(
      PermissaoEstabelecimentoService permissoes,
      EstabelecimentoRepository estabelecimentos,
      UsuarioRepository usuarios,
      MembroRepository membros,
      ConviteEquipeRepository convites) {
    this.permissoes = permissoes;
    this.estabelecimentos = estabelecimentos;
    this.usuarios = usuarios;
    this.membros = membros;
    this.convites = convites;
  }

  public record Membro(UUID id, String nome, String email, String papel, boolean ativo) {}

  public record Convidar(@NotBlank @Email @Size(max = 254) String email) {}

  public record Aceitar(@NotBlank @Size(max = 100) String token) {}

  public record Alterar(@NotNull Boolean ativo) {}

  public record ConviteCriado(String token, Instant expiraEm) {}

  public record ConviteResumo(UUID id, String email, Instant expiraEm, String status) {}

  public record Aceito(String estabelecimento) {}

  @GetMapping("/api/v1/estabelecimentos/{id}/equipe")
  @Transactional(readOnly = true)
  public List<Membro> listar(@PathVariable UUID id, Authentication auth) {
    permissoes.exigirProprietario(id, auth.getName());
    negocio(id, false);
    return membros.findByEstabelecimentoId(id).stream()
        .map(
            m -> {
              var u = usuarios.findById(m.getUsuarioId()).orElseThrow();
              return new Membro(m.getId(), u.getNome(), u.getEmail(), m.getPapel(), m.getAtivo());
            })
        .sorted(Comparator.comparing(Membro::nome, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @PostMapping("/api/v1/estabelecimentos/{id}/equipe/convites")
  @ResponseStatus(CREATED)
  @Transactional
  public ConviteCriado convidar(
      @PathVariable UUID id, @Valid @RequestBody Convidar dados, Authentication auth) {
    var dono = permissoes.exigirProprietario(id, auth.getName());
    negocio(id, true);
    String email = dados.email().trim().toLowerCase(Locale.ROOT);
    usuarios
        .findByEmailIgnoreCase(email)
        .ifPresent(
            u -> {
              if (!u.getAtivo()) throw new ResponseStatusException(CONFLICT, "Conta desativada");
              if (membros.existsByUsuarioIdAndEstabelecimentoIdAndAtivoTrue(u.getId(), id))
                throw new ResponseStatusException(CONFLICT, "Esta conta já integra a equipe ativa");
            });
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var convite =
        convites
            .findByEstabelecimentoIdAndEmail(id, email)
            .orElseGet(() -> new ConviteEquipe(id, email));
    convite.renovar(hash(token), Instant.now().plus(Duration.ofHours(48)), dono.getId());
    convites.save(convite);
    return new ConviteCriado(token, convite.getExpiraEm());
  }

  @GetMapping("/api/v1/estabelecimentos/{id}/equipe/convites")
  @Transactional(readOnly = true)
  public List<ConviteResumo> convites(@PathVariable UUID id, Authentication auth) {
    permissoes.exigirProprietario(id, auth.getName());
    negocio(id, false);
    return convites.findByEstabelecimentoIdOrderByEmail(id).stream()
        .map(
            c ->
                new ConviteResumo(
                    c.getId(),
                    c.getEmail(),
                    c.getExpiraEm(),
                    c.getUsadoEm() != null
                        ? "ACEITO"
                        : c.getExpiraEm().isAfter(Instant.now())
                            ? "PENDENTE"
                            : "EXPIRADO_OU_REVOGADO"))
        .toList();
  }

  @DeleteMapping("/api/v1/estabelecimentos/{id}/equipe/convites/{convite}")
  @ResponseStatus(NO_CONTENT)
  @Transactional
  public void revogar(@PathVariable UUID id, @PathVariable UUID convite, Authentication auth) {
    permissoes.exigirProprietario(id, auth.getName());
    negocio(id, true);
    convites
        .findById(convite)
        .filter(c -> c.getEstabelecimentoId().equals(id))
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Convite não encontrado"))
        .revogar();
  }

  @PatchMapping("/api/v1/estabelecimentos/{id}/equipe/{membro}")
  @ResponseStatus(NO_CONTENT)
  @Transactional
  public void alterar(
      @PathVariable UUID id,
      @PathVariable UUID membro,
      @Valid @RequestBody Alterar dados,
      Authentication auth) {
    permissoes.exigirProprietario(id, auth.getName());
    negocio(id, true);
    var m =
        membros
            .findById(membro)
            .filter(v -> v.getEstabelecimentoId().equals(id))
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Membro não encontrado"));
    if (!m.getPapel().equals("FUNCIONARIO"))
      throw new ResponseStatusException(
          CONFLICT, "O acesso do proprietário não pode ser alterado aqui");
    m.alterarAtivo(dados.ativo());
  }

  @PostMapping("/api/v1/me/equipe/aceitar")
  @Transactional
  public Aceito aceitar(@Valid @RequestBody Aceitar dados, Authentication auth) {
    var u =
        usuarios
            .findByEmailIgnoreCase(auth.getName())
            .filter(Usuario::getAtivo)
            .orElseThrow(() -> new AccessDeniedException("Acesso negado"));
    String hash = hash(dados.token());
    UUID id = convites.estabelecimentoDoToken(hash).orElseThrow(EquipeController::invalido);
    var e = negocio(id, true);
    var c = convites.findByTokenHash(hash).orElseThrow(EquipeController::invalido);
    if (!c.getEmail().equalsIgnoreCase(u.getEmail()))
      throw new ResponseStatusException(
          FORBIDDEN, "Entre com o e-mail para o qual o convite foi emitido");
    var anterior = membros.findByUsuarioIdAndEstabelecimentoId(u.getId(), id);
    if (c.getUsadoEm() != null) {
      if (anterior.isPresent() && anterior.get().getAtivo()) return new Aceito(e.getNome());
      throw invalido();
    }
    if (!c.getExpiraEm().isAfter(Instant.now())) throw invalido();
    if (anterior.isPresent()) anterior.get().alterarAtivo(true);
    else
      membros.save(
          new MembroEstabelecimento(UUID.randomUUID(), u.getId(), id, "FUNCIONARIO", true));
    c.consumir();
    return new Aceito(e.getNome());
  }

  private Estabelecimento negocio(UUID id, boolean lock) {
    return (lock ? estabelecimentos.buscarParaAlteracao(id) : estabelecimentos.findById(id))
        .filter(Estabelecimento::getAtivo)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    NOT_FOUND, "Estabelecimento não encontrado ou inativo"));
  }

  private static ResponseStatusException invalido() {
    return new ResponseStatusException(BAD_REQUEST, "Convite inválido, expirado ou revogado");
  }

  private static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
