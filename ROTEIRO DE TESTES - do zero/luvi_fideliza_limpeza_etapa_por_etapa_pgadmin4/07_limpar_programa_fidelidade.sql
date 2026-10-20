-- ETAPA 07: APAGA TODOS OS DADOS DE public.programa_fidelidade. IRREVERSIVEL APOS COMMIT.
-- Execute SOMENTE em banco de testes, com backup e aplicativo parado.
-- Se ocorrer erro, execute ROLLBACK e interrompa a sequencia.
BEGIN;
DELETE FROM public.programa_fidelidade;
COMMIT;
SELECT COUNT(*) AS registros_restantes FROM public.programa_fidelidade;
