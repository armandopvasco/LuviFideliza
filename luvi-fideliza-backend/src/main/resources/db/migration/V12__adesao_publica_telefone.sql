ALTER TABLE usuario ALTER COLUMN email DROP NOT NULL;
ALTER TABLE usuario ADD COLUMN telefone VARCHAR(13);
CREATE UNIQUE INDEX uk_usuario_telefone ON usuario(telefone) WHERE telefone IS NOT NULL;
CREATE TABLE adesao_estabelecimento (
  estabelecimento_id UUID NOT NULL REFERENCES estabelecimento(id),
  usuario_id UUID NOT NULL REFERENCES usuario(id),
  criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(estabelecimento_id, usuario_id)
);
