package com.tetris.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkManager {
    public interface NetworkCallback {
        void onConnected();
        void onDisconnected(String reason);
        void onMessageReceived(String message);
    }

    private static NetworkManager instance = null;

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private boolean isHost = false;
    private boolean isConnected = false;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private NetworkCallback currentCallback = null;

    private NetworkManager() {}

    public boolean isHost() {
        return isHost;
    }

    public boolean isConnected() {
        return isConnected;
    }

    // 取得本機區域網路 IP 位址
    public String getLocalIPAddress() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ex) {
                return "127.0.0.1";
            }
        }
    }

    // 啟動 ServerSocket 監聽 (Host)
    public void startHost(int port, NetworkCallback callback) {
        shutdown(); // 確保舊的連線已完全關閉
        this.isHost = true;
        this.currentCallback = callback;

        threadPool.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                // 阻塞等待 Client 連線
                clientSocket = serverSocket.accept();
                
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                isConnected = true;

                if (currentCallback != null) {
                    currentCallback.onConnected();
                }
                
                readMessages();
            } catch (Exception e) {
                if (isConnected) {
                    handleDisconnect(e.getMessage());
                } else {
                    if (currentCallback != null) {
                        currentCallback.onDisconnected("Failed to host on port " + port);
                    }
                }
            }
        });
    }

    // 建立 Socket 連線 (Join)
    public void startJoin(String ip, int port, NetworkCallback callback) {
        shutdown();
        this.isHost = false;
        this.currentCallback = callback;

        threadPool.execute(() -> {
            try {
                clientSocket = new Socket(ip, port);
                
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                isConnected = true;

                if (currentCallback != null) {
                    currentCallback.onConnected();
                }

                readMessages();
            } catch (Exception e) {
                if (currentCallback != null) {
                    currentCallback.onDisconnected("Failed to connect to " + ip + ":" + port);
                }
            }
        });
    }

    // 傳送訊息
    public void send(String msg) {
        threadPool.execute(() -> {
            try {
                if (out != null && isConnected) {
                    out.println(msg);
                }
            } catch (Exception e) {
                System.err.println("Network send error: " + e.getMessage());
            }
        });
    }

    // 讀取網路封包的背景迴圈
    private void readMessages() {
        try {
            String line;
            while (isConnected && (line = in.readLine()) != null) {
                if (currentCallback != null) {
                    currentCallback.onMessageReceived(line);
                }
            }
        } catch (Exception e) {
            // 連線斷開
        } finally {
            handleDisconnect("Connection closed.");
        }
    }

    // 處理斷線
    private synchronized void handleDisconnect(String reason) {
        if (isConnected) {
            isConnected = false;
            if (currentCallback != null) {
                currentCallback.onDisconnected(reason);
            }
            shutdown();
        }
    }

    // 關閉所有網路資源
    public synchronized void shutdown() {
        isConnected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (Exception e) {
            System.err.println("Error shutting down network: " + e.getMessage());
        } finally {
            out = null;
            in = null;
            clientSocket = null;
            serverSocket = null;
        }
    }
}
