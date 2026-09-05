package mz.unisced.sge.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Expediente (documento administrativo). E a entidade central do sistema e
 * percorre o ciclo de vida REGISTADO -> EM_TRAMITACAO -> DESPACHADO -> ARQUIVADO.
 */
@Entity
@Table(name = "expediente")
public class Expediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numero de registo unico, gerado automaticamente (ex.: EXP-2026-0001). */
    @Column(nullable = false, unique = true, length = 20)
    private String numeroRegisto;

    @NotBlank(message = "O assunto e obrigatorio")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String assunto;

    @Size(max = 2000)
    @Column(length = 2000)
    private String descricao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoExpediente tipo = TipoExpediente.ENTRADA;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridade prioridade = Prioridade.NORMAL;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NivelConfidencialidade confidencialidade = NivelConfidencialidade.PUBLICO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoExpediente estado = EstadoExpediente.REGISTADO;

    @NotBlank(message = "O remetente e obrigatorio")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String remetente;

    @Size(max = 150)
    @Column(length = 150)
    private String destinatarioExterno;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private Utilizador criadoPor;

    /** Utilizador que tem o expediente em maos neste momento. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsavel_id")
    private Utilizador responsavelActual;

    @Column(nullable = false)
    private LocalDateTime dataRegisto = LocalDateTime.now();

    private LocalDateTime dataActualizacao;

    private LocalDate prazo;

    @OneToMany(mappedBy = "expediente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataMovimento ASC")
    private List<Tramitacao> tramitacoes = new ArrayList<>();

    @OneToMany(mappedBy = "expediente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataDespacho ASC")
    private List<Despacho> despachos = new ArrayList<>();

    @OneToMany(mappedBy = "expediente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataCarregamento ASC")
    private List<Anexo> anexos = new ArrayList<>();

    public void adicionarTramitacao(Tramitacao tramitacao) {
        tramitacao.setExpediente(this);
        this.tramitacoes.add(tramitacao);
    }

    public void adicionarDespacho(Despacho despacho) {
        despacho.setExpediente(this);
        this.despachos.add(despacho);
    }

    public void adicionarAnexo(Anexo anexo) {
        anexo.setExpediente(this);
        this.anexos.add(anexo);
    }

    /** Um expediente em estado final (arquivado ou cancelado) ja nao pode ser movimentado. */
    public boolean isEditavel() {
        return !estado.isFinal();
    }

    public boolean isAtrasado() {
        return prazo != null && !estado.isFinal() && prazo.isBefore(LocalDate.now());
    }

    /** Numero de dias decorridos desde que o prazo expirou; zero se estiver em dia. */
    public long getDiasDeAtraso() {
        if (!isAtrasado()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(prazo, LocalDate.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroRegisto() {
        return numeroRegisto;
    }

    public void setNumeroRegisto(String numeroRegisto) {
        this.numeroRegisto = numeroRegisto;
    }

    public String getAssunto() {
        return assunto;
    }

    public void setAssunto(String assunto) {
        this.assunto = assunto;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public TipoExpediente getTipo() {
        return tipo;
    }

    public void setTipo(TipoExpediente tipo) {
        this.tipo = tipo;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(Prioridade prioridade) {
        this.prioridade = prioridade;
    }

    public NivelConfidencialidade getConfidencialidade() {
        return confidencialidade;
    }

    public void setConfidencialidade(NivelConfidencialidade confidencialidade) {
        this.confidencialidade = confidencialidade;
    }

    public EstadoExpediente getEstado() {
        return estado;
    }

    public void setEstado(EstadoExpediente estado) {
        this.estado = estado;
    }

    public String getRemetente() {
        return remetente;
    }

    public void setRemetente(String remetente) {
        this.remetente = remetente;
    }

    public String getDestinatarioExterno() {
        return destinatarioExterno;
    }

    public void setDestinatarioExterno(String destinatarioExterno) {
        this.destinatarioExterno = destinatarioExterno;
    }

    public Utilizador getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(Utilizador criadoPor) {
        this.criadoPor = criadoPor;
    }

    public Utilizador getResponsavelActual() {
        return responsavelActual;
    }

    public void setResponsavelActual(Utilizador responsavelActual) {
        this.responsavelActual = responsavelActual;
    }

    public LocalDateTime getDataRegisto() {
        return dataRegisto;
    }

    public void setDataRegisto(LocalDateTime dataRegisto) {
        this.dataRegisto = dataRegisto;
    }

    public LocalDateTime getDataActualizacao() {
        return dataActualizacao;
    }

    public void setDataActualizacao(LocalDateTime dataActualizacao) {
        this.dataActualizacao = dataActualizacao;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public void setPrazo(LocalDate prazo) {
        this.prazo = prazo;
    }

    public List<Tramitacao> getTramitacoes() {
        return tramitacoes;
    }

    public void setTramitacoes(List<Tramitacao> tramitacoes) {
        this.tramitacoes = tramitacoes;
    }

    public List<Despacho> getDespachos() {
        return despachos;
    }

    public void setDespachos(List<Despacho> despachos) {
        this.despachos = despachos;
    }

    public List<Anexo> getAnexos() {
        return anexos;
    }

    public void setAnexos(List<Anexo> anexos) {
        this.anexos = anexos;
    }

    @Override
    public String toString() {
        return numeroRegisto + " - " + assunto;
    }
}
