package mz.unisced.sge.web;

import jakarta.validation.Valid;
import mz.unisced.sge.model.Anexo;
import mz.unisced.sge.model.DecisaoDespacho;
import mz.unisced.sge.model.EstadoExpediente;
import mz.unisced.sge.model.Expediente;
import mz.unisced.sge.model.NivelConfidencialidade;
import mz.unisced.sge.model.Prioridade;
import mz.unisced.sge.model.TipoExpediente;
import mz.unisced.sge.model.Utilizador;
import mz.unisced.sge.service.ExpedienteService;
import mz.unisced.sge.service.SessaoUtilizador;
import mz.unisced.sge.service.UtilizadorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Modulo de gestao de expedientes: registo, tramitacao, despacho e arquivo. */
@Controller
@RequestMapping("/expedientes")
public class ExpedienteController {

    private static final int TAMANHO_PAGINA = 10;

    private final ExpedienteService expedienteService;
    private final UtilizadorService utilizadorService;
    private final SessaoUtilizador sessao;

    public ExpedienteController(ExpedienteService expedienteService,
                                UtilizadorService utilizadorService,
                                SessaoUtilizador sessao) {
        this.expedienteService = expedienteService;
        this.utilizadorService = utilizadorService;
        this.sessao = sessao;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String texto,
                         @RequestParam(required = false) EstadoExpediente estado,
                         @RequestParam(required = false) TipoExpediente tipo,
                         @RequestParam(defaultValue = "false") boolean atrasados,
                         @RequestParam(defaultValue = "0") int pagina,
                         Model modelo) {
        Page<Expediente> resultado = expedienteService.pesquisar(texto, estado, tipo, atrasados,
                sessao.actual(), PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA));

        modelo.addAttribute("pagina", resultado);
        modelo.addAttribute("texto", texto);
        modelo.addAttribute("estadoSeleccionado", estado);
        modelo.addAttribute("tipoSeleccionado", tipo);
        modelo.addAttribute("apenasAtrasados", atrasados);
        modelo.addAttribute("estados", EstadoExpediente.values());
        modelo.addAttribute("tipos", TipoExpediente.values());
        return "expedientes/lista";
    }

    @GetMapping("/novo")
    public String formularioNovo(Model modelo) {
        modelo.addAttribute("expediente", new Expediente());
        preencherListas(modelo);
        return "expedientes/formulario";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("expediente") Expediente expediente,
                        BindingResult validacao,
                        Model modelo,
                        RedirectAttributes atributos) {
        if (validacao.hasErrors()) {
            preencherListas(modelo);
            return "expedientes/formulario";
        }
        Expediente gravado = expedienteService.criar(expediente, sessao.actual());
        atributos.addFlashAttribute("sucesso",
                "Expediente " + gravado.getNumeroRegisto() + " registado com sucesso.");
        return "redirect:/expedientes/" + gravado.getId();
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model modelo) {
        Utilizador corrente = sessao.actual();
        modelo.addAttribute("expediente", expedienteService.obter(id, corrente));
        modelo.addAttribute("utilizadores", utilizadorService.listarActivos());
        modelo.addAttribute("decisoes", DecisaoDespacho.values());
        modelo.addAttribute("corrente", corrente);
        return "expedientes/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String formularioEdicao(@PathVariable Long id, Model modelo) {
        modelo.addAttribute("expediente", expedienteService.obter(id, sessao.actual()));
        preencherListas(modelo);
        return "expedientes/formulario";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("expediente") Expediente expediente,
                             BindingResult validacao,
                             Model modelo,
                             RedirectAttributes atributos) {
        if (validacao.hasErrors()) {
            preencherListas(modelo);
            return "expedientes/formulario";
        }
        expedienteService.actualizar(id, expediente, sessao.actual());
        atributos.addFlashAttribute("sucesso", "Expediente actualizado com sucesso.");
        return "redirect:/expedientes/" + id;
    }

    @PostMapping("/{id}/tramitar")
    public String tramitar(@PathVariable Long id,
                           @RequestParam Long destinoId,
                           @RequestParam(required = false) String observacao,
                           RedirectAttributes atributos) {
        expedienteService.tramitar(id, destinoId, observacao, sessao.actual());
        atributos.addFlashAttribute("sucesso", "Expediente encaminhado com sucesso.");
        return "redirect:/expedientes/" + id;
    }

    @PostMapping("/{id}/despachar")
    public String despachar(@PathVariable Long id,
                            @RequestParam DecisaoDespacho decisao,
                            @RequestParam String texto,
                            RedirectAttributes atributos) {
        expedienteService.despachar(id, decisao, texto, sessao.actual());
        atributos.addFlashAttribute("sucesso", "Despacho registado com sucesso.");
        return "redirect:/expedientes/" + id;
    }

    @PostMapping("/{id}/arquivar")
    public String arquivar(@PathVariable Long id,
                           @RequestParam(required = false) String observacao,
                           RedirectAttributes atributos) {
        expedienteService.arquivar(id, observacao, sessao.actual());
        atributos.addFlashAttribute("sucesso", "Expediente arquivado.");
        return "redirect:/expedientes/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id,
                           @RequestParam String motivo,
                           RedirectAttributes atributos) {
        expedienteService.cancelar(id, motivo, sessao.actual());
        atributos.addFlashAttribute("sucesso", "Expediente cancelado.");
        return "redirect:/expedientes/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes atributos) {
        expedienteService.eliminar(id);
        atributos.addFlashAttribute("sucesso", "Expediente eliminado.");
        return "redirect:/expedientes";
    }

    @PostMapping("/{id}/anexos")
    public String anexar(@PathVariable Long id,
                         @RequestParam("ficheiro") MultipartFile ficheiro,
                         RedirectAttributes atributos) {
        expedienteService.anexar(id, ficheiro, sessao.actual());
        atributos.addFlashAttribute("sucesso", "Anexo adicionado.");
        return "redirect:/expedientes/" + id;
    }

    @GetMapping("/anexos/{anexoId}")
    public ResponseEntity<byte[]> descarregarAnexo(@PathVariable Long anexoId) {
        Anexo anexo = expedienteService.obterAnexo(anexoId, sessao.actual());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + anexo.getNomeFicheiro() + "\"")
                .contentType(MediaType.parseMediaType(anexo.getTipoConteudo()))
                .body(anexo.getConteudo());
    }

    private void preencherListas(Model modelo) {
        modelo.addAttribute("tipos", TipoExpediente.values());
        modelo.addAttribute("prioridades", Prioridade.values());
        modelo.addAttribute("confidencialidades", NivelConfidencialidade.values());
    }
}
