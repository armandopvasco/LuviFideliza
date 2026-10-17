package br.com.luvi.fideliza;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.luvi.fideliza.equipe.*;
import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.usuario.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:permissoes-painel;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
    "luvi.bootstrap.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class PermissoesPainelIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UsuarioRepository usuarios;
  @Autowired MembroRepository membros;
  @Autowired EstabelecimentoRepository estabelecimentos;
  UUID estabelecimento;

  @BeforeEach
  void preparar() {
    estabelecimento = UUID.randomUUID();
    estabelecimentos.save(new Estabelecimento(estabelecimento, "Loja", true, Instant.now()));
    for (String perfil : new String[]{"dono", "equipe", "admin", "externo", "inativo"}) {
      UUID id = UUID.randomUUID();
      usuarios.save(new Usuario(id, perfil, perfil + "@teste.local", "hash", false,
          perfil.equals("admin"), !perfil.equals("inativo"), Instant.now()));
      if (perfil.equals("dono") || perfil.equals("equipe") || perfil.equals("inativo")) {
        membros.save(new MembroEstabelecimento(UUID.randomUUID(), id, estabelecimento,
            perfil.equals("equipe") ? "FUNCIONARIO" : "PROPRIETARIO", true));
      }
    }
  }

  String url() { return "/api/v1/painel/estabelecimentos/" + estabelecimento + "/permissoes"; }

  @Test
  void donoPodeEditarEOperar() throws Exception {
    mvc.perform(get(url()).with(user("dono@teste.local"))).andExpect(status().isOk())
        .andExpect(jsonPath("$.podeEditarPrograma").value(true))
        .andExpect(jsonPath("$.podeOperar").value(true));
  }

  @Test
  void equipePodeOperarSemEditar() throws Exception {
    mvc.perform(get(url()).with(user("equipe@teste.local"))).andExpect(status().isOk())
        .andExpect(jsonPath("$.podeEditarPrograma").value(false))
        .andExpect(jsonPath("$.podeOperar").value(true));
  }

  @Test
  void administradorSemVinculoSomenteConsulta() throws Exception {
    mvc.perform(get(url()).with(user("admin@teste.local").roles("ADMIN")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.podeEditarPrograma").value(false))
        .andExpect(jsonPath("$.podeOperar").value(false));
  }

  @Test
  void externosEInativosNaoConsultam() throws Exception {
    mvc.perform(get(url()).with(user("externo@teste.local"))).andExpect(status().isForbidden());
    mvc.perform(get(url()).with(user("inativo@teste.local"))).andExpect(status().isForbidden());
  }

  @Test
  void anonimoNaoConsulta() throws Exception {
    mvc.perform(get(url())).andExpect(status().isUnauthorized());
  }

  @Test
  void estabelecimentoInativoNaoConsulta() throws Exception {
    estabelecimentos.save(new Estabelecimento(estabelecimento, "Loja", false, Instant.now()));
    mvc.perform(get(url()).with(user("dono@teste.local"))).andExpect(status().isNotFound());
  }
}
