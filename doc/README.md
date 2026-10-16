# Documentação — IA-Financial

| Arquivo | Conteúdo |
|---|---|
| [arquitetura.md](arquitetura.md) | Desenho de arquitetura: visão hexagonal (diagrama), relacionamentos das coleções (ER), sequência dos fluxos (cadastro de receita, análise com IA, fechamento agendado), decisões de desenho, erros |
| [ddl-mongodb.md](ddl-mongodb.md) | **DDL**: criação das coleções com validadores `$jsonSchema` e índices (equivalente a CREATE TABLE + CHECK + INDEX), DCL (usuário do banco) e verificação |
| [dml-mongodb.md](dml-mongodb.md) | **DML**: inserts de exemplo, updates com auditoria, exclusão lógica, consultas equivalentes aos endpoints e aggregations (saldo diário, total por mês) |
| [postman/](postman/README.md) | Collection e environment prontos para importar no Postman, com scripts que salvam os ids automaticamente e ordem de execução recomendada |

Visão geral do produto e critérios de aceite (Gherkin) por fase: [../README.md](../README.md).
