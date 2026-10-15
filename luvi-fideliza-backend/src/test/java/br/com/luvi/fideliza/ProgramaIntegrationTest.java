package br.com.luvi.fideliza;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.luvi.fideliza.equipe.*;
import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.usuario.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:programas;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "luvi.bootstrap.enabled=false"
    })
@AutoConfigureMockMvc
@Transactional
class ProgramaIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UsuarioRepository usuarios;
  @Autowired EstabelecimentoRepository estabelecimentos;
  @Autowired MembroRepository membros;
  private UUID estabelecimentoId;
  private UUID outroId;
  private UUID donoId;

  @BeforeEach
  void preparar() {
    donoId = usuario("dono@teste.local", false, true);
    UUID funcionario = usuario("funcionario@teste.local", false, true);
    usuario("admin@teste.local", true, true);
    usuario("inativo@teste.local", false, false);
    estabelecimentoId = estabelecimento("Barbearia", true);
    outroId = estabelecimento("Cafeteria", true);
    membros.save(
        new MembroEstabelecimento(
            UUID.randomUUID(), donoId, estabelecimentoId, "PROPRIETARIO", true));
    membros.save(
        new MembroEstabelecimento(
            UUID.randomUUID(), funcionario, estabelecimentoId, "FUNCIONARIO", true));
  }

  private UUID usuario(String email, boolean admin, boolean ativo) {
    return usuarios
        .save(
            new Usuario(
                UUID.randomUUID(),
                "Teste",
                email,
                "hash-nao-usado-no-teste-de-permissao",
                false,
                admin,
                ativo,
                Instant.now()))
        .getId();
  }

  private UUID estabelecimento(String nome, boolean ativo) {
    return estabelecimentos
        .save(new Estabelecimento(UUID.randomUUID(), nome, ativo, Instant.now()))
        .getId();
  }

  private String url(UUID id) {
    return "/api/v1/estabelecimentos/" + id + "/programa";
  }

  private String regras(int meta) {
    return "{\"titulo\":\" Cortes premiados \",\"metaCarimbos\":"
        + meta
        + ",\"recompensa\":\" Um corte grátis \",\"criteriosCompra\":\" Um carimbo por corte pago"
        + " \"}";
  }

  private String alteracao(int versao, int meta) {
    return regras(meta).replaceFirst("\\{", "{\"versaoAtual\":" + versao + ",");
  }

  private ResultActions criar(UUID id, String email) throws Exception {
    return mvc.perform(
        post(url(id))
            .with(user(email))
            .with(csrf())
            .contentType("application/json")
            .content(regras(10)));
  }

  @Test
  void criaConsultaERegistraAutor() throws Exception {
    criar(estabelecimentoId, "dono@teste.local")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.versao").value(1))
        .andExpect(jsonPath("$.titulo").value("Cortes premiados"))
        .andExpect(jsonPath("$.metaCarimbos").value(10))
        .andExpect(jsonPath("$.criadoPor").value(donoId.toString()));
    mvc.perform(get(url(estabelecimentoId)).with(user("dono@teste.local")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recompensa").value("Um corte grátis"));
  }

  @Test
  void naoDuplicaPrograma() throws Exception {
    criar(estabelecimentoId, "dono@teste.local").andExpect(status().isCreated());
    criar(estabelecimentoId, "dono@teste.local").andExpect(status().isConflict());
    mvc.perform(get(url(estabelecimentoId) + "/historico").with(user("dono@teste.local")))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void atualizaSemSobrescreverHistorico() throws Exception {
    JsonNode primeira =
        json.readTree(
            criar(estabelecimentoId, "dono@teste.local")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    mvc.perform(
            put(url(estabelecimentoId))
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType("application/json")
                .content(alteracao(1, 8)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versao").value(2));
    mvc.perform(get(url(estabelecimentoId) + "/historico").with(user("dono@teste.local")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].metaCarimbos").value(8))
        .andExpect(jsonPath("$[1].metaCarimbos").value(10))
        .andExpect(jsonPath("$[1].versaoId").value(primeira.get("versaoId").asText()));
  }

  @Test
  void rejeitaEdicaoSobreVersaoAntiga() throws Exception {
    criar(estabelecimentoId, "dono@teste.local");
    mvc.perform(
            put(url(estabelecimentoId))
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType("application/json")
                .content(alteracao(1, 8)))
        .andExpect(status().isOk());
    mvc.perform(
            put(url(estabelecimentoId))
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType("application/json")
                .content(alteracao(1, 5)))
        .andExpect(status().isConflict());
    mvc.perform(get(url(estabelecimentoId)).with(user("dono@teste.local")))
        .andExpect(jsonPath("$.metaCarimbos").value(8));
  }

  @Test
  void salvarMesmasRegrasNaoCriaVersao() throws Exception {
    criar(estabelecimentoId, "dono@teste.local");
    mvc.perform(
            put(url(estabelecimentoId))
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType("application/json")
                .content(alteracao(1, 10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versao").value(1));
    mvc.perform(get(url(estabelecimentoId) + "/historico").with(user("dono@teste.local")))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void funcionarioConsultaMasNaoAltera() throws Exception {
    criar(estabelecimentoId, "dono@teste.local");
    mvc.perform(get(url(estabelecimentoId)).with(user("funcionario@teste.local")))
        .andExpect(status().isOk());
    mvc.perform(get(url(estabelecimentoId) + "/historico").with(user("funcionario@teste.local")))
        .andExpect(status().isOk());
    criar(estabelecimentoId, "funcionario@teste.local").andExpect(status().isForbidden());
    mvc.perform(
            put(url(estabelecimentoId))
                .with(user("funcionario@teste.local"))
                .with(csrf())
                .contentType("application/json")
                .content(alteracao(1, 8)))
        .andExpect(status().isForbidden());
  }

  @Test
  void outroEstabelecimentoNaoPodeSerConsultadoNemAlterado() throws Exception {
    mvc.perform(get(url(outroId)).with(user("dono@teste.local"))).andExpect(status().isForbidden());
    mvc.perform(get(url(outroId) + "/historico").with(user("dono@teste.local")))
        .andExpect(status().isForbidden());
    criar(outroId, "dono@teste.local").andExpect(status().isForbidden());
    mvc.perform(
            put(url(outroId))
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType("application/json")
                .content(alteracao(1, 8)))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminPodeConsultarMasNaoMudaRegrasSemSerProprietario() throws Exception {
    criar(estabelecimentoId, "dono@teste.local");
    mvc.perform(get(url(estabelecimentoId)).with(user("admin@teste.local").roles("ADMIN")))
        .andExpect(status().isOk());
    criar(outroId, "admin@teste.local").andExpect(status().isForbidden());
  }

  @Test
  void membroRevogadoEUsuarioInativoNaoAcessam() throws Exception {
    membros.deleteAll();
    mvc.perform(get(url(estabelecimentoId)).with(user("dono@teste.local")))
        .andExpect(status().isForbidden());
    mvc.perform(get(url(estabelecimentoId)).with(user("inativo@teste.local")))
        .andExpect(status().isForbidden());
  }

  @Test
  void negocioInativoEProgramaAusente() throws Exception {
    mvc.perform(get(url(estabelecimentoId)).with(user("dono@teste.local")))
        .andExpect(status().isNotFound());
    UUID inativo = estabelecimento("Inativo", false);
    membros.save(
        new MembroEstabelecimento(UUID.randomUUID(), donoId, inativo, "PROPRIETARIO", true));
    criar(inativo, "dono@teste.local").andExpect(status().isNotFound());
  }

  @Test
  void exigeSessaoECsrf() throws Exception {
    mvc.perform(get(url(estabelecimentoId))).andExpect(status().isUnauthorized());
    mvc.perform(
            post(url(estabelecimentoId))
                .with(user("dono@teste.local"))
                .contentType("application/json")
                .content(regras(10)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "1001", "-1", "null", "2.5"})
  void rejeitaMetaInvalida(String meta) throws Exception {
    mvc.perform(
            post(url(estabelecimentoId))
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType("application/json")
                .content(regras(10).replace("\"metaCarimbos\":10", "\"metaCarimbos\":" + meta)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejeitaTituloVazioEAtualizacaoSemVersao() throws Exception {
    mvc.perform(
            post(url(estabelecimentoId))
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType("application/json")
                .content(regras(10).replace(" Cortes premiados ", " ")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put(url(estabelecimentoId))
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType("application/json")
                .content(regras(8)))
        .andExpect(status().isBadRequest());
  }
}
