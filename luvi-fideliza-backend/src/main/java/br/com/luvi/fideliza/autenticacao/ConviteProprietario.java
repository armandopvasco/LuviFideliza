package br.com.luvi.fideliza.autenticacao;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "convite_proprietario")
public class ConviteProprietario {
  @Id private UUID id;

  @Column(name = "estabelecimento_id", nullable = false)
  private UUID estabelecimentoId;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expira_em", nullable = false)
  private Instant expiraEm;

  @Column(name = "usado_em")
  private Instant usadoEm;

  @Column(name = "criado_por", nullable = false)
  private UUID criadoPor;

  protected ConviteProprietario() {}

  public ConviteProprietario(
      UUID id,
      UUID estabelecimentoId,
      String email,
      String tokenHash,
      Instant expiraEm,
      Instant usadoEm,
      UUID criadoPor) {
    this.id = id;
    this.estabelecimentoId = estabelecimentoId;
    this.email = email;
    this.tokenHash = tokenHash;
    this.expiraEm = expiraEm;
    this.usadoEm = usadoEm;
    this.criadoPor = criadoPor;
  }

  public UUID getId() {
    return id;
  }

  public UUID getEstabelecimentoId() {
    return estabelecimentoId;
  }

  public String getEmail() {
    return email;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiraEm() {
    return expiraEm;
  }

  public Instant getUsadoEm() {
    return usadoEm;
  }

  public UUID getCriadoPor() {
    return criadoPor;
  }

  public void consumir(Instant agora) {
    this.usadoEm = agora;
  }
}
