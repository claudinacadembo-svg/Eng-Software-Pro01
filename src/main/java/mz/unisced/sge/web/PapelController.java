package mz.unisced.sge.web;

import java.util.List;
import mz.unisced.sge.model.Papel;
import mz.unisced.sge.service.PapelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Administracao do modelo RBAC: papeis e permissoes que cada papel concede. */
@Controller
@RequestMapping("/papeis")
public class PapelController {

    private final PapelService papelService;

    public PapelController(PapelService papelService) {
        this.papelService = papelService;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("papeis", papelService.listar());
        modelo.addAttribute("permissoesPorModulo", papelService.permissoesPorModulo());
        return "papeis/lista";
    }

    @GetMapping("/{id}/editar")
    public String formularioEdicao(@PathVariable Long id, Model modelo) {
        Papel papel = papelService.obter(id);
        modelo.addAttribute("papel", papel);
        modelo.addAttribute("permissoesPorModulo", papelService.permissoesPorModulo());
        modelo.addAttribute("permissoesSeleccionadas",
                papel.getPermissoes().stream().map(permissao -> permissao.getId()).toList());
        return "papeis/formulario";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @RequestParam(required = false) String descricao,
                             @RequestParam(required = false) List<Long> permissoesIds,
                             RedirectAttributes atributos) {
        papelService.actualizarPermissoes(id, descricao, permissoesIds);
        atributos.addFlashAttribute("sucesso", "Permissoes do papel actualizadas.");
        return "redirect:/papeis";
    }

    @PostMapping
    public String criar(@RequestParam String nome,
                        @RequestParam(required = false) String descricao,
                        @RequestParam(required = false) List<Long> permissoesIds,
                        RedirectAttributes atributos) {
        papelService.criar(nome, descricao, permissoesIds);
        atributos.addFlashAttribute("sucesso", "Papel criado com sucesso.");
        return "redirect:/papeis";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes atributos) {
        papelService.eliminar(id);
        atributos.addFlashAttribute("sucesso", "Papel eliminado.");
        return "redirect:/papeis";
    }
}
