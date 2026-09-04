package mz.unisced.sge.web;

import mz.unisced.sge.service.SessaoUtilizador;
import mz.unisced.sge.service.UtilizadorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Ecras de autenticacao, perfil e acesso negado. */
@Controller
public class AutenticacaoController {

    private final UtilizadorService utilizadorService;
    private final SessaoUtilizador sessao;

    public AutenticacaoController(UtilizadorService utilizadorService, SessaoUtilizador sessao) {
        this.utilizadorService = utilizadorService;
        this.sessao = sessao;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }

    @GetMapping("/perfil")
    public String perfil(Model modelo) {
        modelo.addAttribute("utilizador", sessao.actual());
        return "perfil";
    }

    @PostMapping("/perfil/palavra-passe")
    public String alterarPalavraPasse(@RequestParam String actual,
                                      @RequestParam String nova,
                                      @RequestParam String confirmacao,
                                      RedirectAttributes atributos) {
        if (!nova.equals(confirmacao)) {
            atributos.addFlashAttribute("erro", "A confirmacao nao coincide com a nova palavra-passe.");
            return "redirect:/perfil";
        }
        utilizadorService.alterarPalavraPassePropria(sessao.actual().getUsername(), actual, nova);
        atributos.addFlashAttribute("sucesso", "Palavra-passe alterada com sucesso.");
        return "redirect:/perfil";
    }
}
