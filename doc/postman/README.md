# Postman — IA-Financial

Exemplos prontos de uso da API, para importar no Postman.

## Arquivos

| Arquivo | O que é |
|---|---|
| `IA-Financial.postman_collection.json` | Coleção completa: Health + Contas, Receitas, Categorias, Saldo diário, Fechamento mensal, Análises IA, Planos IA, Assistente IA |
| `IA-Financial.postman_environment.json` | Environment **IA-Financial Local** com `baseUrl = http://localhost:8080` |

## Como importar

1. Postman → **Import** → arraste os dois arquivos (ou *File → Import*).
2. Selecione o environment **IA-Financial Local** no seletor do canto superior direito.
3. Suba a aplicação (`mvn spring-boot:run`) com o MongoDB local no ar.

## Ordem de execução recomendada

As pastas estão numeradas para rodar de cima para baixo:

1. **Health** — confere que a aplicação respondeu.
2. **1. Contas** — cria a conta e salva o `accountId` automaticamente (script de teste).
3. **2. Receitas** — usa `{{accountId}}`; salva o `incomeId`.
4. **3. Categorias** — CRUD completo (independente).
5. **4. Saldo diário** — usa as receitas já criadas.
6. **5. Fechamento mensal** — snapshot de 2026-10.
7. **6. Análises IA** / **7. Planos IA** / **8. Assistente IA** — exigem **Ollama** rodando em
   `http://localhost:11434` (model `llama3` em `application.yml`). Sem receitas → 400; Ollama
   parado → 503 (esses comportamentos são esperados).

## Variáveis

Os scripts de teste das requisições "Criar ..." guardam os ids em variáveis de coleção
(`accountId`, `incomeId`, `categoryId`, `analysisId`, `closingId`, `planId`, `questionId`), então as
requisições seguintes (Buscar/Alterar/Excluir) funcionam em sequência sem edição manual.

## Dica: rodar tudo de uma vez

Postman → pasta raiz → **Run** (Collection Runner) → executa em ordem com 0ms de delay.
Requisições de exemplo de erro (ex.: "Mês inválido") falham no runner de propósito, pois afirmam o
status de erro.
