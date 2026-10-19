package br.com.luvi.fideliza.fidelidade.repository;

import br.com.luvi.fideliza.fidelidade.entity.ProgramaFidelidade;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramaRepository extends JpaRepository<ProgramaFidelidade, UUID> {
  Optional<ProgramaFidelidade> findByEstabelecimentoIdAndPrincipalTrue(UUID estabelecimentoId);
  java.util.List<ProgramaFidelidade> findByEstabelecimentoIdOrderByCriadoEmAsc(UUID estabelecimentoId);
}
