package br.com.luvi.fideliza;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.luvi.fideliza.equipe.MembroEstabelecimento;
import br.com.luvi.fideliza.equipe.MembroRepository;
import br.com.luvi.fideliza.estabelecimento.Estabelecimento;
import br.com.luvi.fideliza.estabelecimento.EstabelecimentoRepository;
import br.com.luvi.fideliza.fidelidade.dto.ProgramaRequest;
import br.com.luvi.fideliza.fidelidade.repository.ParticipacaoRepository;
import br.com.luvi.fideliza.fidelidade.service.CartaoService;
import br.com.luvi.fideliza.fidelidade.service.ProgramaService;
import br.com.luvi.fideliza.usuario.Usuario;
import br.com.luvi.fideliza.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:appcliente;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "luvi.bootstrap.enabled=false"
    })
@AutoConfigureMockMvc
@Transactional
class ClienteAppIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UsuarioRepository usuarios;
  @Autowired EstabelecimentoRepository negocios;
  @Autowired MembroRepository membros;
  @Autowired ProgramaService programas;
  @Autowired CartaoService cartoes;
  @Autowired ParticipacaoRepository participacoes;
  @Autowired PasswordEncoder encoder;
  @Autowired ObjectMapper json;

  private UUID negocio;
  private UUID participacao;
  private static final String TELEFONE = "5581999990001";

  @BeforeEach
  void preparar() {
    Usuario dono =
        usuarios.save(
            new Usuario(
                UUID.randomUUID(),
                "Dono",
                "dono@app.local",
                encoder.encode("SenhaTeste123!"),
                false,
                false,
                true,
                Instant.now()));
    Usuario cliente =
        usuarios.save(
            new Usuario(
                UUID.randomUUID(),
                "Cliente",
                null,
                encoder.encode("SenhaTeste123!"),
                false,
                false,
                true,
                Instant.now()));
    cliente.definirTelefone(TELEFONE);
    negocio =
        negocios.save(new Estabelecimento(UUID.randomUUID(), "Barbearia", true, Instant.now())).getId();
    membros.save(new MembroEstabelecimento(UUID.randomUUID(), dono.getId(), negocio, "PROPRIETARIO", true));
    membros.save(new MembroEstabelecimento(UUID.randomUUID(), cliente.getId(), negocio, "FUNCIONARIO", true));
    programas.criar(
        negocio, new ProgramaRequest("Cortes", 3, "Corte grátis", "Um por corte"), "dono@app.local");
    var cadastro = cartoes.cadastrar(negocio, UUID.randomUUID(), "Cliente", "81999990001", "dono@app.local");
    participacao = cadastro.dados().participante().id();
    participacoes.findById(participacao).orElseThrow().vincular(cliente.getId());
  }

  @Test
  void telefoneEntraNoPainelENaCarteira() throws Exception {
    mvc.perform(get("/api/v1/painel/estabelecimentos").with(user(TELEFONE).roles("OPERADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nome").value("Barbearia"));
    mvc.perform(get("/api/v1/me/recompensas").with(user(TELEFONE).roles("CLIENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
    mvc.perform(get("/api/v1/me/cartoes").with(user(TELEFONE).roles("CLIENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));
  }

  @Test
  void tokenLiberaCarteiraSemCsrfERevogacaoEncerra() throws Exception {
    MvcResult emitido =
        mvc.perform(
                post("/api/v1/auth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"identificador\":\"81999990001\",\"senha\":\"SenhaTeste123!\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.identificador").value(TELEFONE))
            .andReturn();
    String token = json.readTree(emitido.getResponse().getContentAsString()).get("token").asText();
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(TELEFONE))
        .andExpect(jsonPath("$.perfis[0]").value("ROLE_OPERADOR"));
    mvc.perform(get("/api/v1/me/cartoes").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));
    mvc.perform(get("/api/v1/me/participacoes/" + participacao + "/qr").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.containsString("participacao=" + participacao)));
    mvc.perform(delete("/api/v1/auth/token").header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void cadastroDoAppNaoExigeCsrfEQrExigeSessaoOuToken() throws Exception {
    mvc.perform(
            post("/api/v1/auth/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Luvi-App", "1")
                .content(
                    "{\"nome\":\"Nova\",\"email\":\"nova@app.local\",\"senha\":\"SenhaTeste123!\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/auth/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Outra\",\"email\":\"outra@app.local\",\"senha\":\"SenhaTeste123!\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/publico/estabelecimentos/" + negocio + "/qrcode"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG));
    mvc.perform(get("/api/v1/publico/estabelecimentos/" + UUID.randomUUID() + "/qrcode"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/me/participacoes/" + participacao + "/qrcode"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/v1/estabelecimentos/" + negocio + "/participantes/" + participacao + "/qrcode")
                .with(user("dono@app.local").roles("OPERADOR")))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG));
    mvc.perform(get("/api/v1/me/participacoes/" + UUID.randomUUID() + "/qr").with(user(TELEFONE).roles("CLIENTE")))
        .andExpect(status().isNotFound());
  }
}
