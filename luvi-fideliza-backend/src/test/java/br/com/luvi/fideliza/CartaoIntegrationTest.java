package br.com.luvi.fideliza;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.luvi.fideliza.equipe.*;
import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.fidelidade.dto.*;
import br.com.luvi.fideliza.fidelidade.dto.CartaoDtos.*;
import br.com.luvi.fideliza.fidelidade.entity.*;
import br.com.luvi.fideliza.fidelidade.repository.*;
import br.com.luvi.fideliza.fidelidade.service.*;
import br.com.luvi.fideliza.usuario.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:cartoes;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "luvi.bootstrap.enabled=false"
    })
@AutoConfigureMockMvc
@Transactional
class CartaoIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UsuarioRepository usuarios;
  @Autowired EstabelecimentoRepository negocios;
  @Autowired MembroRepository membros;
  @Autowired ProgramaService programas;
  @Autowired CartaoService service;
  @Autowired ParticipacaoRepository participacoes;
  private UUID negocio;
  private UUID outro;

  @BeforeEach
  void preparar() {
    UUID dono = usuario("dono@teste.local", false);
    UUID funcionario = usuario("funcionario@teste.local", false);
    UUID outroDono = usuario("outrodono@teste.local", false);
    usuario("admin@teste.local", true);
    usuario("cliente@teste.local", false);
    usuario("cliente2@teste.local", false);
    negocio =
        negocios
            .save(new Estabelecimento(UUID.randomUUID(), "Barbearia", true, Instant.now()))
            .getId();
    outro =
        negocios.save(new Estabelecimento(UUID.randomUUID(), "Café", true, Instant.now())).getId();
    membros.save(new MembroEstabelecimento(UUID.randomUUID(), dono, negocio, "PROPRIETARIO", true));
    membros.save(
        new MembroEstabelecimento(UUID.randomUUID(), funcionario, negocio, "FUNCIONARIO", true));
    membros.save(
        new MembroEstabelecimento(UUID.randomUUID(), outroDono, outro, "PROPRIETARIO", true));
    programas.criar(
        negocio,
        new ProgramaRequest("Cortes", 2, "Corte grátis", "Um por corte pago"),
        "dono@teste.local");
    programas.criar(
        outro,
        new ProgramaRequest("Cafés", 3, "Café grátis", "Um por café pago"),
        "outrodono@teste.local");
  }

  private UUID usuario(String email, boolean admin) {
    return usuarios
        .save(
            new Usuario(
                UUID.randomUUID(), "Teste", email, "hash-teste", false, admin, true, Instant.now()))
        .getId();
  }

  private Cadastro cadastro() {
    return service
        .cadastrar(negocio, UUID.randomUUID(), "Cliente piloto", "dono@teste.local")
        .dados();
  }

  private String url(UUID id) {
    return "/api/v1/estabelecimentos/" + id + "/participantes";
  }

  @Test
  void cadastroTemCartaoZeroERepeticaoNaoDuplica() {
    UUID chave = UUID.randomUUID();
    var primeiro = service.cadastrar(negocio, chave, "Cliente piloto", "dono@teste.local");
    var repetido = service.cadastrar(negocio, chave, " Cliente piloto ", "dono@teste.local");
    assertFalse(primeiro.repetido());
    assertTrue(repetido.repetido());
    assertEquals(primeiro.dados().participante().id(), repetido.dados().participante().id());
    assertEquals(0, primeiro.dados().cartao().saldo());
    assertEquals(1, service.participantes(negocio, 0, 20, "dono@teste.local").total());
    assertThrows(
        ResponseStatusException.class,
        () -> service.cadastrar(negocio, chave, "Outro nome", "dono@teste.local"));
  }

  @Test
  void creditoRepetidoRetornaSaldoOriginalSemIncrementar() {
    UUID p = cadastro().participante().id(), chave = UUID.randomUUID();
    var primeiro = service.creditar(negocio, p, chave, "Compra 1", "funcionario@teste.local");
    service.creditar(negocio, p, UUID.randomUUID(), "Compra 2", "dono@teste.local");
    var repetido = service.creditar(negocio, p, chave, "Compra 1", "funcionario@teste.local");
    assertTrue(repetido.repetido());
    assertEquals(primeiro.dados(), repetido.dados());
    assertEquals(2, service.historico(negocio, p, 0, 20, "dono@teste.local").total());
    assertEquals(
        2, service.cartoes(negocio, p, 0, 20, "dono@teste.local").conteudo().get(0).saldo());
  }

  @Test
  void mesmaChaveComOutraCompraOuParticipanteDaConflito() {
    UUID p = cadastro().participante().id(),
        outroParticipante = cadastro().participante().id(),
        chave = UUID.randomUUID();
    service.creditar(negocio, p, chave, "Compra 1", "dono@teste.local");
    assertEquals(
        409,
        assertThrows(
                ResponseStatusException.class,
                () -> service.creditar(negocio, p, chave, "Compra 2", "dono@teste.local"))
            .getStatusCode()
            .value());
    assertEquals(
        409,
        assertThrows(
                ResponseStatusException.class,
                () ->
                    service.creditar(
                        negocio, outroParticipante, chave, "Compra 1", "dono@teste.local"))
            .getStatusCode()
            .value());
  }

  @Test
  void cartaoAbertoPreservaRegraENovoCicloUsaNovaVersao() {
    UUID p = cadastro().participante().id();
    programas.atualizar(
        negocio,
        new AtualizarProgramaRequest(1, "Cortes novos", 3, "Barba grátis", "Um por corte pago"),
        "dono@teste.local");
    service.creditar(negocio, p, UUID.randomUUID(), "1", "dono@teste.local");
    var fim = service.creditar(negocio, p, UUID.randomUUID(), "2", "dono@teste.local").dados();
    assertTrue(fim.concluiuCartao());
    service.creditar(negocio, p, UUID.randomUUID(), "3", "dono@teste.local");
    var lista = service.cartoes(negocio, p, 0, 20, "dono@teste.local").conteudo();
    assertEquals(2, lista.size());
    assertEquals(2, lista.get(0).ciclo());
    assertEquals(2, lista.get(0).versao());
    assertEquals(3, lista.get(0).metaCarimbos());
    assertEquals(1, lista.get(0).saldo());
    assertEquals("CONCLUIDO", lista.get(1).status());
    assertEquals(1, lista.get(1).versao());
    assertEquals(2, lista.get(1).saldo());
    assertEquals("Corte grátis", lista.get(1).recompensa());
  }

  @Test
  void consumidorAceitaConviteEVeSomenteSeusCartoes() {
    UUID p = cadastro().participante().id();
    var convite = service.convidar(negocio, p, "dono@teste.local");
    assertTrue(service.aceitar(convite.token(), "cliente@teste.local").vinculadoAoCliente());
    assertEquals(p, service.aceitar(convite.token(), "cliente@teste.local").id());
    assertThrows(
        ResponseStatusException.class,
        () -> service.aceitar(convite.token(), "cliente2@teste.local"));
    assertEquals(1, service.carteira(0, 20, "cliente@teste.local").total());
    assertEquals(0, service.carteira(0, 20, "cliente2@teste.local").total());
    assertFalse(participacoes.findById(p).orElseThrow().getConviteHash().equals(convite.token()));
  }

  @Test
  void contaUnicaPodeTerCartoesEmDoisEstabelecimentos() {
    UUID p = cadastro().participante().id();
    service.aceitar(
        service.convidar(negocio, p, "dono@teste.local").token(), "cliente@teste.local");
    UUID p2 =
        service
            .cadastrar(outro, UUID.randomUUID(), "Mesmo cliente", "outrodono@teste.local")
            .dados()
            .participante()
            .id();
    service.aceitar(
        service.convidar(outro, p2, "outrodono@teste.local").token(), "cliente@teste.local");
    assertEquals(2, service.carteira(0, 20, "cliente@teste.local").total());
    assertEquals(1, service.participantes(negocio, 0, 20, "dono@teste.local").total());
  }

  @Test
  void renovarConviteInvalidaAnteriorERecusaDuplicidadeDeContaNoPrograma() {
    UUID p = cadastro().participante().id();
    var velho = service.convidar(negocio, p, "dono@teste.local");
    var novo = service.convidar(negocio, p, "dono@teste.local");
    assertThrows(
        ResponseStatusException.class, () -> service.aceitar(velho.token(), "cliente@teste.local"));
    service.aceitar(novo.token(), "cliente@teste.local");
    UUID duplicado = cadastro().participante().id();
    var outroConvite = service.convidar(negocio, duplicado, "dono@teste.local");
    assertEquals(
        409,
        assertThrows(
                ResponseStatusException.class,
                () -> service.aceitar(outroConvite.token(), "cliente@teste.local"))
            .getStatusCode()
            .value());
  }

  @Test
  void conviteExpiradoNaoVincula() {
    UUID p = cadastro().participante().id();
    var convite = service.convidar(negocio, p, "dono@teste.local");
    Participacao entidade = participacoes.findById(p).orElseThrow();
    entidade.convidar(entidade.getConviteHash(), Instant.now().minusSeconds(1));
    assertThrows(
        ResponseStatusException.class,
        () -> service.aceitar(convite.token(), "cliente@teste.local"));
    assertNull(entidade.getUsuarioId());
  }

  @Test
  void operacoesExigemVinculoESempreRespeitamEstabelecimento() throws Exception {
    UUID p = cadastro().participante().id();
    String endpoint = url(negocio) + "/" + p + "/carimbos";
    mvc.perform(
            post(endpoint)
                .with(user("cliente@teste.local"))
                .with(csrf())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType("application/json")
                .content("{\"referenciaCompra\":\"1\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post(endpoint)
                .with(user("admin@teste.local").roles("ADMIN"))
                .with(csrf())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType("application/json")
                .content("{\"referenciaCompra\":\"1\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(get(url(outro) + "/" + p + "/cartoes").with(user("dono@teste.local")))
        .andExpect(status().isForbidden());
    mvc.perform(get(url(outro) + "/" + p + "/cartoes").with(user("outrodono@teste.local")))
        .andExpect(status().isNotFound());
  }

  @Test
  void apiCadastroECarimboTem201ENaRepeticao200() throws Exception {
    UUID chave = UUID.randomUUID();
    for (int codigo : new int[] {201, 200})
      mvc.perform(
              post(url(negocio))
                  .with(user("dono@teste.local"))
                  .with(csrf())
                  .header("Idempotency-Key", chave)
                  .contentType("application/json")
                  .content("{\"nomeExibicao\":\"Teste API\"}"))
          .andExpect(status().is(codigo));
    UUID p = cadastro().participante().id();
    chave = UUID.randomUUID();
    for (int codigo : new int[] {201, 200})
      mvc.perform(
              post(url(negocio) + "/" + p + "/carimbos")
                  .with(user("funcionario@teste.local"))
                  .with(csrf())
                  .header("Idempotency-Key", chave)
                  .contentType("application/json")
                  .content("{\"referenciaCompra\":\"Compra API\"}"))
          .andExpect(status().is(codigo))
          .andExpect(jsonPath("$.saldoApos").value(1));
  }

  @Test
  void validaChaveDadosPaginacaoECsrf() throws Exception {
    mvc.perform(
            post(url(negocio))
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"nomeExibicao\":\"Teste\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post(url(negocio))
                .with(user("dono@teste.local"))
                .with(csrf())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType("application/json")
                .content("{\"nomeExibicao\":\" \"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get(url(negocio) + "?tamanho=1000").with(user("dono@teste.local")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post(url(negocio))
                .with(user("dono@teste.local"))
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType("application/json")
                .content("{\"nomeExibicao\":\"Teste\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void cadastroClienteNaoConcedePapelDeOperador() throws Exception {
    mvc.perform(
            post("/api/v1/auth/clientes")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"nome\":\"Consumidor\",\"email\":\"novo@teste.local\",\"senha\":\"SenhaCliente123\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.senhaHash").doesNotExist());
    var result =
        mvc.perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .param("email", "novo@teste.local")
                    .param("password", "SenhaCliente123"))
            .andExpect(status().isNoContent())
            .andReturn();
    mvc.perform(
            get("/api/v1/auth/me")
                .session(
                    (org.springframework.mock.web.MockHttpSession)
                        result.getRequest().getSession(false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.perfis[0]").value("ROLE_CLIENTE"));
    assertFalse(
        usuarios.findByEmailIgnoreCase("novo@teste.local").orElseThrow().getEmailVerificado());
  }
}
