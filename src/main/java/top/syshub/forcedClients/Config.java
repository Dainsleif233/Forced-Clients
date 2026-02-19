package top.syshub.forcedClients;

import com.moandjiezana.toml.Toml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static top.syshub.forcedClients.ForcedClients.logger;

public class Config {
    public boolean debug = false;
    public Map<String, String> forcedClients = new HashMap<>();

    public Config(File file) {
        if (!file.getParentFile().exists()) {
            try {
                Files.createDirectories(file.getParentFile().toPath());
            } catch (IOException ignored) {
                logger.error("Failed to create directory for config file.");
            }
        }
        if (!file.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/config.toml")) {
                assert in != null;
                Files.copy(in, file.toPath());
            } catch (IOException ignored) {
                logger.error("Failed to copy config file.");
            }
        }

        try {
            Toml toml = new Toml().read(file);
            debug = toml.getBoolean("debug");
            toml.getTable("forced-clients").toMap().forEach((k, v) -> {
                if (k.startsWith("\"") && k.endsWith("\""))
                    k = k.substring(1, k.length() - 1);
                forcedClients.put(k, v != null ? v.toString() : null);
            });
        } catch (RuntimeException ignored) {
            logger.error("Failed to read config file.");
        }
    }
}
