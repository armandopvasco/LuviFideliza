package br.com.luvi.fideliza.fidelidade.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "programa_fidelidade")
public class ProgramaFidelidade {
  @Id private UUID id;

  @Column(name = "estabelecimento_id", nullable = false, unique = true, updatable = false)
  private UUID estabelecimentoId;

  @Column(name = "versao_atual", nullable = false)
  private int versaoAtual;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

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
