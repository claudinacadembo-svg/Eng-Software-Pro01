package mz.unisced.sge.web;

import jakarta.validation.Valid;
import java.util.List;
import mz.unisced.sge.model.Utilizador;
import mz.unisced.sge.service.PapelService;
import mz.unisced.sge.service.SessaoUtilizador;
import mz.unisced.sge.service.UtilizadorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Modulo de gestao de utilizadores e atribuicao de papeis. */
@Controller
@RequestMapping("/utilizadores")
public class UtilizadorController {

    private final UtilizadorService utilizadorService;
    private final PapelService papelService;
    private final SessaoUtilizador sessao;

    public UtilizadorController(UtilizadorService utilizadorService,
                                PapelService papelService,
                                SessaoUtilizador sessao) {
        this.utilizadorService = utilizadorService;
        this.papelService = papelService;
        this.sessao = sessao;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("utilizadores", utilizadorService.listar());
        return "utilizadores/lista";
    }

    @GetMapping("/novo")
    public String formularioNovo(Model modelo) {
        modelo.addAttribute("utilizador", new Utilizador());
        modelo.addAttribute("papeis", papelService.listarSemVerificacao());
        modelo.addAttribute("papeisSeleccionados", List.of());
        return "utilizadores/formulario";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("utilizador") Utilizador utilizador,
                        BindingResult validacao,
                        @RequestParam(required = false) String palavraPasse,
                        @RequestParam(required = false) List<Long> papeisIds,
                        Model modelo,
                        RedirectAttributes atributos) {
        if (validacao.hasErrors()) {
            modelo.addAttribute("papeis", papelService.listarSemVerificacao());
            modelo.addAttribute("papeisSeleccionados", papeisIds == null ? List.of() : papeisIds);
            return "utilizadores/formulario";
        }
        utilizadorService.criar(utilizador, palavraPasse, papeisIds);
        atributos.addFlashAttribute("sucesso", "Utilizador criado com sucesso.");
        return "redirect:/utilizadores";
    }

    @GetMapping("/{id}/editar")
    public String formularioEdicao(@PathVariable Long id, Model modelo) {
        Utilizador utilizador = utilizadorService.obter(id);
        modelo.addAttribute("utilizador", utilizador);
        modelo.addAttribute("papeis", papelService.listarSemVerificacao());
        modelo.addAttribute("papeisSeleccionados",
                utilizador.getPapeis().stream().map(papel -> papel.getId()).toList());
        return "utilizadores/formulario";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("utilizador") Utilizador utilizador,
                             BindingResult validacao,
                             @RequestParam(required = false) String palavraPasse,
                             @RequestParam(required = false) List<Long> papeisIds,
                             Model modelo,
                             RedirectAttributes atributos) {
        if (validacao.hasErrors()) {
            modelo.addAttribute("papeis", papelService.listarSemVerificacao());
            modelo.addAttribute("papeisSeleccionados", papeisIds == null ? List.of() : papeisIds);
            return "utilizadores/formulario";
        }
        utilizadorService.actualizar(id, utilizador, palavraPasse, papeisIds);
        atributos.addFlashAttribute("sucesso", "Utilizador actualizado com sucesso.");
        return "redirect:/utilizadores";
    }

    @PostMapping("/{id}/activacao")
    public String alternarActivacao(@PathVariable Long id, RedirectAttributes atributos) {
        utilizadorService.alternarActivacao(id, sessao.actual().getUsername());
        atributos.addFlashAttribute("sucesso", "Estado do utilizador alterado.");
        return "redirect:/utilizadores";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes atributos) {
        utilizadorService.eliminar(id, sessao.actual().getUsername());
        atributos.addFlashAttribute("sucesso", "Utilizador eliminado.");
        return "redirect:/utilizadores";
    }
}
