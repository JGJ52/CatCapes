package hu.jgj52.catcapes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CatCapesClient implements ClientModInitializer {

    public static final hu.jgj52.catcapes.client.Config CONFIG = hu.jgj52.catcapes.client.Config.createAndLoad();

    @Override
    public void onInitializeClient() {
        ModWebSocket wsc = new ModWebSocket();
        wsc.connect("wss://catcapes.jgj52.hu");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("reloadcapes")
                .executes(context -> {
                    Utils.reloadCapes();
                    context.getSource().sendFeedback(Text.literal("§aCapes reloaded!"));
                    return 1;
                })
        ));
        CONFIG.subscribeToOnline(online -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player == null) return;

            new Thread(() -> {
                boolean success = true;

                if (online) {
                    try {
                        URL url = new URL("https://catcapes.jgj52.hu/v2/set");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);

                        String json = String.format("{\"token\":\"%s\",\"cape\":\"%s\"}", CONFIG.token(), CONFIG.cape());

                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(json.getBytes(StandardCharsets.UTF_8));
                        }

                        int responseCode = conn.getResponseCode();
                        if (responseCode != 200) success = false;

                        conn.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        URL url = new URL("https://catcapes.jgj52.hu/set");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);

                        String json = String.format("{\"uuid\":\"%s\",\"cape\":\"%s\"}", Utils.getOfflineUUID(client.player), CONFIG.cape());

                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(json.getBytes(StandardCharsets.UTF_8));
                        }

                        int responseCode = conn.getResponseCode();
                        if (responseCode != 200) success = false;

                        conn.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                boolean finalSuccess = success;
                client.execute(() -> {
                    if (finalSuccess) {
                        Utils.removePlayerCache(Utils.getUUID(client.player));
                    }
                });
            }).start();
        });
        CONFIG.subscribeToCape(cape -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player == null) return;

            new Thread(() -> {
                boolean success = true;

                if (CONFIG.online()) {
                    try {
                        URL url = new URL("https://catcapes.jgj52.hu/v2/set");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);

                        String json = String.format("{\"token\":\"%s\",\"cape\":\"%s\"}", CONFIG.token(), cape);

                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(json.getBytes(StandardCharsets.UTF_8));
                        }

                        int responseCode = conn.getResponseCode();
                        if (responseCode != 200) success = false;

                        conn.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        URL url = new URL("https://catcapes.jgj52.hu/set");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);

                        String json = String.format("{\"uuid\":\"%s\",\"cape\":\"%s\"}", Utils.getUUID(client.player), cape);

                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(json.getBytes(StandardCharsets.UTF_8));
                        }

                        int responseCode = conn.getResponseCode();
                        if (responseCode != 200) success = false;

                        conn.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                boolean finalSuccess = success;
                client.execute(() -> {
                    if (finalSuccess) {
                        Utils.removePlayerCache(Utils.getUUID(client.player));
                    }
                });
            }).start();
        });
    }
}
