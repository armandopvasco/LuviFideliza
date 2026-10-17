# Validação 0.4

Build Maven package concluído com Java 17 e Spring Boot 3.5.6.
41 testes automatizados passaram: os 25 anteriores, 12 de cartões e 4 de concorrência.
Cobertura: duplicação de cadastro, crédito idempotente e conflitos de dados,
compras concorrentes, regra preservada em cartão aberto, nova versão em ciclo
seguinte, posse do convite, expiração, renovação, duas contas disputando o convite,
carteira da conta e isolamento entre estabelecimentos.

Coleção Postman 0.4 executada por Newman contra a aplicação rodando em H2:
30 requisições principais e 21 consultas automáticas de CSRF, sem falhas.
Além dos códigos HTTP, foram conferidos IDs iguais no retry, saldo 1 após
repetição, saldo 2 após outra compra, duas movimentações e carteira vinculada.
V1, V2 e V3 comparadas byte a byte com a entrega anterior, sem alterações.

Os testes usaram H2, sem executar as migrações PostgreSQL. V4 e os bloqueios
precisam ser conferidos no PostgreSQL local antes de qualquer publicação.
Não há interface web/Flutter, emissão/resgate de recompensa ou estorno nesta entrega.
