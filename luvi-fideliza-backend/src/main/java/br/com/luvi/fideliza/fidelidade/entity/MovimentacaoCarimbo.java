package br.com.luvi.fideliza.fidelidade.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Immutable
@Entity
@Table(
    name = "movimentacao_carimbo",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_carimbo_operacao",
          columnNames = {"estabelecimento_id", "chave_operacao"})
    })
public class MovimentacaoCarimbo {
  @Id private UUID id;

  @Column(name = "estabelecimento_id", nullable = false, updatable = false)
  private UUID estabelecimentoId;

  @Column(name = "participacao_id", nullable = false, updatable = false)
  private UUID participacaoId;

  @Column(name = "cartao_id", nullable = false, updatable = false)
  private UUID cartaoId;

  @Column(name = "chave_operacao", nullable = false, updatable = false)
  private UUID chaveOperacao;

  @Column(name = "referencia_compra", nullable = false, length = 120, updatable = false)
  private String referenciaCompra;

  @Column(name = "saldo_apos", nullable = false, updatable = false)
  private int saldoApos;

  @Column(name = "registrado_em", nullable = false, updatable = false)
  private Instant registradoEm;

  @Column(name = "registrado_por", nullable = false, updatable = false)
  private UUID registradoPor;

  protected MovimentacaoCarimbo() {}

  public MovimentacaoCarimbo(
      UUID id,
      UUID estabelecimentoId,
      UUID participacaoId,
      UUID cartaoId,
      UUID chaveOperacao,
      String referenciaCompra,
      int saldoApos,
      Instant registradoEm,
      UUID registradoPor) {
    this.id = id;
    this.estabelecimentoId = estabelecimentoId;
    this.participacaoId = participacaoId;
    this.cartaoId = cartaoId;
    this.chaveOperacao = chaveOperacao;
    this.referenciaCompra = referenciaCompra;
    this.saldoApos = saldoApos;
    this.registradoEm = registradoEm;
    this.registradoPor = registradoPor;
  }

  public UUID getId() {
    return id;
  }

  public UUID getEstabelecimentoId() {
    return estabelecimentoId;
  }

  public UUID getParticipacaoId() {
    return participacaoId;
  }

  public UUID getCartaoId() {
    return cartaoId;
  }

  public UUID getChaveOperacao() {
    return chaveOperacao;
  }

  public String getReferenciaCompra() {
    return referenciaCompra;
  }

  public int getSaldoApos() {
    return saldoApos;
  }

  public Instant getRegistradoEm() {
    return registradoEm;
  }

  public UUID getRegistradoPor() {
    return registradoPor;
  }
}
