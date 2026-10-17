CREATE TABLE recompensa (
                            id UUID PRIMARY KEY,
                            cartao_id UUID NOT NULL UNIQUE REFERENCES cartao_fidelidade(id),
                            estabelecimento_id UUID NOT NULL REFERENCES estabelecimento(id),
                            emitida_em TIMESTAMPTZ NOT NULL,
                            resgatada_em TIMESTAMPTZ,
                            resgatada_por UUID REFERENCES usuario(id),
                            chave_resgate UUID,

                            CONSTRAINT uk_recompensa_resgate
                                UNIQUE (estabelecimento_id, chave_resgate),

                            CONSTRAINT ck_recompensa_resgate
                                CHECK (
                                    (
                                        resgatada_em IS NULL
                                            AND resgatada_por IS NULL
                                            AND chave_resgate IS NULL
                                        )
                                        OR
                                    (
                                        resgatada_em IS NOT NULL
                                            AND resgatada_por IS NOT NULL
                                            AND chave_resgate IS NOT NULL
                                        )
                                    )
);

-- Preserva os benefícios dos ciclos concluídos antes desta atualização.
-- Usa o próprio ID do cartão, sem exigir extensão para gerar UUID.
INSERT INTO recompensa (
    id,
    cartao_id,
    estabelecimento_id,
    emitida_em
)
SELECT
    c.id,
    c.id,
    p.estabelecimento_id,
    c.concluido_em
FROM cartao_fidelidade c
         JOIN participacao p ON p.id = c.participacao_id
WHERE c.concluido_em IS NOT NULL;