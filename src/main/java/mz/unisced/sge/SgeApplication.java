package mz.unisced.sge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sistema de Gestao de Expedientes (SGE) com controlo de acesso baseado em
 * papeis (RBAC).
 *
 * <p>Trabalho de Campo da disciplina de Engenharia de Software - UnISCED.
 */
@SpringBootApplication
public class SgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SgeApplication.class, args);
    }
}
