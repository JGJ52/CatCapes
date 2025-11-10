package hu.jgj52.catcapes.client;

import hu.jgj52.catcapes.client.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class CatCapesClient implements ClientModInitializer {

    public static final Config CONFIG = Config.createAndLoad();

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("reloadcapes")
                .executes(context -> {
                    Utils.reloadCapes();
                    context.getSource().sendFeedback(Text.literal("§aCapes reloaded!"));
                    return 1;
                })
        ));
        CONFIG.subscribeToCape(cape -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player == null) return;

            UUID uuid = client.player.getUuid();

            new Thread(() -> {
                boolean success = true;

                try {
                    URL url = new URL("https://catcapes.jgj52.hu/set");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    String json = String.format("{\"uuid\":\"%s\",\"cape\":\"%s\"}", uuid, cape);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(json.getBytes(StandardCharsets.UTF_8));
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode != 200) success = false;

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                boolean finalSuccess = success;
                client.execute(() -> {
                    if (client.player != null) {
                        if (finalSuccess) {
                            Utils.removePlayerCache(uuid);
                        }
                    }
                });
            }).start();
        });
    }
}
