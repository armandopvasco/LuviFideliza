-- Atualiza o histórico das versões 1 a 7 para o texto atual dos scripts.
-- Não altera tabelas de negócio. Rode uma vez no banco que já recusou a subida.
UPDATE flyway_schema_history SET checksum = 439432507 WHERE version = '1' AND checksum = 1974004141;
UPDATE flyway_schema_history SET checksum = -1714517799 WHERE version = '2' AND checksum = 1906443237;
UPDATE flyway_schema_history SET checksum = -69716806 WHERE version = '3' AND checksum = -1022077611;
UPDATE flyway_schema_history SET checksum = -1253032434 WHERE version = '4' AND checksum = -2040411717;
UPDATE flyway_schema_history SET checksum = -49652090 WHERE version = '5' AND checksum = -478837191;
UPDATE flyway_schema_history SET checksum = 1101457399 WHERE version = '6' AND checksum = 1551111645;
UPDATE flyway_schema_history SET checksum = 1392250768 WHERE version = '7' AND checksum = 1870458142;
