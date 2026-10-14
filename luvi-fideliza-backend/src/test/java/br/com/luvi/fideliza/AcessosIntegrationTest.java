package br.com.luvi.fideliza;
import br.com.luvi.fideliza.usuario.*;
import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.autenticacao.*;
import br.com.luvi.fideliza.equipe.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:acessos;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "luvi.bootstrap.enabled=false"})
@AutoConfigureMockMvc @Transactional
class AcessosIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired UsuarioRepository usuarios;
 @Autowired EstabelecimentoRepository negocios;
 @Autowired MembroRepository membros;
 @Autowired ConviteRepository convites;
 @Autowired PasswordEncoder encoder;
 @Autowired AcessoEstabelecimentoService service;
 private UUID negocio; private UUID outro;
 @BeforeEach void preparar() {
  usuarios.save(new Usuario(UUID.randomUUID(),"Admin","admin@teste.local",encoder.encode("SenhaTeste123!"),false,true,true,Instant.now()));
  var owner=usuarios.save(new Usuario(UUID.randomUUID(),"Dono","dono@teste.local",encoder.encode("SenhaTeste123!"),false,false,true,Instant.now()));
  negocio=negocios.save(new Estabelecimento(UUID.randomUUID(),"Barbearia",true,Instant.now())).getId();
  outro=negocios.save(new Estabelecimento(UUID.randomUUID(),"Cafeteria",true,Instant.now())).getId();
  membros.save(new MembroEstabelecimento(UUID.randomUUID(),owner.getId(),negocio,"PROPRIETARIO",true));
 }
 @Test void loginCriaSessaoELogoutEncerra() throws Exception {
  var result=mvc.perform(post("/api/v1/auth/login").with(csrf()).param("email","dono@teste.local").param("password","SenhaTeste123!")).andExpect(status().isNoContent()).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession(false);assertNotNull(session);
  mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("dono@teste.local"));
  mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
  assertTrue(session.isInvalid());
 }
 @Test void senhaIncorretaENaoAutenticado() throws Exception {
  mvc.perform(post("/api/v1/auth/login").with(csrf()).param("email","admin@teste.local").param("password","errada")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
 }
 @Test void donoNaoAcessaOutroNegocioNemAreaAdmin() throws Exception {
  mvc.perform(get("/api/v1/estabelecimentos/"+negocio).with(user("dono@teste.local").roles("OPERADOR"))).andExpect(status().isOk());
  mvc.perform(get("/api/v1/estabelecimentos/"+outro).with(user("dono@teste.local").roles("OPERADOR"))).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/admin/estabelecimentos").with(user("dono@teste.local").roles("OPERADOR")).with(csrf()).contentType("application/json").content("{\"nome\":\"Teste\"}")).andExpect(status().isForbidden());
 }
 @Test void cadastroExigeCsrfEValidaDados() throws Exception {
  mvc.perform(post("/api/v1/admin/estabelecimentos").with(user("admin@teste.local").roles("ADMIN")).contentType("application/json").content("{\"nome\":\"Teste\"}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/admin/estabelecimentos").with(user("admin@teste.local").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"nome\":\"\"}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/v1/admin/estabelecimentos").with(user("admin@teste.local").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"nome\":\"Novo negócio\"}")).andExpect(status().isCreated());
 }
 @Test void conviteCriaProprietarioESoPodeSerUsadoUmaVez() {
  var c=service.convidar(outro,"novo@teste.local","admin@teste.local");
  service.aceitar(c.token(),"Novo dono","SenhaTeste123!");
  var u=usuarios.findByEmailIgnoreCase("novo@teste.local").orElseThrow();
  assertFalse(u.getAdministradorPlataforma());assertFalse(u.getEmailVerificado());assertTrue(encoder.matches("SenhaTeste123!",u.getSenhaHash()));
  assertTrue(membros.existsByUsuarioIdAndEstabelecimentoIdAndAtivoTrue(u.getId(),outro));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.aceitar(c.token(),"Outro","SenhaTeste123!"));
 }
 @Test void conviteExpiradoNaoCriaUsuario() {
  var admin=usuarios.findByEmailIgnoreCase("admin@teste.local").orElseThrow();
  String token="token-expirado";
  String hash;
  try {hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new RuntimeException(e);}
  convites.save(new ConviteProprietario(UUID.randomUUID(),outro,"expirado@teste.local",hash,Instant.now().minusSeconds(1),null,admin.getId()));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.aceitar(token,"Nome","SenhaTeste123!"));
  assertTrue(usuarios.findByEmailIgnoreCase("expirado@teste.local").isEmpty());
 }
}
