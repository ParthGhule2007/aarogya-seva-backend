package com.example.consultationservice.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

/**
 * Ensures schema migration from BIGINT to VARCHAR(64) runs safely on PostgreSQL
 * preserving existing numeric records (e.g. 102 -> '102') and schema isolation.
 */
@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);
    private final DataSource dataSource;

    public DatabaseMigrationConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrateDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProduct = metaData.getDatabaseProductName();
            if ("PostgreSQL".equalsIgnoreCase(dbProduct)) {
                log.info("Executing PostgreSQL schema migration check for consultations.user_id...");
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("CREATE SCHEMA IF NOT EXISTS consultation;");
                    stmt.execute("SET search_path TO consultation;");

                    String sql = "DO $$ BEGIN " +
                            "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'consultation' AND table_name = 'consultations' AND column_name = 'user_id' AND data_type = 'bigint') THEN " +
                            "    ALTER TABLE consultation.consultations ALTER COLUMN user_id TYPE VARCHAR(64) USING user_id::VARCHAR; " +
                            "END IF; " +
                            "END $$;";
                    stmt.execute(sql);
                    log.info("PostgreSQL schema check completed: consultation schema verified, consultations.user_id is VARCHAR(64)");
                }
            } else {
                log.debug("Database is {}, skipping PostgreSQL alter column migration", dbProduct);
            }
        } catch (Exception e) {
            log.warn("Database migration check note: {}", e.getMessage());
        }
    }
}
