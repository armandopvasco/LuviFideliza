ALTER TABLE convite_equipe ALTER COLUMN email DROP NOT NULL;
ALTER TABLE convite_equipe ADD COLUMN telefone VARCHAR(13);
CREATE UNIQUE INDEX uk_convite_equipe_telefone ON convite_equipe(estabelecimento_id, telefone) WHERE telefone IS NOT NULL;
ALTER TABLE convite_proprietario ALTER COLUMN email DROP NOT NULL;
ALTER TABLE convite_proprietario ADD COLUMN telefone VARCHAR(13);
