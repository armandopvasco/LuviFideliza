package br.com.luvi.fideliza;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.luvi.fideliza.equipe.*;
import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.usuario.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:painel-visual;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "luvi.bootstrap.enabled=false"
    })
@AutoConfigureMockMvc
@Transactional
class PainelVisualIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UsuarioRepository usuarios;
  @Autowired EstabelecimentoRepository estabelecimentos;
  @Autowired MembroRepository membros;
  @Autowired ConviteEquipeRepository convites;
  @Autowired ObjectMapper json;
  UUID loja, outra, funcionario;

  @BeforeEach
  void preparar() throws Exception {
    loja = UUID.randomUUID();
    outra = UUID.randomUUID();
    estabelecimentos.save(new Estabelecimento(loja, "Café LUVI", true, Instant.now()));
    estabelecimentos.save(new Estabelecimento(outra, "Outra loja", true, Instant.now()));
    UUID dono = usuario("dono@teste.local", false);
    funcionario = usuario("time@teste.local", false);
    usuario("externo@teste.local", false);
    usuario("admin@teste.local", true);
    membros.save(new MembroEstabelecimento(UUID.randomUUID(), dono, loja, "PROPRIETARIO", true));
    mvc.perform(
            post(base() + "/programa")
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
{"titulo":"Café amigo","metaCarimbos":1,"recompensa":"Um café","criteriosCompra":"Uma compra"}
"""))
        .andExpect(status().isCreated());
  }

  UUID usuario(String email, boolean admin) {
    return usuarios
        .save(
            new Usuario(UUID.randomUUID(), email, email, "hash", false, admin, true, Instant.now()))
        .getId();
  }

  String base() {
    return "/api/v1/estabelecimentos/" + loja;
  }

  String painel() {
    return "/api/v1/painel/estabelecimentos/" + loja;
  }

  String convite(String email) throws Exception {
    var resposta =
        mvc.perform(
                post(base() + "/equipe/convites")
                    .with(user("dono@teste.local"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(new EquipeController.Convidar(email))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(resposta).get("token").asText();
  }

  void aceitar(String token, String email, int status) throws Exception {
    mvc.perform(
            post("/api/v1/me/equipe/aceitar")
                .with(user(email))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new EquipeController.Aceitar(token))))
        .andExpect(status().is(status));
  }

  @Test
  void conviteExigeEmailCorretoEReenvioNaoDuplica() throws Exception {
    String token = convite("TIME@teste.local");
    aceitar(token, "externo@teste.local", 403);
    aceitar(token, "time@teste.local", 200);
    aceitar(token, "time@teste.local", 200);
    var membro = membros.findByUsuarioIdAndEstabelecimentoId(funcionario, loja).orElseThrow();
    assertEquals("FUNCIONARIO", membro.getPapel());
    assertTrue(membro.getAtivo());
    mvc.perform(get(painel() + "/permissoes").with(user("time@teste.local")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.podeEditarPrograma").value(false))
        .andExpect(jsonPath("$.podeOperar").value(true));
  }

  @Test
  void novoConviteInvalidaAnteriorERevogacaoImpedeAceite() throws Exception {
    String primeiro = convite("time@teste.local"), segundo = convite("time@teste.local");
    aceitar(primeiro, "time@teste.local", 400);
    UUID conviteId =
        convites.findByEstabelecimentoIdAndEmail(loja, "time@teste.local").orElseThrow().getId();
    mvc.perform(
            delete(base() + "/equipe/convites/" + conviteId)
                .with(user("dono@teste.local"))
                .with(csrf()))
        .andExpect(status().isNoContent());
    aceitar(segundo, "time@teste.local", 400);
  }

  @Test
  void funcionarioEAdminSemVinculoNaoGerenciamEquipe() throws Exception {
    aceitar(convite("time@teste.local"), "time@teste.local", 200);
    for (String email :
        new String[] {"time@teste.local", "externo@teste.local", "admin@teste.local"}) {
      mvc.perform(get(base() + "/equipe").with(user(email))).andExpect(status().isForbidden());
      mvc.perform(
              post(base() + "/equipe/convites")
                  .with(user(email))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"email\":\"novo@teste.local\"}"))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void desativacaoBloqueiaSessaoExistenteEConviteUsadoNaoReativa() throws Exception {
    String token = convite("time@teste.local");
    aceitar(token, "time@teste.local", 200);
    UUID membro =
        membros.findByUsuarioIdAndEstabelecimentoId(funcionario, loja).orElseThrow().getId();
    mvc.perform(
            patch(base() + "/equipe/" + membro)
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}"))
        .andExpect(status().isNoContent());
    mvc.perform(get(painel() + "/resumo").with(user("time@teste.local")))
        .andExpect(status().isForbidden());
    aceitar(token, "time@teste.local", 400);
    mvc.perform(
            patch(base() + "/equipe/" + membro)
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":true}"))
        .andExpect(status().isNoContent());
    mvc.perform(get(painel() + "/resumo").with(user("time@teste.local")))
        .andExpect(status().isOk());
  }

  @Test
  void proprietarioEOutrosEstabelecimentosSaoProtegidos() throws Exception {
    UUID dono = usuarios.findByEmailIgnoreCase("dono@teste.local").orElseThrow().getId();
    UUID membro = membros.findByUsuarioIdAndEstabelecimentoId(dono, loja).orElseThrow().getId();
    mvc.perform(
            patch(base() + "/equipe/" + membro)
                .with(user("dono@teste.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}"))
        .andExpect(status().isConflict());
    mvc.perform(
            get("/api/v1/painel/estabelecimentos/" + outra + "/resumo")
                .with(user("dono@teste.local")))
        .andExpect(status().isForbidden());
  }

  @Test
  void resumoBuscaRecompensaEResgateRefletemDadosReais() throws Exception {
    String resposta =
        mvc.perform(
                post(base() + "/participantes")
                    .with(user("dono@teste.local"))
                    .with(csrf())
                    .header("Idempotency-Key", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nomeExibicao\":\"Ana 100%\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID p = UUID.fromString(json.readTree(resposta).get("participante").get("id").asText());
    mvc.perform(
            get(base() + "/participantes").param("busca", "100%").with(user("dono@teste.local")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));
    mvc.perform(get(base() + "/participantes").param("busca", "_").with(user("dono@teste.local")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
    mvc.perform(get(base() + "/participantes/" + p).with(user("dono@teste.local")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nomeExibicao").value("Ana 100%"));
    mvc.perform(
            post(base() + "/participantes/" + p + "/carimbos")
                .with(user("dono@teste.local"))
                .with(csrf())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referenciaCompra\":\"Compra1\"}"))
        .andExpect(status().isCreated());
    mvc.perform(get(painel() + "/resumo").with(user("dono@teste.local")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participantes").value(1))
        .andExpect(jsonPath("$.recompensasDisponiveis").value(1))
        .andExpect(jsonPath("$.carimbos").value(1));
    String lista =
        mvc.perform(
                get(painel() + "/recompensas")
                    .param("status", "DISPONIVEL")
                    .with(user("dono@teste.local")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conteudo[0].participante").value("Ana 100%"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String recompensa = json.readTree(lista).get("conteudo").get(0).get("id").asText();
    UUID chave = UUID.randomUUID();
    for (int esperado : new int[] {201, 200})
      mvc.perform(
              post(base() + "/recompensas/" + recompensa + "/resgate")
                  .with(user("dono@teste.local"))
                  .with(csrf())
                  .header("Idempotency-Key", chave))
          .andExpect(status().is(esperado));
    mvc.perform(
            post(base() + "/recompensas/" + recompensa + "/resgate")
                .with(user("dono@teste.local"))
                .with(csrf())
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isConflict());
    mvc.perform(get(painel() + "/resumo").with(user("dono@teste.local")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recompensasDisponiveis").value(0))
        .andExpect(jsonPath("$.recompensasResgatadas").value(1));
  }

  @Test
  void csrfEValidacaoContinuamObrigatorios() throws Exception {
    mvc.perform(
            post(base() + "/equipe/convites")
                .with(user("dono@teste.local"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"time@teste.local\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            get(painel() + "/recompensas")
                .with(user("dono@teste.local"))
                .param("status", "INVALIDO"))
        .andExpect(status().isBadRequest());
    mvc.perform(get(painel() + "/resumo")).andExpect(status().isUnauthorized());
  }

  @Test
  void recursosVisuaisSaoPublicosSemLiberarAPIs() throws Exception {
    for (String path : new String[] {"/painel.html", "/painel.css", "/painel.js", "/convite.html"})
      mvc.perform(get(path)).andExpect(status().isOk());
    mvc.perform(get(base() + "/equipe")).andExpect(status().isUnauthorized());
  }
}
