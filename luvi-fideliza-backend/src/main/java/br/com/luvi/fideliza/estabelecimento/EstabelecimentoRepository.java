package br.com.luvi.fideliza.estabelecimento;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Estabelecimento e where e.id = :id")
  Optional<Estabelecimento> buscarParaAlteracao(@Param("id") UUID id);
}
