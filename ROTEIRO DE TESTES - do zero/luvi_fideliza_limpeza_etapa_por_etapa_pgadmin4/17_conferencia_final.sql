SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename;
SELECT id,nome,email,telefone,administrador_plataforma FROM public.usuario;
SELECT installed_rank,version,description,success FROM public.flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
