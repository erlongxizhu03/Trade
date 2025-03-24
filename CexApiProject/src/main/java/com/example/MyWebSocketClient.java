package com.example;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class MyWebSocketClient {
    public static void main(String[] args) throws URISyntaxException {
        URI uri = new URI("wss://test-futures-ws.ln.exchange/kline-api/ws"); // 修改为你的WebSocket服务器地址
        WebSocketClient client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                System.out.println("Connected");
                send("{\"event\":\"req\",\"params\":{\"channel\":\"market_e_btcusdt_trade_ticker\",\"cb_id\":\"e_btcusdt\"}}"); // 发送消息到服务器
            }

            @Override
            public void onMessage(String message) {
                System.out.println("Received: " + message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("Disconnected 1231");
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace(); // 打印错误信息到控制台
            }
        };
        client.connect(); // 连接到服务器
    }
}