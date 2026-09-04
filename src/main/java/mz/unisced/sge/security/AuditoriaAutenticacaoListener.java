package mz.unisced.sge.security;

import java.time.LocalDateTime;
import mz.unisced.sge.repository.UtilizadorRepository;
import mz.unisced.sge.service.AuditoriaService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Regista em auditoria as autenticacoes bem e mal sucedidas. */
@Component
public class AuditoriaAutenticacaoListener {

    private final AuditoriaService auditoriaService;
    private final UtilizadorRepository utilizadorRepository;

    public AuditoriaAutenticacaoListener(AuditoriaService auditoriaService,
                                         UtilizadorRepository utilizadorRepository) {
        this.auditoriaService = auditoriaService;
        this.utilizadorRepository = utilizadorRepository;
    }

    @EventListener
    @Transactional
    public void aoAutenticarComSucesso(AuthenticationSuccessEvent evento) {
        String username = evento.getAuthentication().getName();
        utilizadorRepository.findByUsername(username).ifPresent(utilizador -> {
            utilizador.setUltimoAcesso(LocalDateTime.now());
            utilizadorRepository.save(utilizador);
        });
        auditoriaService.registar(username, "LOGIN_SUCESSO", "Utilizador", null,
                "Autenticacao efectuada com sucesso", true);
    }

    @EventListener
    public void aoFalharAutenticacao(AbstractAuthenticationFailureEvent evento) {
        String username = String.valueOf(evento.getAuthentication().getName());
        String motivo = evento.getException().getClass().getSimpleName();
        auditoriaService.registar(username, "LOGIN_FALHADO", "Utilizador", null,
                "Tentativa de autenticacao falhada: " + motivo, false);
    }
}
