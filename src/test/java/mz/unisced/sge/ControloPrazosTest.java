package mz.unisced.sge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import mz.unisced.sge.model.EstadoExpediente;
import mz.unisced.sge.model.Expediente;
import mz.unisced.sge.model.Utilizador;
import mz.unisced.sge.repository.ExpedienteRepository;
import mz.unisced.sge.repository.UtilizadorRepository;
import mz.unisced.sge.service.ExpedienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithUserDetails;

/**
 * Testes do controlo de prazos: contagem de expedientes fora de prazo, listagem
 * dos que estao a cargo de um utilizador e filtro da pesquisa.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sge-prazos;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ControloPrazosTest {

    @Autowired
    private ExpedienteService expedienteService;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private UtilizadorRepository utilizadorRepository;

    @Test
    @DisplayName("Um expediente com prazo expirado e contado como atrasado")
    void expedienteComPrazoExpiradoContaComoAtrasado() {
        Expediente expediente = expedienteRepository.findAll().stream()
                .filter(e -> e.getPrazo() != null && e.getPrazo().isBefore(LocalDate.now()))
                .findFirst()
                .orElseThrow();

        assertThat(expediente.isAtrasado()).isTrue();
        assertThat(expediente.getDiasDeAtraso()).isPositive();
        assertThat(expedienteService.totalAtrasados()).isPositive();
    }

    @Test
    @DisplayName("Um expediente arquivado deixa de ser considerado fora de prazo")
    void expedienteArquivadoNaoContaComoAtrasado() {
        Expediente expediente = new Expediente();
        expediente.setPrazo(LocalDate.now().minusDays(10));
        expediente.setEstado(EstadoExpediente.ARQUIVADO);

        assertThat(expediente.isAtrasado()).isFalse();
        assertThat(expediente.getDiasDeAtraso()).isZero();
    }

    @Test
    @DisplayName("Os expedientes atrasados sao atribuidos ao respectivo responsavel")
    void atrasadosSaoListadosPorResponsavel() {
        Expediente atrasado = expedienteRepository.findAll().stream()
                .filter(Expediente::isAtrasado)
                .findFirst()
                .orElseThrow();
        Utilizador responsavel = atrasado.getResponsavelActual();

        List<Expediente> meus = expedienteService.atrasadosDe(responsavel);

        assertThat(meus).isNotEmpty();
        assertThat(meus).allMatch(Expediente::isAtrasado);
        assertThat(meus).extracting(Expediente::getNumeroRegisto)
                .contains(atrasado.getNumeroRegisto());
    }

    @Test
    @WithUserDetails("chefe")
    @DisplayName("O filtro de pesquisa devolve apenas os expedientes fora de prazo")
    void filtroDevolveApenasExpedientesForaDePrazo() {
        Utilizador chefe = utilizadorRepository.findByUsername("chefe").orElseThrow();

        Page<Expediente> todos = expedienteService.pesquisar(null, null, null, false, chefe,
                PageRequest.of(0, 20));
        Page<Expediente> atrasados = expedienteService.pesquisar(null, null, null, true, chefe,
                PageRequest.of(0, 20));

        assertThat(atrasados.getContent()).isNotEmpty();
        assertThat(atrasados.getContent()).allMatch(Expediente::isAtrasado);
        assertThat(atrasados.getTotalElements()).isLessThan(todos.getTotalElements());
    }
}
