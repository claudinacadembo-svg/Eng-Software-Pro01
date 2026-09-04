package mz.unisced.sge.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import mz.unisced.sge.service.AuditoriaService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Trata as violacoes de RBAC: alem de encaminhar para a pagina de acesso negado,
 * deixa registo em auditoria de quem tentou aceder a que recurso.
 */
@Component
public class AuditoriaAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditoriaService auditoriaService;

    public AuditoriaAccessDeniedHandler(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Override
    public void handle(HttpServletRequest pedido, HttpServletResponse resposta,
                       AccessDeniedException excepcao) throws IOException, ServletException {
        auditoriaService.registarFalha("ACESSO_NEGADO", "Recurso", null,
                pedido.getMethod() + " " + pedido.getRequestURI());
        resposta.sendRedirect(pedido.getContextPath() + "/acesso-negado");
    }
}
