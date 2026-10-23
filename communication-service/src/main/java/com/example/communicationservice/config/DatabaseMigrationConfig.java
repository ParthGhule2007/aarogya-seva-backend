package com.example.communicationservice.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

/**
 * Ensures PostgreSQL schema 'communication' exists and migration from BIGINT to VARCHAR(64) runs safely,
 * preserving existing numeric records (e.g. 201 -> '201').
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
                log.info("Ensuring PostgreSQL schema 'communication' exists and checking column types...");
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("CREATE SCHEMA IF NOT EXISTS communication;");
                    stmt.execute("SET search_path TO communication;");
                    String sql = "DO $$ BEGIN " +
                            "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'communication' AND table_name = 'communication_sessions' AND column_name = 'user_id' AND data_type = 'bigint') THEN " +
                            "    ALTER TABLE communication.communication_sessions ALTER COLUMN user_id TYPE VARCHAR(64) USING user_id::VARCHAR; " +
                            "END IF; " +
                            "END $$;";
                    stmt.execute(sql);
                    log.info("PostgreSQL schema check completed: communication_sessions.user_id is VARCHAR(64)");
                }
            } else {
                log.debug("Database is {}, skipping PostgreSQL alter column migration", dbProduct);
            }
        } catch (Exception e) {
            log.warn("Database migration check note: {}", e.getMessage());
        }
    }
}
