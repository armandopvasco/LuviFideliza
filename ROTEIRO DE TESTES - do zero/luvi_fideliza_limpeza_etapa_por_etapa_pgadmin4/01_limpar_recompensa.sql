-- ETAPA 01: APAGA TODOS OS DADOS DE public.recompensa. IRREVERSIVEL APOS COMMIT.
-- Execute SOMENTE em banco de testes, com backup e aplicativo parado.
-- Se ocorrer erro, execute ROLLBACK e interrompa a sequencia.
BEGIN;
DELETE FROM public.recompensa;
COMMIT;
SELECT COUNT(*) AS registros_restantes FROM public.recompensa;
