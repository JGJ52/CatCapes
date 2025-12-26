package hu.jgj52.catcapes.client;

import com.madgag.gif.fmsware.GifDecoder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Utils {
    private static final Map<String, Boolean> IS_ANIMATED = new ConcurrentHashMap<>();
    private static final Map<String, Identifier> CAPE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<Identifier>> ANIMATED_CAPES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> ANIMATED_TICKS = new ConcurrentHashMap<>();
    private static final Map<String, Long> ANIMATED_TICK_COUNTER = new ConcurrentHashMap<>();
    private static final Map<UUID, String> PLAYER_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> NO_CAPE = ConcurrentHashMap.newKeySet();

    public static Identifier getCape(UUID uuid) {
        if (NO_CAPE.contains(uuid)) {
            return null;
        }

        if (LOADING.contains(uuid)) {
            return null;
        }

        String name = PLAYER_CACHE.get(uuid);

        if (name != null) {
            if (Boolean.TRUE.equals(IS_ANIMATED.get(name))) {
                Long nowTick = ANIMATED_TICK_COUNTER.getOrDefault(name, 0L);
                List<Identifier> frames = ANIMATED_CAPES.get(name);
                if (frames == null || frames.isEmpty()) return null;

                int frameIndex = ANIMATED_TICKS.getOrDefault(name, 0);

                if (System.currentTimeMillis() - nowTick >= 1000 * CatCapesClient.CONFIG.update()) {
                    frameIndex++;
                    if (frameIndex >= frames.size()) frameIndex = 0;
                    nowTick = System.currentTimeMillis();
                }

                ANIMATED_TICKS.put(name, frameIndex);
                ANIMATED_TICK_COUNTER.put(name, nowTick);

                return frames.get(frameIndex);
            } else {
                return CAPE_CACHE.get(name);
            }
        }


        LOADING.add(uuid);

        new Thread(() -> {
            try {
                String capeName;
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

                if (CAPE_CACHE.containsKey(capeName)) {
                    LOADING.remove(uuid);
                    return;
                }

                Path capePath = FabricLoader.getInstance().getGameDir().resolve("catcapes/" + capeName);
                Files.createDirectories(capePath.getParent());

                if (!Files.exists(capePath)) {
                    URL url = new URL("https://catcapes.jgj52.hu/cape/" + capeName);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    int resCode = connection.getResponseCode();
                    if (resCode == 200) {
                        String contentType = connection.getHeaderField("Content-Type");
                        List<String> now = CatCapesClient.CONFIG.animatedCapes();
                        if (contentType.equals("image/gif")) now.add(capeName);
                        CatCapesClient.CONFIG.animatedCapes(now);

                        try (InputStream in = connection.getInputStream()) {
                            Files.copy(in, capePath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } else {
                        System.err.println("Failed to download cape, HTTP " + resCode);
                    }

                    connection.disconnect();
                }

                if (CatCapesClient.CONFIG.animatedCapes().contains(capeName)) {
                    GifDecoder decoder = new GifDecoder();
                    decoder.read(Files.newInputStream(capePath));

                    int frameCount = decoder.getFrameCount();
                    List<Identifier> frames = new ArrayList<>();
                    for (int i = 0; i < frameCount; i++) {
                        BufferedImage frame = decoder.getFrame(i);
                        NativeImage image = new NativeImage(frame.getWidth(), frame.getHeight(), true);
                        for (int x = 0; x < frame.getWidth(); x++) {
                            for (int y = 0; y < frame.getHeight(); y++) {
                                //? if <=1.21.4 {
                                    /*image.setColorArgb(x, y, frame.getRGB(x, y));
                                *///? } else {
                                    image.setColor(x, y, frame.getRGB(x, y));
                                //? }
                            }
                        }
                        Identifier id = Identifier.of("catcapes", "capes/" + capeName + "/" + i);
                        int finalI = i;
                        MinecraftClient.getInstance().execute(() -> {
                            NativeImageBackedTexture texture = //? if <=1.21.4 {
                                    /*new NativeImageBackedTexture(image);
                                     *///?} else {
                                    new NativeImageBackedTexture(() -> "catcapes:capes/" + capeName + "/" + finalI, image);
                            //?}
                            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
                            texture.upload();
                        });
                        frames.add(id);
                    }
                    ANIMATED_CAPES.put(capeName, frames);
                    IS_ANIMATED.put(capeName, true);
                } else {
                    NativeImage img = NativeImage.read(Files.newInputStream(capePath));
                    Identifier textureId = Identifier.of("catcapes", "capes/" + capeName);

                    MinecraftClient.getInstance().execute(() -> {
                        NativeImageBackedTexture texture = //? if <=1.21.4 {
                                /*new NativeImageBackedTexture(img);
                                 *///?} else {
                                new NativeImageBackedTexture(() -> "catcapes:capes/" + capeName, img);
                        //?}
                        MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
                        texture.upload();
                        CAPE_CACHE.put(capeName, textureId);
                        IS_ANIMATED.put(capeName, false);
                    });
                }
            } catch (Exception e) {
                NO_CAPE.add(uuid);
                System.err.println("Failed to load cape for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
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

    public static UUID getUUID(PlayerEntity player) {
        return player.getUuid();
    }
    public static UUID getUUID(PlayerListEntry player) {
        return player.getProfile()
        //? if <=1.21.8 {
            /*.getId()
        *///? } else {
            .id()
        //? }
        ;
    }
}