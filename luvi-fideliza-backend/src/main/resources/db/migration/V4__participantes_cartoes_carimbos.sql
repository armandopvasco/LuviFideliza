CREATE TABLE participacao (
    id UUID PRIMARY KEY,
    estabelecimento_id UUID NOT NULL REFERENCES estabelecimento(id),
    programa_id UUID NOT NULL REFERENCES programa_fidelidade(id),
    usuario_id UUID REFERENCES usuario(id),
    nome_exibicao VARCHAR(120) NOT NULL CHECK (LENGTH(TRIM(nome_exibicao)) > 0),
    chave_cadastro UUID NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL,
    criado_por UUID NOT NULL REFERENCES usuario(id),
    convite_hash VARCHAR(64) UNIQUE,
    convite_expira_em TIMESTAMPTZ,
    CONSTRAINT uk_participacao_cadastro UNIQUE (estabelecimento_id, chave_cadastro),
    CONSTRAINT uk_participacao_usuario UNIQUE (programa_id, usuario_id)
);

CREATE INDEX ix_participacao_usuario ON participacao(usuario_id);

CREATE TABLE cartao_fidelidade (
    id UUID PRIMARY KEY,
    participacao_id UUID NOT NULL REFERENCES participacao(id),
    versao_programa_id UUID NOT NULL REFERENCES versao_programa(id),
    numero INTEGER NOT NULL CHECK (numero >= 1),
    saldo INTEGER NOT NULL CHECK (saldo >= 0),
    aberto_em TIMESTAMPTZ NOT NULL,
    concluido_em TIMESTAMPTZ,
    CONSTRAINT uk_cartao_ciclo UNIQUE (participacao_id, numero)
);

CREATE TABLE movimentacao_carimbo (
    id UUID PRIMARY KEY,
    estabelecimento_id UUID NOT NULL REFERENCES estabelecimento(id),
    participacao_id UUID NOT NULL REFERENCES participacao(id),
    cartao_id UUID NOT NULL REFERENCES cartao_fidelidade(id),
    chave_operacao UUID NOT NULL,
    referencia_compra VARCHAR(120) NOT NULL CHECK (LENGTH(TRIM(referencia_compra)) > 0),
    saldo_apos INTEGER NOT NULL CHECK (saldo_apos >= 1),
    registrado_em TIMESTAMPTZ NOT NULL,
    registrado_por UUID NOT NULL REFERENCES usuario(id),
    CONSTRAINT uk_carimbo_operacao UNIQUE (estabelecimento_id, chave_operacao)
);

CREATE INDEX ix_carimbo_participacao ON movimentacao_carimbo(participacao_id);
