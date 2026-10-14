package br.com.luvi.fideliza.usuario;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
 Optional<Usuario> findByEmailIgnoreCase(String email);
 boolean existsByAdministradorPlataformaTrue();
}
