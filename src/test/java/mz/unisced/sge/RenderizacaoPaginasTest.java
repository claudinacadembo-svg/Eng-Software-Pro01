package mz.unisced.sge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mz.unisced.sge.repository.ExpedienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Garante que todos os ecras da aplicacao sao produzidos sem erros de
 * apresentacao para um utilizador com acesso total.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sge-testes-paginas;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class RenderizacaoPaginasTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Test
    @DisplayName("O ecra de autenticacao e publico")
    void ecraDeLogin() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("Todos os ecras do administrador sao apresentados sem erros")
    void ecrasDoAdministrador() throws Exception {
        for (String caminho : new String[]{"/", "/expedientes", "/expedientes/novo", "/utilizadores",
                "/utilizadores/novo", "/papeis", "/auditoria", "/relatorios", "/perfil", "/acesso-negado"}) {
            mockMvc.perform(get(caminho)).andExpect(status().isOk());
        }

        Long idExpediente = expedienteRepository.findAll().get(0).getId();
        mockMvc.perform(get("/expedientes/" + idExpediente)).andExpect(status().isOk());
        mockMvc.perform(get("/expedientes/" + idExpediente + "/editar")).andExpect(status().isOk());
        mockMvc.perform(get("/papeis/1/editar")).andExpect(status().isOk());
        mockMvc.perform(get("/utilizadores/1/editar")).andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/csv")).andExpect(status().isOk());
    }
}
