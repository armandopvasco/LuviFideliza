package br.com.luvi.fideliza.autenticacao;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ConviteRepository extends JpaRepository<ConviteProprietario, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from ConviteProprietario c where c.tokenHash = :hash")
  Optional<ConviteProprietario> buscarParaConsumo(@Param("hash") String hash);
}
