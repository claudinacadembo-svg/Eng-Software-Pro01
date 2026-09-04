package mz.unisced.sge.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mz.unisced.sge.model.Papel;
import mz.unisced.sge.model.Utilizador;
import mz.unisced.sge.repository.PapelRepository;
import mz.unisced.sge.repository.UtilizadorRepository;
import mz.unisced.sge.security.Permissoes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestao de utilizadores e atribuicao de papeis. Todas as operacoes estao
 * protegidas por permissoes RBAC verificadas ao nivel do metodo.
 */
@Service
@Transactional
public class UtilizadorService {

    private final UtilizadorRepository utilizadorRepository;
    private final PapelRepository papelRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UtilizadorService(UtilizadorRepository utilizadorRepository,
                             PapelRepository papelRepository,
                             PasswordEncoder passwordEncoder,
                             AuditoriaService auditoriaService) {
        this.utilizadorRepository = utilizadorRepository;
        this.papelRepository = papelRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAuthority('UTILIZADOR_LER')")
    @Transactional(readOnly = true)
    public List<Utilizador> listar() {
        return utilizadorRepository.findAllByOrderByNomeCompletoAsc();
    }

    @PreAuthorize("hasAuthority('UTILIZADOR_LER')")
    @Transactional(readOnly = true)
    public Utilizador obter(Long id) {
        return utilizadorRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Utilizador nao encontrado."));
    }

    @Transactional(readOnly = true)
    public Utilizador obterPorUsername(String username) {
        return utilizadorRepository.findByUsername(username)
                .orElseThrow(() -> new RegraNegocioException("Utilizador nao encontrado: " + username));
    }

    @Transactional(readOnly = true)
    public List<Utilizador> listarActivos() {
        return utilizadorRepository.findByActivoTrueOrderByNomeCompletoAsc();
    }

    @PreAuthorize("hasAuthority('UTILIZADOR_CRIAR')")
    public Utilizador criar(Utilizador utilizador, String palavraPasse, List<Long> papeisIds) {
        if (utilizadorRepository.existsByUsername(utilizador.getUsername())) {
            throw new RegraNegocioException("Ja existe um utilizador com o nome de utilizador indicado.");
        }
        if (utilizadorRepository.existsByEmail(utilizador.getEmail())) {
            throw new RegraNegocioException("Ja existe um utilizador com o email indicado.");
        }
        if (palavraPasse == null || palavraPasse.length() < 6) {
            throw new RegraNegocioException("A palavra-passe deve ter pelo menos 6 caracteres.");
        }
        utilizador.setPalavraPasse(passwordEncoder.encode(palavraPasse));
        utilizador.setPapeis(carregarPapeis(papeisIds));
        Utilizador gravado = utilizadorRepository.save(utilizador);
        auditoriaService.registar("UTILIZADOR_CRIADO", "Utilizador", gravado.getId(),
                "Criado o utilizador " + gravado.getUsername() + " com os papeis " + gravado.getPapeis());
        return gravado;
    }

    @PreAuthorize("hasAuthority('UTILIZADOR_ACTUALIZAR')")
    public Utilizador actualizar(Long id, Utilizador dados, String novaPalavraPasse, List<Long> papeisIds) {
        Utilizador existente = utilizadorRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Utilizador nao encontrado."));

        utilizadorRepository.findByUsername(dados.getUsername())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegraNegocioException("O nome de utilizador ja esta atribuido a outra pessoa.");
                });
        utilizadorRepository.findByEmail(dados.getEmail())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegraNegocioException("O email ja esta atribuido a outra pessoa.");
                });

        existente.setUsername(dados.getUsername());
        existente.setNomeCompleto(dados.getNomeCompleto());
        existente.setEmail(dados.getEmail());
        existente.setDepartamento(dados.getDepartamento());
        existente.setActivo(dados.isActivo());
        existente.setPapeis(carregarPapeis(papeisIds));

        if (novaPalavraPasse != null && !novaPalavraPasse.isBlank()) {
            if (novaPalavraPasse.length() < 6) {
                throw new RegraNegocioException("A palavra-passe deve ter pelo menos 6 caracteres.");
            }
            existente.setPalavraPasse(passwordEncoder.encode(novaPalavraPasse));
        }

        garantirQueSobraAdministrador(existente);

        Utilizador gravado = utilizadorRepository.save(existente);
        auditoriaService.registar("UTILIZADOR_ACTUALIZADO", "Utilizador", gravado.getId(),
                "Actualizado o utilizador " + gravado.getUsername() + "; papeis: " + gravado.getPapeis());
        return gravado;
    }

    @PreAuthorize("hasAuthority('UTILIZADOR_ELIMINAR')")
    public void eliminar(Long id, String usernameCorrente) {
        Utilizador utilizador = utilizadorRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Utilizador nao encontrado."));
        if (utilizador.getUsername().equals(usernameCorrente)) {
            throw new RegraNegocioException("Nao pode eliminar a sua propria conta.");
        }
        utilizador.setActivo(false);
        garantirQueSobraAdministrador(utilizador);
        utilizadorRepository.delete(utilizador);
        auditoriaService.registar("UTILIZADOR_ELIMINADO", "Utilizador", id,
                "Eliminado o utilizador " + utilizador.getUsername());
    }

    @PreAuthorize("hasAuthority('UTILIZADOR_ACTUALIZAR')")
    public void alternarActivacao(Long id, String usernameCorrente) {
        Utilizador utilizador = utilizadorRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Utilizador nao encontrado."));
        if (utilizador.getUsername().equals(usernameCorrente)) {
            throw new RegraNegocioException("Nao pode desactivar a sua propria conta.");
        }
        utilizador.setActivo(!utilizador.isActivo());
        garantirQueSobraAdministrador(utilizador);
        utilizadorRepository.save(utilizador);
        auditoriaService.registar(utilizador.isActivo() ? "UTILIZADOR_ACTIVADO" : "UTILIZADOR_DESACTIVADO",
                "Utilizador", id, "Utilizador " + utilizador.getUsername());
    }

    /** Alteracao da propria palavra-passe, disponivel a qualquer utilizador autenticado. */
    public void alterarPalavraPassePropria(String username, String actual, String nova) {
        Utilizador utilizador = obterPorUsername(username);
        if (!passwordEncoder.matches(actual, utilizador.getPalavraPasse())) {
            auditoriaService.registarFalha("PALAVRA_PASSE_ALTERADA", "Utilizador", utilizador.getId(),
                    "Palavra-passe actual incorrecta");
            throw new RegraNegocioException("A palavra-passe actual esta incorrecta.");
        }
        if (nova == null || nova.length() < 6) {
            throw new RegraNegocioException("A nova palavra-passe deve ter pelo menos 6 caracteres.");
        }
        utilizador.setPalavraPasse(passwordEncoder.encode(nova));
        utilizadorRepository.save(utilizador);
        auditoriaService.registar("PALAVRA_PASSE_ALTERADA", "Utilizador", utilizador.getId(),
                "O utilizador alterou a sua palavra-passe");
    }

    private Set<Papel> carregarPapeis(List<Long> papeisIds) {
        Set<Papel> papeis = new HashSet<>();
        if (papeisIds != null) {
            papeis.addAll(papelRepository.findAllById(papeisIds));
        }
        if (papeis.isEmpty()) {
            throw new RegraNegocioException("Atribua pelo menos um papel ao utilizador.");
        }
        return papeis;
    }

    /**
     * Impede que a alteracao em curso deixe o sistema sem nenhum administrador
     * activo - situacao que tornaria o RBAC impossivel de gerir.
     */
    private void garantirQueSobraAdministrador(Utilizador alterado) {
        boolean continuaAdministradorActivo = alterado.isActivo()
                && alterado.temPapel(Permissoes.PAPEL_ADMINISTRADOR);
        if (continuaAdministradorActivo) {
            return;
        }
        boolean existeOutro = utilizadorRepository.findAll().stream()
                .anyMatch(u -> !u.getId().equals(alterado.getId())
                        && u.isActivo()
                        && u.temPapel(Permissoes.PAPEL_ADMINISTRADOR));
        if (!existeOutro) {
            throw new RegraNegocioException("O sistema tem de manter pelo menos um administrador activo.");
        }
    }
}
