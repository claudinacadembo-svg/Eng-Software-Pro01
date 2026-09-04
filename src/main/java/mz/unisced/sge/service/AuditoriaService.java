package mz.unisced.sge.service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import mz.unisced.sge.model.LogAuditoria;
import mz.unisced.sge.repository.LogAuditoriaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Servico de auditoria. Grava numa tabela propria todas as accoes relevantes do
 * sistema, incluindo as tentativas de acesso negadas pelo RBAC.
 */
@Service
public class AuditoriaService {

    private final LogAuditoriaRepository logRepository;

    public AuditoriaService(LogAuditoriaRepository logRepository) {
        this.logRepository = logRepository;
    }

    /** Regista uma accao bem sucedida do utilizador actualmente autenticado. */
    public void registar(String accao, String entidade, Long entidadeId, String detalhe) {
        registar(utilizadorCorrente(), accao, entidade, entidadeId, detalhe, true);
    }

    public void registarFalha(String accao, String entidade, Long entidadeId, String detalhe) {
        registar(utilizadorCorrente(), accao, entidade, entidadeId, detalhe, false);
    }

    /**
     * Grava o registo numa transaccao independente, para que a auditoria
     * sobreviva mesmo que a operacao de negocio acabe por ser revertida.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registar(String username, String accao, String entidade, Long entidadeId,
                         String detalhe, boolean sucesso) {
        LogAuditoria log = new LogAuditoria(username, accao, entidade, entidadeId,
                truncar(detalhe), enderecoIp(), sucesso);
        logRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<LogAuditoria> pesquisar(String username, String accao, LocalDateTime inicio,
                                        LocalDateTime fim, Pageable pageable) {
        return logRepository.pesquisar(vazioParaNulo(username), vazioParaNulo(accao), inicio, fim, pageable);
    }

    @Transactional(readOnly = true)
    public List<String> accoesRegistadas() {
        return logRepository.listarAccoes();
    }

    @Transactional(readOnly = true)
    public List<LogAuditoria> ultimosRegistos() {
        return logRepository.findTop10ByOrderByDataHoraDesc();
    }

    @Transactional(readOnly = true)
    public long totalRegistos() {
        return logRepository.count();
    }

    @Transactional(readOnly = true)
    public long totalAcessosNegados() {
        return logRepository.countBySucessoFalse();
    }

    private String utilizadorCorrente() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            return "anonimo";
        }
        return autenticacao.getName();
    }

    private String enderecoIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            HttpServletRequest pedido = atributos.getRequest();
            String encaminhado = pedido.getHeader("X-Forwarded-For");
            return encaminhado != null ? encaminhado : pedido.getRemoteAddr();
        }
        return "-";
    }

    private String truncar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() > 500 ? texto.substring(0, 497) + "..." : texto;
    }

    private String vazioParaNulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor;
    }
}
