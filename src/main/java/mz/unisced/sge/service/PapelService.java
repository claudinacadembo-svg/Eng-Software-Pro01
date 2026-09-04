package mz.unisced.sge.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import mz.unisced.sge.model.Papel;
import mz.unisced.sge.model.Permissao;
import mz.unisced.sge.repository.PapelRepository;
import mz.unisced.sge.repository.PermissaoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administracao do proprio modelo RBAC: criacao de papeis e atribuicao das
 * permissoes que cada papel concede.
 */
@Service
@Transactional
public class PapelService {

    private final PapelRepository papelRepository;
    private final PermissaoRepository permissaoRepository;
    private final AuditoriaService auditoriaService;

    public PapelService(PapelRepository papelRepository,
                        PermissaoRepository permissaoRepository,
                        AuditoriaService auditoriaService) {
        this.papelRepository = papelRepository;
        this.permissaoRepository = permissaoRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAuthority('PAPEL_LER')")
    @Transactional(readOnly = true)
    public List<Papel> listar() {
        return papelRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<Papel> listarSemVerificacao() {
        return papelRepository.findAllByOrderByNomeAsc();
    }

    @PreAuthorize("hasAuthority('PAPEL_LER')")
    @Transactional(readOnly = true)
    public Papel obter(Long id) {
        return papelRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Papel nao encontrado."));
    }

    /** Permissoes agrupadas por modulo, para desenhar o formulario de atribuicao. */
    @PreAuthorize("hasAuthority('PAPEL_LER')")
    @Transactional(readOnly = true)
    public Map<String, List<Permissao>> permissoesPorModulo() {
        return permissaoRepository.findAllByOrderByModuloAscCodigoAsc().stream()
                .collect(Collectors.groupingBy(Permissao::getModulo, LinkedHashMap::new, Collectors.toList()));
    }

    @PreAuthorize("hasAuthority('PAPEL_GERIR')")
    public Papel criar(String nome, String descricao, List<Long> permissoesIds) {
        String nomeNormalizado = nome == null ? "" : nome.trim().toUpperCase().replace(' ', '_');
        if (nomeNormalizado.isBlank()) {
            throw new RegraNegocioException("O nome do papel e obrigatorio.");
        }
        if (papelRepository.findByNome(nomeNormalizado).isPresent()) {
            throw new RegraNegocioException("Ja existe um papel com esse nome.");
        }
        Papel papel = new Papel(nomeNormalizado, descricao == null ? "" : descricao, false);
        papel.setPermissoes(carregarPermissoes(permissoesIds));
        Papel gravado = papelRepository.save(papel);
        auditoriaService.registar("PAPEL_CRIADO", "Papel", gravado.getId(),
                "Criado o papel " + gravado.getNome());
        return gravado;
    }

    @PreAuthorize("hasAuthority('PAPEL_GERIR')")
    public Papel actualizarPermissoes(Long id, String descricao, List<Long> permissoesIds) {
        Papel papel = papelRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Papel nao encontrado."));
        if (descricao != null && !descricao.isBlank()) {
            papel.setDescricao(descricao);
        }
        papel.setPermissoes(carregarPermissoes(permissoesIds));
        Papel gravado = papelRepository.save(papel);
        auditoriaService.registar("PAPEL_ACTUALIZADO", "Papel", gravado.getId(),
                "Permissoes do papel " + gravado.getNome() + ": " + gravado.getPermissoes());
        return gravado;
    }

    @PreAuthorize("hasAuthority('PAPEL_GERIR')")
    public void eliminar(Long id) {
        Papel papel = papelRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Papel nao encontrado."));
        if (papel.isSistema()) {
            throw new RegraNegocioException("Os papeis de sistema nao podem ser eliminados.");
        }
        papelRepository.delete(papel);
        auditoriaService.registar("PAPEL_ELIMINADO", "Papel", id, "Eliminado o papel " + papel.getNome());
    }

    private Set<Permissao> carregarPermissoes(List<Long> permissoesIds) {
        Set<Permissao> permissoes = new LinkedHashSet<>();
        if (permissoesIds != null) {
            permissoes.addAll(permissaoRepository.findAllById(permissoesIds));
        }
        return permissoes;
    }
}
