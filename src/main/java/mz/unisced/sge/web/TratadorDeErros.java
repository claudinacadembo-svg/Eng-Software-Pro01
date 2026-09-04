package mz.unisced.sge.web;

import jakarta.servlet.http.HttpServletRequest;
import mz.unisced.sge.service.RegraNegocioException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Converte as excepcoes previstas em mensagens no ecra, evitando paginas de
 * erro tecnicas para o utilizador final. As violacoes de RBAC nao sao tratadas
 * aqui de proposito: devem chegar ao AuditoriaAccessDeniedHandler, que as
 * regista em auditoria antes de encaminhar para a pagina de acesso negado.
 */
@ControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RegraNegocioException.class)
    public String aoFalharRegraDeNegocio(RegraNegocioException excepcao,
                                         HttpServletRequest pedido,
                                         RedirectAttributes atributos) {
        atributos.addFlashAttribute("erro", excepcao.getMessage());
        return "redirect:" + origem(pedido);
    }

    private String origem(HttpServletRequest pedido) {
        String anterior = pedido.getHeader("Referer");
        if (anterior == null || anterior.isBlank()) {
            return "/";
        }
        // Usa apenas o caminho, para nao reencaminhar para dominios externos.
        int inicioDoCaminho = anterior.indexOf('/', anterior.indexOf("//") + 2);
        return inicioDoCaminho < 0 ? "/" : anterior.substring(inicioDoCaminho);
    }
}
