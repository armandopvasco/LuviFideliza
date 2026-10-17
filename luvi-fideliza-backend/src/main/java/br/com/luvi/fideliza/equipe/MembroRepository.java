package br.com.luvi.fideliza.equipe;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembroRepository extends JpaRepository<MembroEstabelecimento, UUID> {
  java.util.Optional<MembroEstabelecimento> findByUsuarioIdAndEstabelecimentoId(
      UUID usuario, UUID estabelecimento);

  java.util.List<MembroEstabelecimento> findByEstabelecimentoId(UUID estabelecimento);

  boolean existsByUsuarioIdAndAtivoTrue(UUID usuario);

  boolean existsByUsuarioIdAndEstabelecimentoIdAndAtivoTrue(UUID usuario, UUID estabelecimento);

  boolean existsByUsuarioIdAndEstabelecimentoIdAndPapelAndAtivoTrue(
      UUID usuario, UUID estabelecimento, String papel);
}
