package br.com.luvi.fideliza.usuario;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
  Optional<Usuario> findByEmailIgnoreCase(String email);

  Optional<Usuario> findByTelefone(String telefone);
  boolean existsByAdministradorPlataformaTrue();
}
