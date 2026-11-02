package br.com.luvi.fideliza.autenticacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_acesso")
public class TokenAcesso {
  @Id private UUID id;

  @Column(name = "usuario_id", nullable = false, updatable = false)
  private UUID usuarioId;

  @Column(name = "token_hash", nullable = false, length = 64, unique = true, updatable = false)
  private String tokenHash;

  @Column(name = "expira_em", nullable = false)
  private Instant expiraEm;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  protected TokenAcesso() {}

  public TokenAcesso(UUID id, UUID usuarioId, String tokenHash, Instant expiraEm, Instant criadoEm) {
    this.id = id;
    this.usuarioId = usuarioId;
    this.tokenHash = tokenHash;
    this.expiraEm = expiraEm;
    this.criadoEm = criadoEm;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUsuarioId() {
    return usuarioId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiraEm() {
    return expiraEm;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
