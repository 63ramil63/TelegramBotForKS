package org.example.bot.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.Properties;

public class BotConfig {
    private String botToken;
    private String botName;
    private int cacheDuration;
    private String fileStoragePath;
    private String fileDelimiter;
    private List<String> allowedExtensions;
    private int maxFileSize;
    private List<String> initialAdmins;
    private int threadPoolSize;
    private int maxThreadPoolSize;

    private BotConfig() {}

    public static BotConfig load(String configPath) {
        BotConfig config = new BotConfig();
        Properties properties = new Properties();

        try (InputStream is = new FileInputStream(configPath)) {
            properties.load(is);

            config.botToken = properties.getProperty("bot_token");
            config.botName = properties.getProperty("bot_name");
            config.cacheDuration = Integer.parseInt(properties.getProperty("duration", "30"));
            config.fileStoragePath = properties.getProperty("path");
            config.fileDelimiter = FileSystems.getDefault().getSeparator();
            config.allowedExtensions = List.of(properties.getProperty("extensions", "").split(","));
            config.maxFileSize = Integer.parseInt(properties.getProperty("fileMaxSize", "50"));
            config.initialAdmins = List.of(properties.getProperty("admins", "").split(","));
            config.threadPoolSize = Integer.parseInt(properties.getProperty("thread_pool_size", "4"));
            config.maxThreadPoolSize = Integer.parseInt(properties.getProperty("max_thread_pool_size", "10"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load bot configuration", e);
        }

        return config;
    }

    public String getBotToken() { return botToken; }
    public String getBotName() { return botName; }
    public int getCacheDuration() { return cacheDuration; }
    public String getFileStoragePath() { return fileStoragePath; }
    public String getFileDelimiter() { return fileDelimiter; }
    public List<String> getAllowedExtensions() { return allowedExtensions; }
    public int getMaxFileSize() { return maxFileSize; }
    public List<String> getInitialAdmins() { return initialAdmins; }
    public int getThreadPoolSize() { return threadPoolSize; }
    public int getMaxThreadPoolSize() { return maxThreadPoolSize; }
}
