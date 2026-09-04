package mz.unisced.sge.security;

import mz.unisced.sge.model.Utilizador;
import mz.unisced.sge.repository.UtilizadorRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carrega o utilizador e o respectivo RBAC a partir da base de dados. */
@Service
public class UtilizadorDetailsService implements UserDetailsService {

    private final UtilizadorRepository utilizadorRepository;

    public UtilizadorDetailsService(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilizador utilizador = utilizadorRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador nao encontrado: " + username));
        return new UtilizadorAutenticado(utilizador);
    }
}
