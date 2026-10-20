-- ETAPA 04: APAGA TODOS OS DADOS DE public.adesao_estabelecimento. IRREVERSIVEL APOS COMMIT.
-- Execute SOMENTE em banco de testes, com backup e aplicativo parado.
-- Se ocorrer erro, execute ROLLBACK e interrompa a sequencia.
BEGIN;
DELETE FROM public.adesao_estabelecimento;
COMMIT;
SELECT COUNT(*) AS registros_restantes FROM public.adesao_estabelecimento;
