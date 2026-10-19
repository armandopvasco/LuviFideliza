ALTER TABLE estabelecimento ADD COLUMN icone_carimbo VARCHAR(32) NOT NULL DEFAULT 'estrela';
ALTER TABLE estabelecimento ADD COLUMN foto_fundo_cartao BOOLEAN NOT NULL DEFAULT FALSE;
