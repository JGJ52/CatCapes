package hu.jgj52.catcapes.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public class ModWebSocket implements WebSocket.Listener {

    private WebSocket webSocket;

    public void connect(String url) {
        HttpClient client = HttpClient.newHttpClient();
        webSocket = client.newWebSocketBuilder()
                .buildAsync(URI.create(url), this)
                .join();
    }

    public void send(String message) {
        if (webSocket != null) {
            webSocket.sendText(message, true);
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client closed");
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("Connected");
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String message = data.toString();
        if (message.startsWith("reloadcapes")) {
            UUID player = UUID.fromString(message.split(" ")[1]);
            Utils.removePlayerCache(player);
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        error.printStackTrace();
    }
}
