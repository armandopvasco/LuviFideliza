package br.com.luvi.fideliza.usuario;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="usuario")
public class Usuario {
 @Id
 private UUID id;
 @Column(nullable=false, length=120)
 private String nome;
 @Column(nullable=false, length=254)
 private String email;
 @Column(name="senha_hash", nullable=false, length=255)
 private String senhaHash;
 @Column(name="email_verificado", nullable=false)
 private boolean emailVerificado;
 @Column(name="administrador_plataforma", nullable=false)
 private boolean administradorPlataforma;
 @Column(nullable=false)
 private boolean ativo;
 @Column(name="criado_em", nullable=false)
 private Instant criadoEm;
 protected Usuario() {}
 public Usuario(UUID id, String nome, String email, String senhaHash, boolean emailVerificado, boolean administradorPlataforma, boolean ativo, Instant criadoEm) {
  this.id=id;
  this.nome=nome;
  this.email=email;
  this.senhaHash=senhaHash;
  this.emailVerificado=emailVerificado;
  this.administradorPlataforma=administradorPlataforma;
  this.ativo=ativo;
  this.criadoEm=criadoEm;
 }
 public UUID getId() { return id; }
 public String getNome() { return nome; }
 public String getEmail() { return email; }
 public String getSenhaHash() { return senhaHash; }
 public boolean getEmailVerificado() { return emailVerificado; }
 public boolean getAdministradorPlataforma() { return administradorPlataforma; }
 public boolean getAtivo() { return ativo; }
 public Instant getCriadoEm() { return criadoEm; }
}
