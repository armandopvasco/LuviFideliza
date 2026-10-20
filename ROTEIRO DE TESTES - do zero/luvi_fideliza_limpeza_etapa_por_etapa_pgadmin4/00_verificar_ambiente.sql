-- Execute primeiro. Apenas consultas, sem exclusoes.
SELECT current_database() AS banco, current_schema() AS schema;
SELECT id, nome, email, telefone, administrador_plataforma, ativo FROM public.usuario ORDER BY administrador_plataforma DESC, nome;
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;
-- Consulte as FKs para identificar dependencias adicionais.
SELECT conrelid::regclass AS tabela_filha, confrelid::regclass AS tabela_pai, conname FROM pg_constraint WHERE contype = 'f' AND connamespace = 'public'::regnamespace ORDER BY 1,2;
