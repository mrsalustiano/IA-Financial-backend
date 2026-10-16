# Documentação — IA-Financial

| Arquivo | Conteúdo |
|---|---|
| [arquitetura.md](arquitetura.md) | Desenho de arquitetura: visão hexagonal (diagrama), relacionamentos das coleções (ER), sequência dos fluxos (autenticação JWT, cadastro de receita, análise com IA, fechamento agendado), decisões de desenho, erros |
| [ddl-mongodb.md](ddl-mongodb.md) | **DDL**: criação das 10 coleções (incluindo `users` e `refresh_tokens`) com validadores `$jsonSchema` e índices (equivalente a CREATE TABLE + CHECK + INDEX), DCL (usuário do banco) e verificação |
| [dml-mongodb.md](dml-mongodb.md) | **DML**: inserts de exemplo (usuário sem expor senha, refresh token com hash), updates com auditoria, exclusão lógica, consultas equivalentes aos endpoints e aggregations (saldo diário, total por mês) |
| [postman/](postman/README.md) | Collection e environment prontos para importar no Postman — autenticação JWT (login/refresh rotativo/logout/me), gestão de usuários e todos os fluxos das FASES 1–9, com scripts que salvam ids e tokens automaticamente |

Visão geral do produto e critérios de aceite (Gherkin) por fase: [../README.md](../README.md).
