package me.thiagorigonatti.rinhadebackendjavacore;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {

    private static final String DATABASE_URL = System.getenv("DATABASE_URL");
    private static final String DATABASE_USER = System.getenv("DATABASE_USER");
    private static final String DATABASE_PASSWORD = System.getenv("DATABASE_PASSWORD");
    private static final int HIKARI_MAX_POOL_SIZE = Integer.parseInt(System.getenv("HIKARI_MAX_POOL_SIZE"));
    private static final int HIKARI_MIN_IDLE = Integer.parseInt(System.getenv("HIKARI_MIN_IDLE"));
    private static final int HIKARI_IDLE_TIMEOUT = Integer.parseInt(System.getenv("HIKARI_IDLE_TIMEOUT"));

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DATABASE_URL);
        config.setUsername(DATABASE_USER);
        config.setPassword(DATABASE_PASSWORD);
        config.setMaximumPoolSize(HIKARI_MAX_POOL_SIZE);
        config.setMinimumIdle(HIKARI_MIN_IDLE);
        config.setIdleTimeout(HIKARI_IDLE_TIMEOUT);
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConn()  {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Connection conn;

    static {
        try {
            conn = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
