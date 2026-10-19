-- Preserva o programa existente como campanha principal e seus cartões históricos.
ALTER TABLE estabelecimento ADD COLUMN exibir_todas_campanhas BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE programa_fidelidade DROP CONSTRAINT IF EXISTS programa_fidelidade_estabelecimento_id_key;
ALTER TABLE programa_fidelidade ADD COLUMN ativa BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE programa_fidelidade ADD COLUMN principal BOOLEAN NOT NULL DEFAULT FALSE;
-- A campanha preexistente continua sendo a principal para a API legada.
UPDATE programa_fidelidade SET principal = TRUE;
CREATE UNIQUE INDEX uk_programa_principal_por_estabelecimento
  ON programa_fidelidade(estabelecimento_id) WHERE principal = TRUE;
CREATE INDEX ix_programa_estabelecimento ON programa_fidelidade(estabelecimento_id);
