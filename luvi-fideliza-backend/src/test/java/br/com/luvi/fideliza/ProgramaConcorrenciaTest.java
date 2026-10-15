package br.com.luvi.fideliza;

import static org.junit.jupiter.api.Assertions.*;

import br.com.luvi.fideliza.equipe.*;
import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.fidelidade.dto.*;
import br.com.luvi.fideliza.fidelidade.service.ProgramaService;
import br.com.luvi.fideliza.usuario.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:concorrencia;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "luvi.bootstrap.enabled=false"
    })
class ProgramaConcorrenciaTest {
  @Autowired UsuarioRepository usuarios;
  @Autowired EstabelecimentoRepository estabelecimentos;
  @Autowired MembroRepository membros;
  @Autowired ProgramaService service;
  private UUID negocio;
  private String email;

  @BeforeEach
  void preparar() {
    email = UUID.randomUUID() + "@teste.local";
    var usuario =
        usuarios.save(
            new Usuario(
                UUID.randomUUID(), "Dono", email, "hash-teste", false, false, true, Instant.now()));
    negocio =
        estabelecimentos
            .save(new Estabelecimento(UUID.randomUUID(), "Concorrência", true, Instant.now()))
            .getId();
    membros.save(
        new MembroEstabelecimento(
            UUID.randomUUID(), usuario.getId(), negocio, "PROPRIETARIO", true));
  }

  private ProgramaRequest regras() {
    return new ProgramaRequest("Programa", 10, "Corte grátis", "Um por corte pago");
  }

  @Test
  void doisCadastrosCriamApenasUmPrograma() throws Exception {
    List<Integer> resultados =
        simultaneo(
            () -> {
              try {
                service.criar(negocio, regras(), email);
                return 201;
              } catch (ResponseStatusException ex) {
                return ex.getStatusCode().value();
              }
            });
    assertEquals(List.of(201, 409), resultados);
    assertEquals(1, service.historico(negocio, email).size());
  }

  @Test
  void duasEdicoesDaMesmaVersaoTemUmConflito() throws Exception {
    service.criar(negocio, regras(), email);
    var pedido =
        new AtualizarProgramaRequest(1, "Programa", 8, "Corte grátis", "Um por corte pago");
    List<Integer> resultados =
        simultaneo(
            () -> {
              try {
                service.atualizar(negocio, pedido, email);
                return 200;
              } catch (ResponseStatusException ex) {
                return ex.getStatusCode().value();
              }
            });
    assertEquals(List.of(200, 409), resultados);
    assertEquals(2, service.historico(negocio, email).size());
    assertEquals(2, service.consultar(negocio, email).versao());
  }

  private List<Integer> simultaneo(Callable<Integer> operacao) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch prontas = new CountDownLatch(2);
    CountDownLatch inicio = new CountDownLatch(1);
    Callable<Integer> tarefa =
        () -> {
          prontas.countDown();
          if (!inicio.await(10, TimeUnit.SECONDS))
            throw new IllegalStateException("Tempo de teste excedido");
          return operacao.call();
        };
    try {
      Future<Integer> primeira = pool.submit(tarefa);
      Future<Integer> segunda = pool.submit(tarefa);
      assertTrue(prontas.await(10, TimeUnit.SECONDS));
      inicio.countDown();
      return java.util.stream.Stream.of(
              primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS))
          .sorted()
          .toList();
    } finally {
      inicio.countDown();
      pool.shutdownNow();
    }
  }
}
