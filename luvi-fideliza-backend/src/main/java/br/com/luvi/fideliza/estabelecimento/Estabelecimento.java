package br.com.luvi.fideliza.estabelecimento;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="estabelecimento")
public class Estabelecimento {
 @Id
 private UUID id;
 @Column(nullable=false, length=150)
 private String nome;
 @Column(nullable=false)
 private boolean ativo;
 @Column(name="criado_em", nullable=false)
 private Instant criadoEm;
 protected Estabelecimento() {}
 public Estabelecimento(UUID id, String nome, boolean ativo, Instant criadoEm) {
  this.id=id;
  this.nome=nome;
  this.ativo=ativo;
  this.criadoEm=criadoEm;
 }
 public UUID getId() { return id; }
 public String getNome() { return nome; }
 public boolean getAtivo() { return ativo; }
 public Instant getCriadoEm() { return criadoEm; }
}
