-- ETAPA 08: APAGA TODOS OS DADOS DE public.convite_equipe. IRREVERSIVEL APOS COMMIT.
-- Execute SOMENTE em banco de testes, com backup e aplicativo parado.
-- Se ocorrer erro, execute ROLLBACK e interrompa a sequencia.
BEGIN;
DELETE FROM public.convite_equipe;
COMMIT;
SELECT COUNT(*) AS registros_restantes FROM public.convite_equipe;
