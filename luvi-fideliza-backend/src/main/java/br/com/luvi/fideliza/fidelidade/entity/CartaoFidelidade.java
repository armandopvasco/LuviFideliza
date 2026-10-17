package br.com.luvi.fideliza.fidelidade.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "cartao_fidelidade",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_cartao_ciclo",
          columnNames = {"participacao_id", "numero"})
    })
public class CartaoFidelidade {
  @Id private UUID id;

  @Column(name = "participacao_id", nullable = false, updatable = false)
  private UUID participacaoId;

  @Column(name = "versao_programa_id", nullable = false, updatable = false)
  private UUID versaoProgramaId;

  @Column(nullable = false, updatable = false)
  private int numero;

  @Column(nullable = false)
  private int saldo;

  @Column(name = "aberto_em", nullable = false, updatable = false)
  private Instant abertoEm;

  @Column(name = "concluido_em")
  private Instant concluidoEm;

  protected CartaoFidelidade() {}

  public CartaoFidelidade(
      UUID id,
      UUID participacaoId,
      UUID versaoProgramaId,
      int numero,
      int saldo,
      Instant abertoEm,
      Instant concluidoEm) {
    this.id = id;
    this.participacaoId = participacaoId;
    this.versaoProgramaId = versaoProgramaId;
    this.numero = numero;
    this.saldo = saldo;
    this.abertoEm = abertoEm;
    this.concluidoEm = concluidoEm;
  }

  public UUID getId() {
    return id;
  }

  public UUID getParticipacaoId() {
    return participacaoId;
  }

  public UUID getVersaoProgramaId() {
    return versaoProgramaId;
  }

  public int getNumero() {
    return numero;
  }

  public int getSaldo() {
    return saldo;
  }

  public Instant getAbertoEm() {
    return abertoEm;
  }

  public Instant getConcluidoEm() {
    return concluidoEm;
  }

  public void creditar(int meta, Instant agora) {
    if (concluidoEm != null || saldo >= meta)
      throw new IllegalStateException("Cartão já concluído");
    saldo++;
    if (saldo == meta) concluidoEm = agora;
  }
}
