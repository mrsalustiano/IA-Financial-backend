# Postman — IA-Financial

Exemplos prontos de uso da API, para importar no Postman.

## Arquivos

| Arquivo | O que é |
|---|---|
| `IA-Financial.postman_collection.json` | Coleção completa: **0-Auth (JWT)**, Health, 1-Contas, 2-Receitas, 3-Categorias, 4-Saldo diário, 5-Fechamento mensal, 6-Análises IA, 7-Planos IA, 8-Assistente IA e **9-Usuários (JWT)** |
| `IA-Financial.postman_environment.json` | Environment **IA-Financial Local** (`baseUrl`, `accessToken`, `refreshToken`, `userId` e ids das entidades) |

## Como importar

1. Postman → **Import** → arraste os dois arquivos (ou *File → Import*).
2. Selecione o environment **IA-Financial Local** no seletor do canto superior direito.
3. Suba a aplicação (`mvn spring-boot:run`, ou com perfil dev:
   `mvn spring-boot:run -Dspring-boot.run.profiles=dev`) com o MongoDB local no ar.
4. Antes de usar: **crie o usuário do banco** conforme `doc/ddl-mongodb.md` (usuário `admin`).

## Autenticação (FASE 10)

1. **0-Auth (JWT) → Cadastrar usuário** — cria o usuário (e-mail, CPF e senha com BCrypt) e salva
   o `userId`.
2. **0-Auth (JWT) → Login** — autentica e salva `accessToken` e `refreshToken` automaticamente.
3. Todas as demais requisições já enviam `Authorization: Bearer {{accessToken}}` — nada a editar.
4. Se o accessToken expirar (15 min), rode **Renovar token**: ele rotaciona os dois tokens
   (o refreshToken antigo é revogado) e salva os novos.
5. **Logout** revoga todos os refresh tokens do usuário e limpa as variáveis.

> Usuário inativo (via **Desativar usuário**) não consegue logar (403) e perde acesso a `/me`.

## Ordem de execução recomendada

As pastas estão numeradas para rodar de cima para baixo:

1. **0-Auth (JWT)** — cadastra o usuário e faz login (obrigatório antes das demais).
2. **Health** — confere que a aplicação respondeu.
3. **1. Contas** — cria a conta e salva o `accountId` automaticamente (script de teste).
4. **2. Receitas** — usa `{{accountId}}`; salva o `incomeId`.
5. **3. Categorias** — CRUD completo (independente).
6. **4. Saldo diário** — usa as receitas já criadas.
7. **5. Fechamento mensal** — snapshot de 2026-10.
8. **6. Análises IA** / **7. Planos IA** / **8. Assistente IA** — exigem **Ollama** rodando em
   `http://localhost:11434` (model `llama3`). Sem receitas → 400; Ollama parado → 503
   (esses comportamentos são esperados).
9. **9-Usuários (JWT)** — listagem, detalhe, edição (e teste de 403 em usuário alheio) e
   desativação. Rode por último: a desativação bloqueia o login deste usuário.

## Variáveis

Os scripts de teste das requisições "Criar ..."/"Login" guardam os valores em variáveis de coleção
(`userId`, `accessToken`, `refreshToken`, `accountId`, `incomeId`, `categoryId`, `analysisId`,
`closingId`, `planId`, `questionId`), então as requisições seguintes (Buscar/Alterar/Excluir)
funcionam em sequência sem edição manual.

## Dica: rodar tudo de uma vez

Postman → pasta raiz → **Run** (Collection Runner) → executa em ordem com 0ms de delay.
Requisições de exemplo de erro (ex.: "Mês inválido", "Editar usuário alheio (403)") falham no
runner de propósito, pois afirmam o status de erro.
