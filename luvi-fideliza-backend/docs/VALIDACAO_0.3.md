# Validação da entrega 0.3

Build Maven package concluído com Java 17 e Spring Boot 3.5.6.
25 testes automatizados passaram: 6 de acessos, 17 de programa e 2 de concorrência.
A coleção Postman 0.3 foi executada por Newman contra a aplicação rodando:
15 requisições principais e 10 buscas automáticas de CSRF, sem falhas.
Cobertura funcional: login/logout, convite, propriedade, consulta, criação,
edição, histórico, metas inválidas, CSRF, conflito por versão e concorrência.
V1 e V2 comparadas byte a byte com a base 0.2, sem alterações.

Essas verificações usaram H2. A migração V3 e os bloqueios precisam ser
validados em PostgreSQL no ambiente local antes de qualquer publicação.
Esta entrega não inclui cartões, crédito de carimbos nem interface web.
