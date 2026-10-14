package br.com.luvi.fideliza.estabelecimento;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, UUID> {}
