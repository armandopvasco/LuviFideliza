CREATE TABLE convite_proprietario (
 id UUID PRIMARY KEY,
 estabelecimento_id UUID NOT NULL REFERENCES estabelecimento(id),
 email VARCHAR(254) NOT NULL,
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 expira_em TIMESTAMPTZ NOT NULL,
 usado_em TIMESTAMPTZ,
 criado_por UUID NOT NULL REFERENCES usuario(id)
);
CREATE INDEX ix_convite_estabelecimento ON convite_proprietario(estabelecimento_id);
