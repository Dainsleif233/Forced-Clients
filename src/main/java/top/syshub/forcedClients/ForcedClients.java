package top.syshub.forcedClients;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
        id = "forced_clients",
        name = "Forced Clients",
        version = BuildConstants.VERSION,
        description = "Create a forced client for the specified client brand, like forced hosts.",
        authors = {"Dainsleif"}
)
public class ForcedClients {

    private final ProxyServer server;
    public static Logger logger;
    private final Path dataDirectory;
    private Config config;
    private final ConcurrentHashMap<UUID, PendingEvent> waitClient = new ConcurrentHashMap<>();

    private record PendingEvent(PlayerChooseInitialServerEvent event, Continuation continuation) {}

    @Inject
    public ForcedClients(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        ForcedClients.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        config = new Config(dataDirectory.resolve("config.toml").toFile());
        logger.info("Forced Clients loaded!");
    }

    @Subscribe
    public void onProxyReload(ProxyReloadEvent event) {
        config = new Config(dataDirectory.resolve("config.toml").toFile());
        logger.info("Forced Clients reloaded!");
    }

    @Subscribe
    public EventTask onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        return EventTask.withContinuation(continuation -> waitClient.put(
                event.getPlayer().getUniqueId(),
                new PendingEvent(event, continuation)
        ));
    }

    @Subscribe
    public void onPlayerClientBrand(PlayerClientBrandEvent event) {
        Player player = event.getPlayer();
        if (!waitClient.containsKey(player.getUniqueId())) return;
        PendingEvent pending = waitClient.remove(player.getUniqueId());
        String client = event.getBrand();
        if (config.debug) logger.info("Player {} has joined with client {}", player.getUsername(), client);
        if (client == null || !config.forcedClients.containsKey(client)) {
            pending.continuation.resume();
            return;
        }
        String target = config.forcedClients.get(client);
        RegisteredServer targetServer = server.getAllServers().stream()
                .filter(s -> s.getServerInfo().getName().equals(target))
                .findFirst()
                .orElse(null);
        if (targetServer == null) {
            pending.continuation.resume();
            return;
        }

        pending.event.setInitialServer(targetServer);
        pending.continuation.resume();
    }
}
