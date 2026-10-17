package br.com.luvi.fideliza.fidelidade.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "participacao",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_participacao_cadastro",
          columnNames = {"estabelecimento_id", "chave_cadastro"}),
      @UniqueConstraint(
          name = "uk_participacao_usuario",
          columnNames = {"programa_id", "usuario_id"})
    })
public class Participacao {
  @Id private UUID id;

  @Column(name = "estabelecimento_id", nullable = false, updatable = false)
  private UUID estabelecimentoId;

  @Column(name = "programa_id", nullable = false, updatable = false)
  private UUID programaId;

  @Column(name = "usuario_id")
  private UUID usuarioId;

  @Column(name = "nome_exibicao", nullable = false, length = 120, updatable = false)
  private String nomeExibicao;

  @Column(name = "chave_cadastro", nullable = false, updatable = false)
  private UUID chaveCadastro;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "criado_por", nullable = false, updatable = false)
  private UUID criadoPor;

  @Column(name = "convite_hash", length = 64, unique = true)
  private String conviteHash;

  @Column(name = "convite_expira_em")
  private Instant conviteExpiraEm;

  protected Participacao() {}

  public Participacao(
      UUID id,
      UUID estabelecimentoId,
      UUID programaId,
      UUID usuarioId,
      String nomeExibicao,
      UUID chaveCadastro,
      Instant criadoEm,
      UUID criadoPor,
      String conviteHash,
      Instant conviteExpiraEm) {
    this.id = id;
    this.estabelecimentoId = estabelecimentoId;
    this.programaId = programaId;
    this.usuarioId = usuarioId;
    this.nomeExibicao = nomeExibicao;
    this.chaveCadastro = chaveCadastro;
    this.criadoEm = criadoEm;
    this.criadoPor = criadoPor;
    this.conviteHash = conviteHash;
    this.conviteExpiraEm = conviteExpiraEm;
  }

  public UUID getId() {
    return id;
  }

  public UUID getEstabelecimentoId() {
    return estabelecimentoId;
  }

  public UUID getProgramaId() {
    return programaId;
  }

  public UUID getUsuarioId() {
    return usuarioId;
  }

  public String getNomeExibicao() {
    return nomeExibicao;
  }

  public UUID getChaveCadastro() {
    return chaveCadastro;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public UUID getCriadoPor() {
    return criadoPor;
  }

  public String getConviteHash() {
    return conviteHash;
  }

  public Instant getConviteExpiraEm() {
    return conviteExpiraEm;
  }

  public void convidar(String hash, Instant expiraEm) {
    this.conviteHash = hash;
    this.conviteExpiraEm = expiraEm;
  }

  public void vincular(UUID usuario) {
    this.usuarioId = usuario;
  }
}
