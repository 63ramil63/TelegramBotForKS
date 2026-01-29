package org.example.bot.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public class BotConfig {
    private static String botToken;
    private static String botName;
    private static int cacheDuration;
    private static String fileStoragePath;
    private static String fileDelimiter;
    private static List<String> allowedExtensions;
    private static int maxFileSize;
    private static List<String> initialAdmins;
    private static int threadPoolSize;
    private static int maxThreadPoolSize;

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private BotConfig() {}

    public static BotConfig load(String configPath) {
        BotConfig config = new BotConfig();
        Properties properties = new Properties();

        try (InputStream is = new FileInputStream(configPath)) {
            properties.load(is);

            botToken = properties.getProperty("bot_token");
            botName = properties.getProperty("bot_name");
            cacheDuration = Integer.parseInt(properties.getProperty("duration", "30"));
            fileStoragePath = properties.getProperty("path");
            fileDelimiter = FileSystems.getDefault().getSeparator();
            allowedExtensions = List.of(properties.getProperty("extensions", "").split(","));
            maxFileSize = Integer.parseInt(properties.getProperty("fileMaxSize", "50"));
            initialAdmins = List.of(properties.getProperty("admins", "").split(","));
            threadPoolSize = Integer.parseInt(properties.getProperty("thread_pool_size", "4"));
            maxThreadPoolSize = Integer.parseInt(properties.getProperty("max_thread_pool_size", "10"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load bot configuration", e);
        }

        return config;
    }

    public static String getBotToken() { return botToken; }
    public static String getBotName() { return botName; }
    public static int getCacheDuration() { return cacheDuration; }
    public static String getFileStoragePath() { return fileStoragePath; }
    public static String getFileDelimiter() { return fileDelimiter; }
    public static List<String> getAllowedExtensions() { return allowedExtensions; }
    public static int getMaxFileSize() { return maxFileSize; }
    public static List<String> getInitialAdmins() { return initialAdmins; }
    public static int getThreadPoolSize() { return threadPoolSize; }
    public static int getMaxThreadPoolSize() { return maxThreadPoolSize; }
}
