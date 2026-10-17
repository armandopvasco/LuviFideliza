package br.com.luvi.fideliza;

import static org.junit.jupiter.api.Assertions.*;

import br.com.luvi.fideliza.equipe.*;
import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.fidelidade.dto.*;
import br.com.luvi.fideliza.fidelidade.service.*;
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
      "spring.datasource.url=jdbc:h2:mem:carimbo-concorrencia;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "luvi.bootstrap.enabled=false"
    })
class CarimboConcorrenciaTest {
  @Autowired UsuarioRepository usuarios;
  @Autowired EstabelecimentoRepository negocios;
  @Autowired MembroRepository membros;
  @Autowired ProgramaService programas;
  @Autowired CartaoService service;
  private UUID negocio;
  private String dono;
  private String cliente1;
  private String cliente2;

  @BeforeEach
  void preparar() {
    String prefixo = UUID.randomUUID().toString();
    dono = prefixo + "@teste.local";
    cliente1 = "c1-" + dono;
    cliente2 = "c2-" + dono;
    UUID usuario = usuario(dono);
    usuario(cliente1);
    usuario(cliente2);
    negocio =
        negocios
            .save(new Estabelecimento(UUID.randomUUID(), "Concorrência", true, Instant.now()))
            .getId();
    membros.save(
        new MembroEstabelecimento(UUID.randomUUID(), usuario, negocio, "PROPRIETARIO", true));
    programas.criar(
        negocio, new ProgramaRequest("Programa", 2, "Serviço grátis", "Uma compra elegível"), dono);
  }

  private UUID usuario(String email) {
    return usuarios
        .save(
            new Usuario(
                UUID.randomUUID(), "Teste", email, "hash-teste", false, false, true, Instant.now()))
        .getId();
  }

  private UUID participante() {
    return service
        .cadastrar(negocio, UUID.randomUUID(), "Cliente", dono)
        .dados()
        .participante()
        .id();
  }

  @Test
  void mesmoCarimboSimultaneoNaoDuplica() throws Exception {
    UUID p = participante(), chave = UUID.randomUUID();
    List<Boolean> repeticoes =
        simultaneo(() -> service.creditar(negocio, p, chave, "Compra única", dono).repetido());
    assertEquals(1, repeticoes.stream().filter(Boolean::booleanValue).count());
    assertEquals(1, service.historico(negocio, p, 0, 20, dono).total());
    assertEquals(1, service.cartoes(negocio, p, 0, 20, dono).conteudo().get(0).saldo());
  }

  @Test
  void comprasDiferentesNaoPerdemSaldo() throws Exception {
    UUID p = participante();
    simultaneo(
        () ->
            service
                .creditar(negocio, p, UUID.randomUUID(), UUID.randomUUID().toString(), dono)
                .dados());
    var c = service.cartoes(negocio, p, 0, 20, dono).conteudo().get(0);
    assertEquals(2, c.saldo());
    assertEquals("CONCLUIDO", c.status());
    assertEquals(2, service.historico(negocio, p, 0, 20, dono).total());
  }

  @Test
  void cadastroSimultaneoNaoDuplicaParticipante() throws Exception {
    UUID chave = UUID.randomUUID();
    var resultados =
        simultaneo(
            () -> service.cadastrar(negocio, chave, "Cliente", dono).dados().participante().id());
    assertEquals(resultados.get(0), resultados.get(1));
    assertEquals(1, service.participantes(negocio, 0, 20, dono).total());
  }

  @Test
  void somenteUmaContaPodeAceitarOMesmoConvite() throws Exception {
    UUID p = participante();
    String token = service.convidar(negocio, p, dono).token();
    var resultados = simultaneoComDuasContas(token);
    assertTrue(resultados.contains(200));
    assertTrue(resultados.contains(400));
    assertEquals(
        1, service.carteira(0, 20, cliente1).total() + service.carteira(0, 20, cliente2).total());
  }

  private List<Integer> simultaneoComDuasContas(String token) throws Exception {
    java.util.concurrent.atomic.AtomicInteger contador =
        new java.util.concurrent.atomic.AtomicInteger();
    return simultaneo(
        () -> {
          String email = contador.getAndIncrement() == 0 ? cliente1 : cliente2;
          try {
            service.aceitar(token, email);
            return 200;
          } catch (ResponseStatusException ex) {
            return ex.getStatusCode().value();
          }
        });
  }

  private <T> List<T> simultaneo(Callable<T> operacao) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch prontas = new CountDownLatch(2), inicio = new CountDownLatch(1);
    Callable<T> tarefa =
        () -> {
          prontas.countDown();
          if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
          return operacao.call();
        };
    try {
      Future<T> primeira = pool.submit(tarefa), segunda = pool.submit(tarefa);
      assertTrue(prontas.await(10, TimeUnit.SECONDS));
      inicio.countDown();
      return List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS));
    } finally {
      inicio.countDown();
      pool.shutdownNow();
    }
  }
}
