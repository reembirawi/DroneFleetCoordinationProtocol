package org.example.config;

import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final Properties properties = new Properties();

    static {
        try (InputStream in =
                     AppConfig.class
                             .getClassLoader()
                             .getResourceAsStream("config.properties")) {

            if (in == null) {
                throw new RuntimeException("config.properties not found");
            }

            properties.load(in);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    private AppConfig() {}

    public static String getString(String key) {
        return properties.getProperty(key);
    }
    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
}