package br.com.luvi.fideliza.estabelecimento;

import static org.springframework.http.HttpStatus.*;

import br.com.luvi.fideliza.autenticacao.*;
import br.com.luvi.fideliza.equipe.*;
import br.com.luvi.fideliza.usuario.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AcessoEstabelecimentoService {
  private final UsuarioRepository usuarios;
  private final EstabelecimentoRepository estabelecimentos;
  private final MembroRepository membros;
  private final ConviteRepository convites;
  private final PasswordEncoder encoder;

  public AcessoEstabelecimentoService(
      UsuarioRepository u,
      EstabelecimentoRepository e,
      MembroRepository m,
      ConviteRepository c,
      PasswordEncoder p) {
    usuarios = u;
    estabelecimentos = e;
    membros = m;
    convites = c;
    encoder = p;
  }

  public record Negocio(UUID id, String nome) {}

  public record ConviteCriado(UUID estabelecimentoId, String token, Instant expiraEm) {}

  @Transactional
  public Negocio cadastrar(String nome, String email) {
    exigirAdmin(email);
    var e =
        estabelecimentos.save(
            new Estabelecimento(UUID.randomUUID(), nome.trim(), true, Instant.now()));
    return new Negocio(e.getId(), e.getNome());
  }

  private Usuario usuario(String email) {
    var u =
        usuarios
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new AccessDeniedException("Acesso negado"));
    if (!u.getAtivo()) throw new AccessDeniedException("Acesso negado");
    return u;
  }

  private Usuario exigirAdmin(String email) {
    var u = usuario(email);
    if (!u.getAdministradorPlataforma()) throw new AccessDeniedException("Acesso negado");
    return u;
  }

  @Transactional(readOnly = true)
  public Negocio consultar(UUID id, String email) {
    var u = usuario(email);
    if (!u.getAdministradorPlataforma()
        && !membros.existsByUsuarioIdAndEstabelecimentoIdAndAtivoTrue(u.getId(), id))
      throw new AccessDeniedException("Acesso negado");
    var e =
        estabelecimentos
            .findById(id)
            .filter(Estabelecimento::getAtivo)
            .orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Estabelecimento não encontrado"));
    return new Negocio(e.getId(), e.getNome());
  }

  @Transactional
  public ConviteCriado convidar(UUID id, String emailProprietario, String emailAdmin) {
    var admin = exigirAdmin(emailAdmin);
    if (estabelecimentos.findById(id).filter(Estabelecimento::getAtivo).isEmpty())
      throw new ResponseStatusException(NOT_FOUND, "Estabelecimento não encontrado");
    String email = emailProprietario.trim().toLowerCase(Locale.ROOT);
    if (usuarios.findByEmailIgnoreCase(email).isPresent())
      throw new ResponseStatusException(
          CONFLICT, "E-mail já cadastrado. Vinculação de contas existentes será entregue depois.");
    byte[] random = new byte[32];
    new SecureRandom().nextBytes(random);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    Instant expiry = Instant.now().plus(Duration.ofHours(48));
    convites.save(
        new ConviteProprietario(
            UUID.randomUUID(), id, email, hash(token), expiry, null, admin.getId()));
    return new ConviteCriado(id, token, expiry);
  }

  @Transactional
  public Negocio aceitar(String token, String nome, String senha) {
    var c =
        convites
            .buscarParaConsumo(hash(token))
            .orElseThrow(AcessoEstabelecimentoService::conviteInvalido);
    Instant agora = Instant.now();
    if (c.getUsadoEm() != null || !c.getExpiraEm().isAfter(agora)) throw conviteInvalido();
    var e =
        estabelecimentos
            .findById(c.getEstabelecimentoId())
            .filter(Estabelecimento::getAtivo)
            .orElseThrow(AcessoEstabelecimentoService::conviteInvalido);
    if (usuarios.findByEmailIgnoreCase(c.getEmail()).isPresent())
      throw new ResponseStatusException(CONFLICT, "E-mail já cadastrado");
    var u =
        usuarios.save(
            new Usuario(
                UUID.randomUUID(),
                nome.trim(),
                c.getEmail(),
                encoder.encode(senha),
                false,
                false,
                true,
                agora));
    membros.save(
        new MembroEstabelecimento(UUID.randomUUID(), u.getId(), e.getId(), "PROPRIETARIO", true));
    c.consumir(agora);
    return new Negocio(e.getId(), e.getNome());
  }

  private static ResponseStatusException conviteInvalido() {
    return new ResponseStatusException(BAD_REQUEST, "Convite inválido, expirado ou já utilizado");
  }

  static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
