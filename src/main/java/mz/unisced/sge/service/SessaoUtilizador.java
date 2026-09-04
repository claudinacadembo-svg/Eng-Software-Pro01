package mz.unisced.sge.service;

import mz.unisced.sge.model.Utilizador;
import mz.unisced.sge.repository.UtilizadorRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Devolve o utilizador autenticado ja recarregado da base de dados, para que as
 * operacoes trabalhem sempre com os papeis e permissoes actuais.
 */
@Component
public class SessaoUtilizador {

    private final UtilizadorRepository utilizadorRepository;

    public SessaoUtilizador(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    @Transactional(readOnly = true)
    public Utilizador actual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            throw new RegraNegocioException("Nao existe utilizador autenticado.");
        }
        return utilizadorRepository.findByUsername(autenticacao.getName())
                .orElseThrow(() -> new RegraNegocioException("Utilizador autenticado inexistente."));
    }
}
