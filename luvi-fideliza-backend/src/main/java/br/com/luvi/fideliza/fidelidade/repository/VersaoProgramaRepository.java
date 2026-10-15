package br.com.luvi.fideliza.fidelidade.repository;

import br.com.luvi.fideliza.fidelidade.entity.VersaoPrograma;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VersaoProgramaRepository extends JpaRepository<VersaoPrograma, UUID> {
  Optional<VersaoPrograma> findByProgramaIdAndNumero(UUID programaId, int numero);

  List<VersaoPrograma> findByProgramaIdOrderByNumeroDesc(UUID programaId);
}
