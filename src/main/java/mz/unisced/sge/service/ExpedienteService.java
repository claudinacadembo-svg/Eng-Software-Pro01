package mz.unisced.sge.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import mz.unisced.sge.model.Anexo;
import mz.unisced.sge.model.DecisaoDespacho;
import mz.unisced.sge.model.Despacho;
import mz.unisced.sge.model.EstadoExpediente;
import mz.unisced.sge.model.Expediente;
import mz.unisced.sge.model.NivelConfidencialidade;
import mz.unisced.sge.model.TipoExpediente;
import mz.unisced.sge.model.TipoMovimento;
import mz.unisced.sge.model.Tramitacao;
import mz.unisced.sge.model.Utilizador;
import mz.unisced.sge.repository.AnexoRepository;
import mz.unisced.sge.repository.ExpedienteRepository;
import mz.unisced.sge.repository.UtilizadorRepository;
import mz.unisced.sge.security.Permissoes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Regras de negocio do ciclo de vida do expediente: registo, tramitacao,
 * despacho e arquivo. Cada operacao exige a permissao RBAC correspondente e
 * deixa registo de auditoria e de tramitacao.
 */
@Service
@Transactional
public class ExpedienteService {

    private static final long TAMANHO_MAXIMO_ANEXO = 5L * 1024 * 1024;

    /** Estados em que o expediente ja esta concluido e deixa de contar para prazos. */
    private static final List<EstadoExpediente> ESTADOS_FINAIS =
            List.of(EstadoExpediente.ARQUIVADO, EstadoExpediente.CANCELADO);

    private final ExpedienteRepository expedienteRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AnexoRepository anexoRepository;
    private final AuditoriaService auditoriaService;

    public ExpedienteService(ExpedienteRepository expedienteRepository,
                             UtilizadorRepository utilizadorRepository,
                             AnexoRepository anexoRepository,
                             AuditoriaService auditoriaService) {
        this.expedienteRepository = expedienteRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.anexoRepository = anexoRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAuthority('EXPEDIENTE_LER')")
    @Transactional(readOnly = true)
    public Page<Expediente> pesquisar(String texto, EstadoExpediente estado, TipoExpediente tipo,
                                      boolean apenasAtrasados, Utilizador corrente, Pageable pageable) {
        return expedienteRepository.pesquisar(
                (texto == null || texto.isBlank()) ? null : texto.trim(),
                estado,
                tipo,
                apenasAtrasados,
                LocalDate.now(),
                ESTADOS_FINAIS,
                podeVerConfidenciais(corrente),
                NivelConfidencialidade.CONFIDENCIAL,
                corrente.getId(),
                pageable);
    }

    /** Total de expedientes por concluir cujo prazo ja expirou. */
    @Transactional(readOnly = true)
    public long totalAtrasados() {
        return expedienteRepository.contarAtrasados(LocalDate.now(), ESTADOS_FINAIS);
    }

    /** Expedientes fora de prazo que estao a cargo do utilizador indicado. */
    @Transactional(readOnly = true)
    public List<Expediente> atrasadosDe(Utilizador utilizador) {
        return expedienteRepository.atrasadosDe(utilizador, LocalDate.now(), ESTADOS_FINAIS);
    }

    @PreAuthorize("hasAuthority('EXPEDIENTE_LER')")
    @Transactional(readOnly = true)
    public Expediente obter(Long id, Utilizador corrente) {
        Expediente expediente = expedienteRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Expediente nao encontrado."));
        garantirAcessoAoConteudo(expediente, corrente);
        // Forca a leitura das coleccoes ainda dentro da transaccao.
        expediente.getTramitacoes().size();
        expediente.getDespachos().size();
        expediente.getAnexos().size();
        return expediente;
    }

    @Transactional(readOnly = true)
    public List<Expediente> pendentesDe(Utilizador utilizador) {
        return expedienteRepository.findByResponsavelActualAndEstadoNotInOrderByDataRegistoDesc(
                utilizador, ESTADOS_FINAIS);
    }

    @PreAuthorize("hasAuthority('EXPEDIENTE_CRIAR')")
    public Expediente criar(Expediente expediente, Utilizador autor) {
        expediente.setNumeroRegisto(gerarNumeroRegisto());
        expediente.setCriadoPor(autor);
        expediente.setResponsavelActual(autor);
        expediente.setEstado(EstadoExpediente.REGISTADO);
        expediente.setDataRegisto(LocalDateTime.now());
        expediente.setDataActualizacao(LocalDateTime.now());
        expediente.adicionarTramitacao(new Tramitacao(TipoMovimento.REGISTO, null, autor,
                "Expediente registado no sistema.", EstadoExpediente.REGISTADO));

        Expediente gravado = expedienteRepository.save(expediente);
        auditoriaService.registar("EXPEDIENTE_CRIADO", "Expediente", gravado.getId(),
                "Registado o expediente " + gravado.getNumeroRegisto());
        return gravado;
    }

    @PreAuthorize("hasAuthority('EXPEDIENTE_ACTUALIZAR')")
    public Expediente actualizar(Long id, Expediente dados, Utilizador corrente) {
        Expediente existente = carregarEditavel(id, corrente);
        existente.setAssunto(dados.getAssunto());
        existente.setDescricao(dados.getDescricao());
        existente.setTipo(dados.getTipo());
        existente.setPrioridade(dados.getPrioridade());
        existente.setConfidencialidade(dados.getConfidencialidade());
        existente.setRemetente(dados.getRemetente());
        existente.setDestinatarioExterno(dados.getDestinatarioExterno());
        existente.setPrazo(dados.getPrazo());
        existente.setDataActualizacao(LocalDateTime.now());

        Expediente gravado = expedienteRepository.save(existente);
        auditoriaService.registar("EXPEDIENTE_ACTUALIZADO", "Expediente", gravado.getId(),
                "Alterado o expediente " + gravado.getNumeroRegisto());
        return gravado;
    }

    @PreAuthorize("hasAuthority('EXPEDIENTE_TRAMITAR')")
    public Expediente tramitar(Long id, Long destinoId, String observacao, Utilizador corrente) {
        Expediente expediente = carregarEditavel(id, corrente);
        garantirQuePodeMovimentar(expediente, corrente);

        Utilizador destino = utilizadorRepository.findById(destinoId)
                .orElseThrow(() -> new RegraNegocioException("Destinatario nao encontrado."));
        if (!destino.isActivo()) {
            throw new RegraNegocioException("Nao e possivel encaminhar para um utilizador inactivo.");
        }
        if (destino.equals(expediente.getResponsavelActual())
                || (expediente.getResponsavelActual() != null
                    && destino.getId().equals(expediente.getResponsavelActual().getId()))) {
            throw new RegraNegocioException("O expediente ja se encontra com esse utilizador.");
        }

        Utilizador origem = expediente.getResponsavelActual();
        expediente.setResponsavelActual(destino);
        expediente.setEstado(EstadoExpediente.EM_TRAMITACAO);
        expediente.setDataActualizacao(LocalDateTime.now());
        expediente.adicionarTramitacao(new Tramitacao(TipoMovimento.ENCAMINHAMENTO, origem, destino,
                observacao, EstadoExpediente.EM_TRAMITACAO));

        Expediente gravado = expedienteRepository.save(expediente);
        auditoriaService.registar("EXPEDIENTE_TRAMITADO", "Expediente", gravado.getId(),
                "Expediente " + gravado.getNumeroRegisto() + " encaminhado para " + destino.getUsername());
        return gravado;
    }

    @PreAuthorize("hasAuthority('EXPEDIENTE_DESPACHAR')")
    public Expediente despachar(Long id, DecisaoDespacho decisao, String texto, Utilizador corrente) {
        if (texto == null || texto.isBlank()) {
            throw new RegraNegocioException("O texto do despacho e obrigatorio.");
        }
        Expediente expediente = carregarEditavel(id, corrente);

        expediente.adicionarDespacho(new Despacho(corrente, decisao, texto));
        expediente.setEstado(EstadoExpediente.DESPACHADO);
        expediente.setResponsavelActual(corrente);
        expediente.setDataActualizacao(LocalDateTime.now());
        expediente.adicionarTramitacao(new Tramitacao(TipoMovimento.DESPACHO, corrente, corrente,
                decisao.getDescricao() + ": " + texto, EstadoExpediente.DESPACHADO));

        Expediente gravado = expedienteRepository.save(expediente);
        auditoriaService.registar("EXPEDIENTE_DESPACHADO", "Expediente", gravado.getId(),
                "Despacho " + decisao.name() + " no expediente " + gravado.getNumeroRegisto());
        return gravado;
    }

    @PreAuthorize("hasAuthority('EXPEDIENTE_ARQUIVAR')")
    public Expediente arquivar(Long id, String observacao, Utilizador corrente) {
        Expediente expediente = carregarEditavel(id, corrente);
        expediente.setEstado(EstadoExpediente.ARQUIVADO);
        expediente.setDataActualizacao(LocalDateTime.now());
        expediente.adicionarTramitacao(new Tramitacao(TipoMovimento.ARQUIVAMENTO, corrente, null,
                observacao == null || observacao.isBlank() ? "Expediente arquivado." : observacao,
                EstadoExpediente.ARQUIVADO));

        Expediente gravado = expedienteRepository.save(expediente);
        auditoriaService.registar("EXPEDIENTE_ARQUIVADO", "Expediente", gravado.getId(),
                "Arquivado o expediente " + gravado.getNumeroRegisto());
        return gravado;
    }

    @PreAuthorize("hasAuthority('EXPEDIENTE_ARQUIVAR')")
    public Expediente cancelar(Long id, String motivo, Utilizador corrente) {
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("Indique o motivo do cancelamento.");
        }
        Expediente expediente = carregarEditavel(id, corrente);
        expediente.setEstado(EstadoExpediente.CANCELADO);
        expediente.setDataActualizacao(LocalDateTime.now());
        expediente.adicionarTramitacao(new Tramitacao(TipoMovimento.CANCELAMENTO, corrente, null,
                motivo, EstadoExpediente.CANCELADO));

        Expediente gravado = expedienteRepository.save(expediente);
        auditoriaService.registar("EXPEDIENTE_CANCELADO", "Expediente", gravado.getId(),
                "Cancelado o expediente " + gravado.getNumeroRegisto() + ": " + motivo);
        return gravado;
    }

    @PreAuthorize("hasAuthority('EXPEDIENTE_ELIMINAR')")
    public void eliminar(Long id) {
        Expediente expediente = expedienteRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Expediente nao encontrado."));
        expedienteRepository.delete(expediente);
        auditoriaService.registar("EXPEDIENTE_ELIMINADO", "Expediente", id,
                "Eliminado o expediente " + expediente.getNumeroRegisto());
    }

    @PreAuthorize("hasAuthority('EXPEDIENTE_ACTUALIZAR')")
    public void anexar(Long id, MultipartFile ficheiro, Utilizador corrente) {
        if (ficheiro == null || ficheiro.isEmpty()) {
            throw new RegraNegocioException("Seleccione um ficheiro para anexar.");
        }
        if (ficheiro.getSize() > TAMANHO_MAXIMO_ANEXO) {
            throw new RegraNegocioException("O anexo nao pode exceder 5 MB.");
        }
        Expediente expediente = carregarEditavel(id, corrente);

        Anexo anexo = new Anexo();
        anexo.setNomeFicheiro(ficheiro.getOriginalFilename());
        anexo.setTipoConteudo(ficheiro.getContentType() == null
                ? "application/octet-stream" : ficheiro.getContentType());
        anexo.setTamanho(ficheiro.getSize());
        anexo.setCarregadoPor(corrente);
        try {
            anexo.setConteudo(ficheiro.getBytes());
        } catch (IOException excepcao) {
            throw new RegraNegocioException("Nao foi possivel ler o ficheiro enviado.");
        }
        expediente.adicionarAnexo(anexo);
        expedienteRepository.save(expediente);
        auditoriaService.registar("ANEXO_ADICIONADO", "Expediente", expediente.getId(),
                "Anexado o ficheiro " + anexo.getNomeFicheiro());
    }

    @PreAuthorize("hasAuthority('EXPEDIENTE_LER')")
    @Transactional(readOnly = true)
    public Anexo obterAnexo(Long anexoId, Utilizador corrente) {
        Anexo anexo = anexoRepository.findById(anexoId)
                .orElseThrow(() -> new RegraNegocioException("Anexo nao encontrado."));
        garantirAcessoAoConteudo(anexo.getExpediente(), corrente);
        auditoriaService.registar("ANEXO_DESCARREGADO", "Anexo", anexoId,
                "Descarregado o ficheiro " + anexo.getNomeFicheiro());
        return anexo;
    }

    /**
     * Regra de confidencialidade aplicada sobre o RBAC: um expediente
     * CONFIDENCIAL so e acessivel a quem o criou, a quem o tem em maos, ao
     * administrador e a quem tem permissao de auditoria.
     */
    public boolean podeAceder(Expediente expediente, Utilizador utilizador) {
        if (expediente.getConfidencialidade() != NivelConfidencialidade.CONFIDENCIAL) {
            return true;
        }
        if (podeVerConfidenciais(utilizador)) {
            return true;
        }
        boolean autor = expediente.getCriadoPor() != null
                && expediente.getCriadoPor().getId().equals(utilizador.getId());
        boolean responsavel = expediente.getResponsavelActual() != null
                && expediente.getResponsavelActual().getId().equals(utilizador.getId());
        return autor || responsavel;
    }

    private boolean podeVerConfidenciais(Utilizador utilizador) {
        return utilizador.temPapel(Permissoes.PAPEL_ADMINISTRADOR)
                || utilizador.temPermissao(Permissoes.AUDITORIA_VER);
    }

    private void garantirAcessoAoConteudo(Expediente expediente, Utilizador corrente) {
        if (!podeAceder(expediente, corrente)) {
            auditoriaService.registarFalha("ACESSO_NEGADO", "Expediente", expediente.getId(),
                    "Tentativa de acesso a expediente confidencial " + expediente.getNumeroRegisto());
            throw new AccessDeniedException("Expediente confidencial: acesso reservado.");
        }
    }

    private Expediente carregarEditavel(Long id, Utilizador corrente) {
        Expediente expediente = expedienteRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Expediente nao encontrado."));
        garantirAcessoAoConteudo(expediente, corrente);
        if (!expediente.isEditavel()) {
            throw new RegraNegocioException("O expediente esta " + expediente.getEstado().getDescricao()
                    + " e ja nao admite movimentacao.");
        }
        return expediente;
    }

    /** Quem nao tem o expediente em maos precisa de privilegio de despacho para o movimentar. */
    private void garantirQuePodeMovimentar(Expediente expediente, Utilizador corrente) {
        boolean responsavel = expediente.getResponsavelActual() != null
                && expediente.getResponsavelActual().getId().equals(corrente.getId());
        if (!responsavel && !corrente.temPermissao(Permissoes.EXPEDIENTE_DESPACHAR)) {
            auditoriaService.registarFalha("ACESSO_NEGADO", "Expediente", expediente.getId(),
                    "Tentativa de tramitar expediente sob responsabilidade de outro utilizador");
            throw new AccessDeniedException("O expediente esta a cargo de outro utilizador.");
        }
    }

    /** Gera o proximo numero de registo do ano corrente, no formato EXP-AAAA-NNNN. */
    private String gerarNumeroRegisto() {
        String prefixo = "EXP-" + Year.now().getValue() + "-";
        String maior = expedienteRepository.maiorNumeroRegistoComPrefixo(prefixo);
        int proximo = 1;
        if (maior != null) {
            proximo = Integer.parseInt(maior.substring(prefixo.length())) + 1;
        }
        String numero = prefixo + String.format("%04d", proximo);
        while (expedienteRepository.existsByNumeroRegisto(numero)) {
            proximo++;
            numero = prefixo + String.format("%04d", proximo);
        }
        return numero;
    }
}
