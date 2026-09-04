package mz.unisced.sge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mz.unisced.sge.model.NivelConfidencialidade;
import mz.unisced.sge.model.Utilizador;
import mz.unisced.sge.repository.ExpedienteRepository;
import mz.unisced.sge.repository.UtilizadorRepository;
import mz.unisced.sge.service.ExpedienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de integracao do controlo de acesso baseado em papeis. Cada teste
 * autentica-se com uma das contas criadas na carga inicial e verifica se o
 * sistema concede ou nega o acesso de acordo com as permissoes do papel.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sge-testes;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ControloAcessoRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilizadorRepository utilizadorRepository;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private ExpedienteService expedienteService;

    @Test
    @DisplayName("Sem autenticacao o sistema encaminha para o ecra de login")
    void semAutenticacaoEncaminhaParaLogin() throws Exception {
        mockMvc.perform(get("/expedientes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("O administrador acede a gestao de utilizadores")
    void administradorAcedeAUtilizadores() throws Exception {
        mockMvc.perform(get("/utilizadores")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("tecnico")
    @DisplayName("O tecnico nao acede a gestao de utilizadores")
    void tecnicoNaoAcedeAUtilizadores() throws Exception {
        mockMvc.perform(get("/utilizadores"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/acesso-negado"));
    }

    @Test
    @WithUserDetails("tecnico")
    @DisplayName("O tecnico consulta expedientes, para os quais tem permissao")
    void tecnicoConsultaExpedientes() throws Exception {
        mockMvc.perform(get("/expedientes")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("auditor")
    @DisplayName("O auditor nao pode registar expedientes")
    void auditorNaoRegistaExpedientes() throws Exception {
        mockMvc.perform(post("/expedientes").with(csrf())
                        .param("assunto", "Tentativa indevida")
                        .param("remetente", "Externo")
                        .param("tipo", "ENTRADA")
                        .param("prioridade", "NORMAL")
                        .param("confidencialidade", "PUBLICO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/acesso-negado"));
    }

    @Test
    @WithUserDetails("tecnico")
    @DisplayName("O tecnico nao pode despachar expedientes")
    void tecnicoNaoDespacha() throws Exception {
        Long id = expedienteRepository.findAll().get(0).getId();
        mockMvc.perform(post("/expedientes/" + id + "/despachar").with(csrf())
                        .param("decisao", "DEFERIDO")
                        .param("texto", "Deferido sem competencia para o efeito"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/acesso-negado"));
    }

    @Test
    @DisplayName("Expediente confidencial fica invisivel a quem nao e autor, responsavel nem auditor")
    void confidencialidadeRestringeAcesso() {
        Utilizador tecnico = utilizadorRepository.findByUsername("tecnico").orElseThrow();
        Utilizador auditor = utilizadorRepository.findByUsername("auditor").orElseThrow();

        var confidencial = expedienteRepository.findAll().stream()
                .filter(e -> e.getConfidencialidade() == NivelConfidencialidade.CONFIDENCIAL)
                .findFirst()
                .orElseThrow();

        assertThat(expedienteService.podeAceder(confidencial, tecnico)).isFalse();
        assertThat(expedienteService.podeAceder(confidencial, auditor)).isTrue();
    }

    @Test
    @DisplayName("A carga inicial atribui a cada papel o conjunto de permissoes esperado")
    void papeisTemAsPermissoesEsperadas() {
        Utilizador admin = utilizadorRepository.findByUsername("admin").orElseThrow();
        Utilizador tecnico = utilizadorRepository.findByUsername("tecnico").orElseThrow();
        Utilizador auditor = utilizadorRepository.findByUsername("auditor").orElseThrow();

        assertThat(admin.temPermissao("UTILIZADOR_CRIAR")).isTrue();
        assertThat(admin.temPermissao("EXPEDIENTE_DESPACHAR")).isTrue();

        assertThat(tecnico.temPermissao("EXPEDIENTE_CRIAR")).isTrue();
        assertThat(tecnico.temPermissao("EXPEDIENTE_DESPACHAR")).isFalse();
        assertThat(tecnico.temPermissao("AUDITORIA_VER")).isFalse();

        assertThat(auditor.temPermissao("AUDITORIA_VER")).isTrue();
        assertThat(auditor.temPermissao("EXPEDIENTE_CRIAR")).isFalse();
    }
}
