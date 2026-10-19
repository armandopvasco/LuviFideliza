package br.com.luvi.fideliza.fidelidade.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "programa_fidelidade")
public class ProgramaFidelidade {
  @Id private UUID id;

  @Column(name = "estabelecimento_id", nullable = false, updatable = false)
  private UUID estabelecimentoId;

  @Column(name = "versao_atual", nullable = false)
  private int versaoAtual;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(nullable = false)
  private boolean ativa = true;
  @Column(nullable = false)
  private boolean principal = false;
  public boolean isAtiva() { return ativa; }
  public boolean isPrincipal() { return principal; }
  public void setAtiva(boolean valor) { ativa = valor; }
  public void setPrincipal(boolean valor) { principal = valor; }

  protected ProgramaFidelidade() {}

  public ProgramaFidelidade(UUID estabelecimentoId, Instant agora) {
    this.id = UUID.randomUUID();
    this.estabelecimentoId = estabelecimentoId;
    this.versaoAtual = 1;
    this.criadoEm = agora;
  }

  public UUID getId() {
    return id;
  }

  public UUID getEstabelecimentoId() {
    return estabelecimentoId;
  }

  public int getVersaoAtual() {
    return versaoAtual;
  }

  public void avancarVersao() {
    versaoAtual++;
  }
}
