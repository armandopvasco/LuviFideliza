package br.com.luvi.fideliza.autenticacao;

import br.com.luvi.fideliza.equipe.MembroRepository;
import br.com.luvi.fideliza.usuario.Usuario;
import br.com.luvi.fideliza.usuario.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TokenService {
  private final UsuarioRepository usuarios;
  private final MembroRepository membros;
  private final TokenRepository tokens;
  private final PasswordEncoder encoder;
  private final long dias;
  private final SecureRandom aleatorio = new SecureRandom();

  public TokenService(
      UsuarioRepository usuarios,
      MembroRepository membros,
      TokenRepository tokens,
      PasswordEncoder encoder,
      @Value("${luvi.token.dias:30}") long dias) {
    this.usuarios = usuarios;
    this.membros = membros;
    this.tokens = tokens;
    this.encoder = encoder;
    this.dias = dias;
  }

  public record TokenEmitido(String token, Instant expiraEm, String nome, String identificador) {}

  @Transactional
  public TokenEmitido emitir(String identificador, String senha) {
    Usuario usuario = buscar(identificador);
    if (usuario == null
        || !usuario.getAtivo()
        || senha == null
        || !encoder.matches(senha, usuario.getSenhaHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
    }
    byte[] bytes = new byte[32];
    aleatorio.nextBytes(bytes);
    String puro = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Instant expira = Instant.now().plus(Duration.ofDays(Math.max(1, dias)));
    tokens.save(
        new TokenAcesso(UUID.randomUUID(), usuario.getId(), hash(puro), expira, Instant.now()));
    return new TokenEmitido(puro, expira, usuario.getNome(), nomeDeUsuario(usuario));
  }

  @Transactional
  public void revogar(String tokenPuro) {
    if (tokenPuro == null || tokenPuro.isBlank()) return;
    tokens.deleteByTokenHash(hash(tokenPuro.trim()));
  }

  @Transactional(readOnly = true)
  public Authentication autenticar(String tokenPuro) {
    if (tokenPuro == null || tokenPuro.isBlank()) return null;
    TokenAcesso salvo = tokens.findByTokenHash(hash(tokenPuro.trim())).orElse(null);
    if (salvo == null || !salvo.getExpiraEm().isAfter(Instant.now())) return null;
    Usuario usuario =
        usuarios.findById(salvo.getUsuarioId()).filter(Usuario::getAtivo).orElse(null);
    if (usuario == null) return null;
    UserDetails principal =
        User.withUsername(nomeDeUsuario(usuario))
            .password("")
            .roles(papel(usuario))
            .build();
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }

  private String papel(Usuario usuario) {
    if (usuario.getAdministradorPlataforma()) return "ADMIN";
    if (membros.existsByUsuarioIdAndAtivoTrue(usuario.getId())) return "OPERADOR";
    return "CLIENTE";
  }

  private Usuario buscar(String identificador) {
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

  private static String nomeDeUsuario(Usuario usuario) {
    return usuario.getEmail() != null ? usuario.getEmail() : usuario.getTelefone();
  }

  static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
