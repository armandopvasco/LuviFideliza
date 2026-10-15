package br.com.luvi.fideliza.fidelidade.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(
    name = "versao_programa",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_versao_programa_numero",
            columnNames = {"programa_id", "numero"}))
public class VersaoPrograma {
  @Id private UUID id;

  @Column(name = "programa_id", nullable = false, updatable = false)
  private UUID programaId;

  @Column(nullable = false, updatable = false)
  private int numero;

  @Column(nullable = false, length = 120, updatable = false)
  private String titulo;

  @Column(name = "meta_carimbos", nullable = false, updatable = false)
  private int metaCarimbos;

  @Column(nullable = false, length = 200, updatable = false)
  private String recompensa;

  @Column(name = "criterios_compra", nullable = false, length = 500, updatable = false)
  private String criteriosCompra;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "criado_por", nullable = false, updatable = false)
  private UUID criadoPor;

  protected VersaoPrograma() {}

  public VersaoPrograma(
      UUID programaId,
      int numero,
      String titulo,
      int metaCarimbos,
      String recompensa,
      String criteriosCompra,
      Instant criadoEm,
      UUID criadoPor) {
    this.id = UUID.randomUUID();
    this.programaId = programaId;
    this.numero = numero;
    this.titulo = titulo;
    this.metaCarimbos = metaCarimbos;
    this.recompensa = recompensa;
    this.criteriosCompra = criteriosCompra;
    this.criadoEm = criadoEm;
    this.criadoPor = criadoPor;
  }

  public UUID getId() {
    return id;
  }

  public UUID getProgramaId() {
    return programaId;
  }

  public int getNumero() {
    return numero;
  }

  public String getTitulo() {
    return titulo;
  }

  public int getMetaCarimbos() {
    return metaCarimbos;
  }

  public String getRecompensa() {
    return recompensa;
  }

  public String getCriteriosCompra() {
    return criteriosCompra;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public UUID getCriadoPor() {
    return criadoPor;
  }
}
