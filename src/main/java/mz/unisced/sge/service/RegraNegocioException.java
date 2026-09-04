package mz.unisced.sge.service;

/** Erro previsto de negocio, apresentado ao utilizador como mensagem no ecra. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
