package com.vetri.smartcampus.models.common;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DataBaseConnection {

    public static Connection getConnection() {
        String url = getSetting("SMARTCAMPUS_DB_URL", "smartcampus.db.url", "jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require");
        String dbUser = getSetting("SMARTCAMPUS_DB_USER", "smartcampus.db.user", "postgres.wcdahlvkddpfqgkuqzyd");
        String dbPass = getSetting("SMARTCAMPUS_DB_PASSWORD", "smartcampus.db.password", "sudo_me@123");
        String driver = getSetting("SMARTCAMPUS_DB_DRIVER", "smartcampus.db.driver", defaultDriver(url));

        try {
            Class.forName(driver);
            return DriverManager.getConnection(url, dbUser, dbPass);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PreparedStatement getPreparedStatement(Connection con, String sql) {
        try {
            return con.prepareStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean tableExists(Connection con, String tableName) throws Exception {
        DatabaseMetaData metaData = con.getMetaData();
        String schema = null;
        try {
            schema = con.getSchema();
        } catch (Exception ignored) {
        }

        if (metadataHasTable(metaData, schema, tableName) || metadataHasTable(metaData, null, tableName)) {
            return true;
        }
        return metadataHasTable(metaData, schema, tableName.toUpperCase()) || metadataHasTable(metaData, null, tableName.toUpperCase());
    }

    public static int countExistingTables(Connection con, String... tableNames) throws Exception {
        int count = 0;
        for (String tableName : tableNames) {
            if (tableExists(con, tableName)) {
                count++;
            }
        }
        return count;
    }

    private static boolean metadataHasTable(DatabaseMetaData metaData, String schema, String tableName) throws Exception {
        try (ResultSet rs = metaData.getTables(null, schema, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static String getSetting(String envName, String propertyName, String defaultValue) {
        String property = System.getProperty(propertyName);
        if (property != null && !property.isBlank()) {
            return property;
        }
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return defaultValue;
    }

    private static String defaultDriver(String url) {
        if (url != null && url.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        }
        return "org.postgresql.Driver";
    }
}
