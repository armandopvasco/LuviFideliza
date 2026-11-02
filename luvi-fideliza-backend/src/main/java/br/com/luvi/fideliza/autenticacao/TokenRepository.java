package br.com.luvi.fideliza.autenticacao;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepository extends JpaRepository<TokenAcesso, UUID> {
  Optional<TokenAcesso> findByTokenHash(String tokenHash);

  void deleteByTokenHash(String tokenHash);
}
