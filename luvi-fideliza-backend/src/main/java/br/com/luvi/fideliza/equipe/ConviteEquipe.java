package br.com.luvi.fideliza.equipe;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "convite_equipe",
    uniqueConstraints = @UniqueConstraint(columnNames = {"estabelecimento_id", "email"}))
public class ConviteEquipe {
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

  protected ConviteEquipe() {}

  public ConviteEquipe(UUID estabelecimento, String email) {
    this.id = UUID.randomUUID();
    this.estabelecimentoId = estabelecimento;
    this.email = email;
  }

  public void renovar(String hash, Instant expira, UUID autor) {
    tokenHash = hash;
    expiraEm = expira;
    criadoPor = autor;
    usadoEm = null;
  }

  public void consumir() {
    usadoEm = Instant.now();
  }

  public void revogar() {
    expiraEm = Instant.now();
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

  public Instant getExpiraEm() {
    return expiraEm;
  }

  public Instant getUsadoEm() {
    return usadoEm;
  }
}
