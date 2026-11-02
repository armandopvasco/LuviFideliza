package br.com.luvi.fideliza.compartilhado;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Realinha o histórico do Flyway com os scripts atuais e só então migra.
 * Não reexecuta migração já aplicada e não apaga dados. Serve para um banco
 * local cujo checksum das versões 1 a 7 ficou de um texto anterior dos scripts.
 */
@Configuration
@ConditionalOnProperty(name = "luvi.flyway.repair", havingValue = "true")
public class FlywayRepairConfig {
  @Bean
  FlywayMigrationStrategy repairAntesDeMigrar() {
    return flyway -> {
      flyway.repair();
      flyway.migrate();
    };
  }
}
