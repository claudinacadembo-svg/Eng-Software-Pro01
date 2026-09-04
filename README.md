# SGE — Sistema de Gestão de Expedientes com RBAC

Trabalho de Campo da disciplina de **Engenharia de Software**
Universidade Aberta ISCED (UnISCED) — Faculdade de Engenharia e Agricultura
Curso de Licenciatura em Engenharia Informática

Repositório público: <https://github.com/claudinacadembo-svg/Eng-Software-Pro01>

---

## 1. O que é

Aplicação web que automatiza o ciclo de vida dos expedientes de uma instituição —
**entrada → tramitação → despacho → arquivo** — garantindo integridade,
rastreabilidade e confidencialidade da informação através de um mecanismo de
**controlo de acesso baseado em papéis (RBAC)**.

Módulos implementados:

| Módulo | Descrição |
|---|---|
| Gestão de utilizadores e autenticação | Contas, activação/desactivação, palavras-passe cifradas com BCrypt, alteração da própria palavra-passe |
| Controlo de acesso (RBAC) | Permissões atómicas → papéis → utilizadores; administração das permissões de cada papel pela própria aplicação |
| Gestão de expedientes | Registo com numeração automática, anexos, encaminhamento entre utilizadores, despachos, arquivo e cancelamento |
| Auditoria e relatórios | Registo imutável de todas as acções (incluindo acessos negados), indicadores agregados e exportação CSV |

---

## 2. Tecnologias e dependências

| Componente | Versão | Porquê |
|---|---|---|
| Java | 17 ou superior (testado com JDK 20) | Linguagem exigida no trabalho |
| Spring Boot | 3.3.5 | Base da aplicação web (MVC, injecção de dependências) |
| Spring Security | 6 (via Spring Boot) | Autenticação e autorização — o núcleo do RBAC |
| Spring Data JPA / Hibernate | 6.5 (via Spring Boot) | Mapeamento objecto-relacional |
| Thymeleaf + extras-springsecurity6 | 3.1 | Interface web renderizada no servidor |
| Base de dados H2 | 2.2 | Base de dados relacional em ficheiro: **não é preciso instalar nada** |
| JUnit 5 + Spring Security Test | — | Testes automáticos do RBAC |

Todas as dependências são resolvidas automaticamente pelo Maven. A folha de
estilo é própria (`src/main/resources/static/css/estilo.css`), pelo que a
aplicação funciona **sem ligação à Internet** depois de compilada.

### Porquê H2 e não MySQL/SQLite

O H2 é uma base de dados relacional completa que corre **dentro** da aplicação e
guarda os dados num ficheiro local (`data/sge.mv.db`). Quem avaliar o trabalho
não precisa de instalar nem configurar servidor de base de dados: basta executar
um comando. Como se usa JPA/Hibernate, migrar para MySQL ou PostgreSQL exige
apenas trocar as quatro linhas de `spring.datasource.*` em
`src/main/resources/application.properties` e a dependência do driver no
`pom.xml` — nenhuma linha de código Java muda.

---

## 3. Como configurar e executar

### 3.1. Pré-requisitos

* **JDK 17 ou superior** instalado e no `PATH` (verificar com `java -version`).
* Ligação à Internet **na primeira compilação**, para o Maven descarregar as
  dependências.
* Não é necessário instalar o Maven: o projecto inclui o *Maven Wrapper*
  (`mvnw` / `mvnw.cmd`).

### 3.2. Obter o código

```bash
git clone https://github.com/claudinacadembo-svg/Eng-Software-Pro01.git
cd Eng-Software-Pro01
```

### 3.3. Executar

Abrir uma consola **dentro da pasta do projecto** e correr:

**Windows — PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run
```

**Windows — Linha de comandos (CMD):**

```bat
mvnw.cmd spring-boot:run
```

> No PowerShell o prefixo `.\` é obrigatório: sem ele a consola responde
> *"mvnw.cmd não é reconhecido"*, porque o PowerShell não procura programas na
> pasta actual.

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

A primeira execução demora mais tempo, porque o Maven descarrega as
dependências. Quando aparecer a linha `Started SgeApplication`, abrir o
navegador em **<http://localhost:8080>**.

Para parar a aplicação: `Ctrl + C` na consola onde ficou a correr.

Em alternativa, gerar e correr o ficheiro executável:

```powershell
.\mvnw.cmd package -DskipTests
java -jar target/sge-1.0.0.jar
```

Para mudar a porta (se a 8080 estiver ocupada):

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=9090"
```

### 3.4. Contas de demonstração

Criadas automaticamente no primeiro arranque (ver `config/CargaInicialDados.java`):

| Utilizador | Palavra-passe | Papel | O que pode fazer |
|---|---|---|---|
| `admin` | `admin123` | ADMINISTRADOR | Tudo, incluindo gerir utilizadores, papéis e permissões |
| `chefe` | `chefe123` | GESTOR_EXPEDIENTE | Todo o ciclo do expediente: registar, tramitar, despachar, arquivar |
| `tecnico` | `tecnico123` | TECNICO | Registar e encaminhar expedientes; **não** pode despachar |
| `auditor` | `auditor123` | AUDITOR | Apenas leitura: expedientes, auditoria e relatórios |

> Em ambiente real estas palavras-passe devem ser alteradas no primeiro acesso
> (ecrã **O meu perfil**).

### 3.5. Base de dados

Os dados ficam em `data/sge.mv.db`, criado na pasta onde a aplicação é
executada. Para recomeçar do zero, basta apagar a pasta `data/` e voltar a
arrancar.

Consola da base de dados (apenas para o papel ADMINISTRADOR):
<http://localhost:8080/h2-console> — JDBC URL `jdbc:h2:file:./data/sge`,
utilizador `sa`, palavra-passe vazia.

### 3.6. Executar os testes

```powershell
.\mvnw.cmd test
```

(Em Linux/macOS: `./mvnw test`.)

São 10 testes automáticos que verificam as regras de RBAC (quem tem e quem não
tem acesso a cada funcionalidade), a regra de confidencialidade e a
apresentação de todos os ecrãs.

---

## 4. Como o RBAC está implementado

O modelo tem três níveis: **Utilizador → Papel → Permissão**. Um utilizador pode
ter vários papéis e cada papel agrega várias permissões atómicas.

```
Utilizador  ──N:N──  Papel  ──N:N──  Permissão
 (admin)              (ADMINISTRADOR)  (EXPEDIENTE_DESPACHAR, ...)
```

Na autenticação, `UtilizadorAutenticado` traduz este modelo em autoridades do
Spring Security: cada papel gera uma autoridade `ROLE_<NOME>` e cada permissão
gera uma autoridade com o seu próprio código. A autorização é depois aplicada em
**três camadas independentes**:

1. **Por URL** — em `config/SecurityConfig.java`:
   ```java
   .requestMatchers("/utilizadores/**").hasAuthority("UTILIZADOR_LER")
   .requestMatchers("/auditoria/**").hasAuthority("AUDITORIA_VER")
   ```
2. **Por método de serviço** — anotações `@PreAuthorize`, que protegem a regra de
   negócio mesmo que alguém alcance o serviço por outro caminho:
   ```java
   @PreAuthorize("hasAuthority('EXPEDIENTE_DESPACHAR')")
   public Expediente despachar(...) { ... }
   ```
3. **Por interface** — o Thymeleaf só desenha os botões que o utilizador pode
   usar: `sec:authorize="hasAuthority('EXPEDIENTE_ARQUIVAR')"`.

Acima do RBAC existe ainda uma **regra de confidencialidade**: um expediente
marcado como CONFIDENCIAL só é visível ao autor, ao responsável actual, ao
administrador e a quem tenha permissão de auditoria — mesmo que o utilizador
tenha `EXPEDIENTE_LER`. A regra é aplicada tanto na consulta em lista (na
própria consulta à base de dados) como no acesso directo pelo endereço.

Toda a tentativa de acesso negada é gravada em auditoria por
`AuditoriaAccessDeniedHandler`, com utilizador, recurso, endereço IP e data.

### Matriz de permissões por papel

| Permissão | ADMINISTRADOR | GESTOR_EXPEDIENTE | TECNICO | AUDITOR |
|---|:---:|:---:|:---:|:---:|
| UTILIZADOR_LER | ✔ | ✔ | | ✔ |
| UTILIZADOR_CRIAR / ACTUALIZAR / ELIMINAR | ✔ | | | |
| PAPEL_LER | ✔ | | | ✔ |
| PAPEL_GERIR | ✔ | | | |
| EXPEDIENTE_LER | ✔ | ✔ | ✔ | ✔ |
| EXPEDIENTE_CRIAR | ✔ | ✔ | ✔ | |
| EXPEDIENTE_ACTUALIZAR | ✔ | ✔ | ✔ | |
| EXPEDIENTE_TRAMITAR | ✔ | ✔ | ✔ | |
| EXPEDIENTE_DESPACHAR | ✔ | ✔ | | |
| EXPEDIENTE_ARQUIVAR | ✔ | ✔ | | |
| EXPEDIENTE_ELIMINAR | ✔ | | | |
| AUDITORIA_VER | ✔ | | | ✔ |
| RELATORIO_VER | ✔ | ✔ | | ✔ |

Esta matriz **não está fixa no código**: o administrador pode alterar as
permissões de cada papel, ou criar papéis novos, em *Papéis e permissões*.

---

## 5. Estrutura do projecto

```
PROJECTO-ESW/
├── pom.xml                       Dependências e configuração de compilação
├── mvnw / mvnw.cmd / .mvn/       Maven Wrapper (dispensa instalar o Maven)
├── src/main/java/mz/unisced/sge/
│   ├── SgeApplication.java       Ponto de entrada
│   ├── config/
│   │   ├── SecurityConfig.java   Autenticação, autorização por URL, BCrypt
│   │   └── CargaInicialDados.java  Permissões, papéis, contas e dados de exemplo
│   ├── model/                    Entidades JPA e enumerações
│   │   ├── Utilizador, Papel, Permissao          (RBAC)
│   │   ├── Expediente, Tramitacao, Despacho, Anexo  (negócio)
│   │   └── LogAuditoria                           (auditoria)
│   ├── repository/               Acesso a dados (Spring Data JPA)
│   ├── security/                 Ligação do RBAC ao Spring Security
│   │   ├── Permissoes.java                  Catálogo de permissões
│   │   ├── UtilizadorAutenticado.java       Papéis/permissões → autoridades
│   │   ├── UtilizadorDetailsService.java
│   │   ├── AuditoriaAutenticacaoListener.java   Regista logins e falhas
│   │   └── AuditoriaAccessDeniedHandler.java    Regista acessos negados
│   ├── service/                  Regras de negócio, com @PreAuthorize
│   ├── dto/                      Objectos de transporte dos relatórios
│   └── web/                      Controladores MVC
├── src/main/resources/
│   ├── application.properties    Configuração (base de dados, anexos, porta)
│   ├── templates/                Ecrãs Thymeleaf
│   └── static/css/estilo.css     Folha de estilo própria (funciona offline)
└── src/test/java/mz/unisced/sge/
    ├── ControloAcessoRbacTest.java    Testes das regras de acesso
    └── RenderizacaoPaginasTest.java   Testes de apresentação dos ecrãs
```

---

## 6. Fluxo de utilização

1. **Autenticação** — o utilizador entra com as suas credenciais; o sistema
   carrega os papéis e permissões e regista o acesso em auditoria.
2. **Registo do expediente** (`EXPEDIENTE_CRIAR`) — recebe automaticamente um
   número no formato `EXP-AAAA-NNNN`, fica no estado *Registado* e a cargo de
   quem o registou. Podem juntar-se anexos digitalizados (até 5 MB).
3. **Tramitação** (`EXPEDIENTE_TRAMITAR`) — o expediente é encaminhado para
   outro utilizador com uma observação; passa a *Em tramitação* e cada movimento
   fica no histórico. Quem não é o responsável actual só pode movimentar o
   expediente se tiver privilégio de despacho.
4. **Despacho** (`EXPEDIENTE_DESPACHAR`) — é proferida uma decisão (deferido,
   indeferido ou encaminhado para parecer) com o respectivo texto; o expediente
   passa a *Despachado*.
5. **Arquivo** (`EXPEDIENTE_ARQUIVAR`) — o expediente é arquivado (ou cancelado
   com motivo) e deixa de admitir movimentação.
6. **Auditoria e relatórios** (`AUDITORIA_VER`, `RELATORIO_VER`) — consulta do
   histórico completo de acções com filtros, indicadores por estado, tipo,
   prioridade e responsável, e exportação em CSV.

---

## 7. Documentação técnica

A documentação técnica completa do trabalho encontra-se na pasta [docs/](docs/):

| Ficheiro | Conteúdo |
|---|---|
| `docs/Relatorio-SGE-UnISCED.docx` | Relatório técnico: introdução e objectivos, metodologia (Scrum), requisitos funcionais e não funcionais, diagramas UML explicados, modelo entidade-relacionamento, arquitectura, detalhe da implementação do RBAC, testes e conclusão |
| `docs/diagramas/*.puml` | Código-fonte dos diagramas (PlantUML) |
| `docs/diagramas/*.png` | Diagramas gerados: casos de uso, classes, actividade, sequência, modelo entidade-relacionamento e arquitectura |

Para voltar a gerar as imagens após alterar um diagrama:

```powershell
java -jar plantuml.jar -tpng docs/diagramas/*.puml
```

---

## 8. Autores

Trabalho realizado em grupo, no âmbito da disciplina de Engenharia de Software.

| Nome | Contribuição |
|---|---|
| Claudina Cadembo | *(preencher)* |
| *(2.º elemento do grupo)* | *(preencher)* |
