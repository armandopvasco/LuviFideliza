CREATE TABLE token_acesso (
  id UUID PRIMARY KEY,
  usuario_id UUID NOT NULL REFERENCES usuario(id),
  token_hash VARCHAR(64) NOT NULL,
  expira_em TIMESTAMPTZ NOT NULL,
  criado_em TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_token_acesso_hash UNIQUE (token_hash)
);
CREATE INDEX ix_token_acesso_usuario ON token_acesso(usuario_id);
