-- ETAPA 02: APAGA TODOS OS DADOS DE public.movimentacao_carimbo. IRREVERSIVEL APOS COMMIT.
-- Execute SOMENTE em banco de testes, com backup e aplicativo parado.
-- Se ocorrer erro, execute ROLLBACK e interrompa a sequencia.
BEGIN;
DELETE FROM public.movimentacao_carimbo;
COMMIT;
SELECT COUNT(*) AS registros_restantes FROM public.movimentacao_carimbo;
