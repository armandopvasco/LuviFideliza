CREATE TABLE programa_fidelidade (
    id UUID PRIMARY KEY,
    estabelecimento_id UUID NOT NULL UNIQUE REFERENCES estabelecimento(id),
    versao_atual INTEGER NOT NULL CHECK (versao_atual >= 1),
    criado_em TIMESTAMPTZ NOT NULL
);

-- As versões são registros históricos; a aplicação nunca as sobrescreve.
CREATE TABLE versao_programa (
    id UUID PRIMARY KEY,
    programa_id UUID NOT NULL REFERENCES programa_fidelidade(id),
    numero INTEGER NOT NULL CHECK (numero >= 1),
    titulo VARCHAR(120) NOT NULL CHECK (LENGTH(TRIM(titulo)) > 0),
    meta_carimbos INTEGER NOT NULL CHECK (meta_carimbos BETWEEN 1 AND 1000),
    recompensa VARCHAR(200) NOT NULL CHECK (LENGTH(TRIM(recompensa)) > 0),
    criterios_compra VARCHAR(500) NOT NULL CHECK (LENGTH(TRIM(criterios_compra)) > 0),
    criado_em TIMESTAMPTZ NOT NULL,
    criado_por UUID NOT NULL REFERENCES usuario(id),
    CONSTRAINT uk_versao_programa_numero UNIQUE (programa_id, numero)
);
