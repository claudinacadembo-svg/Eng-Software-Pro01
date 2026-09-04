package mz.unisced.sge.config;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import mz.unisced.sge.model.EstadoExpediente;
import mz.unisced.sge.model.Expediente;
import mz.unisced.sge.model.NivelConfidencialidade;
import mz.unisced.sge.model.Papel;
import mz.unisced.sge.model.Permissao;
import mz.unisced.sge.model.Prioridade;
import mz.unisced.sge.model.TipoExpediente;
import mz.unisced.sge.model.TipoMovimento;
import mz.unisced.sge.model.Tramitacao;
import mz.unisced.sge.model.Utilizador;
import mz.unisced.sge.repository.ExpedienteRepository;
import mz.unisced.sge.repository.PapelRepository;
import mz.unisced.sge.repository.PermissaoRepository;
import mz.unisced.sge.repository.UtilizadorRepository;
import mz.unisced.sge.security.Permissoes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga inicial: cria o catalogo de permissoes, os papeis pre-definidos, as
 * contas de demonstracao e alguns expedientes de exemplo. So corre quando as
 * tabelas estao vazias, pelo que e seguro reiniciar a aplicacao.
 */
@Component
public class CargaInicialDados implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargaInicialDados.class);

    private final PermissaoRepository permissaoRepository;
    private final PapelRepository papelRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final ExpedienteRepository expedienteRepository;
    private final PasswordEncoder passwordEncoder;

    public CargaInicialDados(PermissaoRepository permissaoRepository,
                             PapelRepository papelRepository,
                             UtilizadorRepository utilizadorRepository,
                             ExpedienteRepository expedienteRepository,
                             PasswordEncoder passwordEncoder) {
        this.permissaoRepository = permissaoRepository;
        this.papelRepository = papelRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.expedienteRepository = expedienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Permissao> permissoes = criarPermissoes();
        criarPapeis(permissoes);
        criarUtilizadores();
        criarExpedientesDeDemonstracao();
    }

    private Map<String, Permissao> criarPermissoes() {
        Map<String, Permissao> catalogo = new LinkedHashMap<>();
        registar(catalogo, Permissoes.UTILIZADOR_LER, "Consultar utilizadores", "Utilizadores");
        registar(catalogo, Permissoes.UTILIZADOR_CRIAR, "Criar utilizadores", "Utilizadores");
        registar(catalogo, Permissoes.UTILIZADOR_ACTUALIZAR, "Actualizar utilizadores", "Utilizadores");
        registar(catalogo, Permissoes.UTILIZADOR_ELIMINAR, "Eliminar utilizadores", "Utilizadores");

        registar(catalogo, Permissoes.PAPEL_LER, "Consultar papeis e permissoes", "Papeis");
        registar(catalogo, Permissoes.PAPEL_GERIR, "Criar papeis e atribuir permissoes", "Papeis");

        registar(catalogo, Permissoes.EXPEDIENTE_LER, "Consultar expedientes", "Expedientes");
        registar(catalogo, Permissoes.EXPEDIENTE_CRIAR, "Registar novos expedientes", "Expedientes");
        registar(catalogo, Permissoes.EXPEDIENTE_ACTUALIZAR, "Alterar dados e anexos do expediente", "Expedientes");
        registar(catalogo, Permissoes.EXPEDIENTE_ELIMINAR, "Eliminar expedientes", "Expedientes");
        registar(catalogo, Permissoes.EXPEDIENTE_TRAMITAR, "Encaminhar expedientes", "Expedientes");
        registar(catalogo, Permissoes.EXPEDIENTE_DESPACHAR, "Proferir despachos", "Expedientes");
        registar(catalogo, Permissoes.EXPEDIENTE_ARQUIVAR, "Arquivar e cancelar expedientes", "Expedientes");

        registar(catalogo, Permissoes.AUDITORIA_VER, "Consultar registos de auditoria", "Auditoria");
        registar(catalogo, Permissoes.RELATORIO_VER, "Consultar e exportar relatorios", "Auditoria");
        return catalogo;
    }

    private void registar(Map<String, Permissao> catalogo, String codigo, String descricao, String modulo) {
        Permissao permissao = permissaoRepository.findByCodigo(codigo)
                .orElseGet(() -> permissaoRepository.save(new Permissao(codigo, descricao, modulo)));
        catalogo.put(codigo, permissao);
    }

    private void criarPapeis(Map<String, Permissao> permissoes) {
        criarPapel(Permissoes.PAPEL_ADMINISTRADOR,
                "Administra utilizadores, papeis e permissoes do sistema",
                permissoes, permissoes.keySet().toArray(new String[0]));

        criarPapel(Permissoes.PAPEL_GESTOR_EXPEDIENTE,
                "Chefe de secretaria: conduz todo o ciclo de vida do expediente",
                permissoes,
                Permissoes.EXPEDIENTE_LER, Permissoes.EXPEDIENTE_CRIAR, Permissoes.EXPEDIENTE_ACTUALIZAR,
                Permissoes.EXPEDIENTE_TRAMITAR, Permissoes.EXPEDIENTE_DESPACHAR, Permissoes.EXPEDIENTE_ARQUIVAR,
                Permissoes.UTILIZADOR_LER, Permissoes.RELATORIO_VER);

        criarPapel(Permissoes.PAPEL_TECNICO,
                "Tecnico: regista expedientes e encaminha os que tem a seu cargo",
                permissoes,
                Permissoes.EXPEDIENTE_LER, Permissoes.EXPEDIENTE_CRIAR, Permissoes.EXPEDIENTE_ACTUALIZAR,
                Permissoes.EXPEDIENTE_TRAMITAR);

        criarPapel(Permissoes.PAPEL_AUDITOR,
                "Auditor: acesso apenas de leitura a expedientes, auditoria e relatorios",
                permissoes,
                Permissoes.EXPEDIENTE_LER, Permissoes.AUDITORIA_VER, Permissoes.RELATORIO_VER,
                Permissoes.UTILIZADOR_LER, Permissoes.PAPEL_LER);
    }

    private void criarPapel(String nome, String descricao, Map<String, Permissao> catalogo, String... codigos) {
        if (papelRepository.findByNome(nome).isPresent()) {
            return;
        }
        Papel papel = new Papel(nome, descricao, true);
        Set<Permissao> atribuidas = new LinkedHashSet<>();
        for (String codigo : codigos) {
            Permissao permissao = catalogo.get(codigo);
            if (permissao != null) {
                atribuidas.add(permissao);
            }
        }
        papel.setPermissoes(atribuidas);
        papelRepository.save(papel);
        LOG.info("Papel criado: {} com {} permissoes", nome, atribuidas.size());
    }

    private void criarUtilizadores() {
        criarUtilizador("admin", "Administrador do Sistema", "admin@unisced.ac.mz",
                "admin123", "Direccao", Permissoes.PAPEL_ADMINISTRADOR);
        criarUtilizador("chefe", "Ana Mucavele", "ana.mucavele@unisced.ac.mz",
                "chefe123", "Secretaria Geral", Permissoes.PAPEL_GESTOR_EXPEDIENTE);
        criarUtilizador("tecnico", "Carlos Jamisse", "carlos.jamisse@unisced.ac.mz",
                "tecnico123", "Expediente", Permissoes.PAPEL_TECNICO);
        criarUtilizador("auditor", "Isabel Nhaca", "isabel.nhaca@unisced.ac.mz",
                "auditor123", "Auditoria Interna", Permissoes.PAPEL_AUDITOR);
    }

    private void criarUtilizador(String username, String nome, String email, String palavraPasse,
                                 String departamento, String... papeis) {
        if (utilizadorRepository.existsByUsername(username)) {
            return;
        }
        Utilizador utilizador = new Utilizador(username, nome, email, passwordEncoder.encode(palavraPasse));
        utilizador.setDepartamento(departamento);
        Set<Papel> atribuidos = new HashSet<>();
        Arrays.stream(papeis).forEach(p -> papelRepository.findByNome(p).ifPresent(atribuidos::add));
        utilizador.setPapeis(atribuidos);
        utilizadorRepository.save(utilizador);
        LOG.info("Utilizador criado: {} ({})", username, String.join(", ", papeis));
    }

    private void criarExpedientesDeDemonstracao() {
        if (expedienteRepository.count() > 0) {
            return;
        }
        Utilizador chefe = utilizadorRepository.findByUsername("chefe").orElseThrow();
        Utilizador tecnico = utilizadorRepository.findByUsername("tecnico").orElseThrow();

        Expediente pedido = novoExpediente("EXP-" + LocalDate.now().getYear() + "-0001",
                "Pedido de declaracao de frequencia",
                "Estudante solicita declaracao de frequencia para efeitos de bolsa.",
                TipoExpediente.ENTRADA, Prioridade.NORMAL, NivelConfidencialidade.PUBLICO,
                "Registo Academico", tecnico, tecnico);
        pedido.setPrazo(LocalDate.now().plusDays(5));
        pedido.adicionarTramitacao(new Tramitacao(TipoMovimento.REGISTO, null, tecnico,
                "Expediente registado no sistema.", EstadoExpediente.REGISTADO));
        expedienteRepository.save(pedido);

        Expediente oficio = novoExpediente("EXP-" + LocalDate.now().getYear() + "-0002",
                "Oficio de convocatoria do Conselho Pedagogico",
                "Convocatoria para a reuniao ordinaria do Conselho Pedagogico.",
                TipoExpediente.INTERNO, Prioridade.URGENTE, NivelConfidencialidade.RESTRITO,
                "Direccao da Faculdade", chefe, chefe);
        oficio.setEstado(EstadoExpediente.EM_TRAMITACAO);
        oficio.setPrazo(LocalDate.now().plusDays(2));
        oficio.adicionarTramitacao(new Tramitacao(TipoMovimento.REGISTO, null, chefe,
                "Expediente registado no sistema.", EstadoExpediente.REGISTADO));
        oficio.adicionarTramitacao(new Tramitacao(TipoMovimento.ENCAMINHAMENTO, chefe, tecnico,
                "Preparar lista de presencas.", EstadoExpediente.EM_TRAMITACAO));
        oficio.setResponsavelActual(tecnico);
        expedienteRepository.save(oficio);

        Expediente processo = novoExpediente("EXP-" + LocalDate.now().getYear() + "-0003",
                "Processo disciplinar n.o 12",
                "Documentacao reservada relativa a processo disciplinar em curso.",
                TipoExpediente.INTERNO, Prioridade.MUITO_URGENTE, NivelConfidencialidade.CONFIDENCIAL,
                "Direccao", chefe, chefe);
        processo.adicionarTramitacao(new Tramitacao(TipoMovimento.REGISTO, null, chefe,
                "Expediente confidencial registado.", EstadoExpediente.REGISTADO));
        expedienteRepository.save(processo);

        LOG.info("Criados {} expedientes de demonstracao", expedienteRepository.count());
    }

    private Expediente novoExpediente(String numero, String assunto, String descricao, TipoExpediente tipo,
                                      Prioridade prioridade, NivelConfidencialidade confidencialidade,
                                      String remetente, Utilizador autor, Utilizador responsavel) {
        Expediente expediente = new Expediente();
        expediente.setNumeroRegisto(numero);
        expediente.setAssunto(assunto);
        expediente.setDescricao(descricao);
        expediente.setTipo(tipo);
        expediente.setPrioridade(prioridade);
        expediente.setConfidencialidade(confidencialidade);
        expediente.setRemetente(remetente);
        expediente.setCriadoPor(autor);
        expediente.setResponsavelActual(responsavel);
        return expediente;
    }
}
