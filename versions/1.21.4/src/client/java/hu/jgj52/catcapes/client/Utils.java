package hu.jgj52.catcapes.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Utils {

    private static final Map<String, Identifier> CAPE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, String> PLAYER_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> NO_CAPE = ConcurrentHashMap.newKeySet();

    public static Identifier downloadCape(UUID uuid) {
        if (NO_CAPE.contains(uuid)) {
            return null;
        }

        if (LOADING.contains(uuid)) {
            return null;
        }

        String name = PLAYER_CACHE.get(uuid);

        if (name != null) {
            return CAPE_CACHE.get(name);
        }

        LOADING.add(uuid);

        new Thread(() -> {
            try {
                String capeName;
                if (uuid.equals(MinecraftClient.getInstance().player.getGameProfile().getId()) && !CatCapesClient.CONFIG.cape().isEmpty()) {
                    capeName = CatCapesClient.CONFIG.cape();
                    PLAYER_CACHE.put(uuid, capeName);
                } else {
                    HttpURLConnection conn = (HttpURLConnection) new URL("https://catcapes.jgj52.hu/player/" + uuid).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode != 200) {
                        NO_CAPE.add(uuid);
                        LOADING.remove(uuid);
                        return;
                    }

                    try (InputStream in = conn.getInputStream()) {
                        capeName = new String(in.readAllBytes()).trim();
                    }

                    PLAYER_CACHE.put(uuid, capeName);
                }

                if (CAPE_CACHE.containsKey(capeName)) {
                    LOADING.remove(uuid);
                    return;
                }

                Path capePath = FabricLoader.getInstance().getGameDir().resolve("capes/" + capeName);
                Files.createDirectories(capePath.getParent());

                if (!Files.exists(capePath)) {
                    try (InputStream in = new URL("https://catcapes.jgj52.hu/cape/" + capeName).openStream()) {
                        Files.copy(in, capePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                NativeImage img = NativeImage.read(Files.newInputStream(capePath));
                Identifier textureId = Identifier.of("catcapes", "capes/" + capeName);

                MinecraftClient.getInstance().execute(() -> {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(img);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
                    texture.upload();
                    CAPE_CACHE.put(capeName, textureId);
                });

            } catch (Exception e) {
                NO_CAPE.add(uuid);
                System.err.println("Failed to load cape for " + uuid + ": " + e.getMessage());
            } finally {
                LOADING.remove(uuid);
            }
        }, "Cape-Loader-" + uuid).start();

        return null;
    }

    public static void reloadCapes() {
        PLAYER_CACHE.clear();
        NO_CAPE.clear();
    }

    public static void removePlayerCache(UUID uuid) {
        PLAYER_CACHE.remove(uuid);
    }
}