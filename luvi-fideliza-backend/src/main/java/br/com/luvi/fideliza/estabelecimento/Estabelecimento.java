package br.com.luvi.fideliza.estabelecimento;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "estabelecimento")
public class Estabelecimento {
  @Id private UUID id;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false)
  private boolean ativo;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm;

  @Column(name = "exibir_todas_campanhas", nullable = false)
  private boolean exibirTodasCampanhas = true;

  @Column(name = "cor_cartao", nullable = false, length = 7)
  private String corCartao = "#30475A";
  @Column(name = "cor_carimbo", nullable = false, length = 7)
  private String corCarimbo = "#87A18F";
  @Column(name = "icone_carimbo", nullable = false, length = 32)
  private String iconeCarimbo = "estrela";
  @Column(name = "foto_fundo_cartao", nullable = false)
  private boolean fotoFundoCartao = false;
  public String getIconeCarimbo() { return iconeCarimbo; }
  public void setIconeCarimbo(String valor) { iconeCarimbo = valor; }
  public boolean isFotoFundoCartao() { return fotoFundoCartao; }
  public void setFotoFundoCartao(boolean valor) { fotoFundoCartao = valor; }
  public String getCorCartao() { return corCartao; }
  public String getCorCarimbo() { return corCarimbo; }
  public void setCorCartao(String valor) { corCartao = valor; }
  public void setCorCarimbo(String valor) { corCarimbo = valor; }

  public boolean isExibirTodasCampanhas() { return exibirTodasCampanhas; }
  public void setExibirTodasCampanhas(boolean valor) { this.exibirTodasCampanhas = valor; }

  protected Estabelecimento() {}

  public Estabelecimento(UUID id, String nome, boolean ativo, Instant criadoEm) {
    this.id = id;
    this.nome = nome;
    this.ativo = ativo;
    this.criadoEm = criadoEm;
  }

  public UUID getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public boolean getAtivo() {
    return ativo;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
