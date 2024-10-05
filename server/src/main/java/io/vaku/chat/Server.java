package io.vaku.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private int port;
    private Map<String, ClientHandler> clients;
    private AuthenticatedProvider authenticatedProvider;

    public Server(int port) {
        this.port = port;
        this.clients = new HashMap<>();
        authenticatedProvider = new InMemoryAuthenticationProvider(this);
        authenticatedProvider.initialize();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AuthenticatedProvider getAuthenticatedProvider() {
        return authenticatedProvider;
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.put(clientHandler.getUsername(), clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler.getUsername());
    }

    public synchronized void broadcastMessage(ClientHandler handler, String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(message);
        }
    }

    public synchronized void sendMessageTo(String from, String to, String message) {
        clients.get(to).sendMessage("Private message from " + from + ":" + message);
    }

    public synchronized void kick(String username) {
        for (String name : clients.keySet()) {
            if (name.equals(username)) {
                var client = clients.get(username);
                client.sendMessage("You were kicked, sorry");
                client.sendMessage("/exitok");
                unsubscribe(client);
            }
        }
    }

    public boolean isClientExists(String username) {
        return clients.containsKey(username);
    }
}
