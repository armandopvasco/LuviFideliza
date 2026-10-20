-- Consultas somente leitura, compatíveis com a estrutura v0.9.1.
-- Execute separadamente no pgAdmin 4.

-- 1) Pessoas e papéis globais
SELECT id, nome, email, telefone, administrador_plataforma, ativo, criado_em
FROM usuario ORDER BY nome;

-- 2) Proprietários e funcionários por loja
SELECT e.nome AS estabelecimento, u.nome AS pessoa, m.papel,
       u.email, u.telefone, m.ativo
FROM membro_estabelecimento m
JOIN usuario u ON u.id = m.usuario_id
JOIN estabelecimento e ON e.id = m.estabelecimento_id
ORDER BY e.nome, m.papel, u.nome;

-- 3) Clientes associados a estabelecimentos
SELECT e.nome AS estabelecimento, u.nome AS cliente,
       u.email, u.telefone, a.criado_em AS adesao_em
FROM adesao_estabelecimento a
JOIN usuario u ON u.id = a.usuario_id
JOIN estabelecimento e ON e.id = a.estabelecimento_id
ORDER BY e.nome, u.nome;

-- 4) Quantitativo de clientes por estabelecimento
SELECT e.nome, COUNT(a.usuario_id) AS total_clientes
FROM estabelecimento e
LEFT JOIN adesao_estabelecimento a ON a.estabelecimento_id = e.id
GROUP BY e.id, e.nome ORDER BY e.nome;
