package service;

import server.ClientHandler;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServiceImpl implements ChatService {

    private ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    @Override
    public void addClient(String username, ClientHandler client) {
        clients.put(username, client);
        broadcast(username + " joined the chat", null);
    }

    @Override
    public void removeClient(String username) {
        clients.remove(username);
        broadcast(username + " left the chat", null);
    }

    @Override
    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients.values()) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    @Override
    public void sendPrivateMessage(String toUser, String message, String fromUser) {
        ClientHandler client = clients.get(toUser);
        if (client != null) {
            client.sendMessage("[PRIVATE] " + fromUser + ": " + message);
        }
    }

    @Override
    public String getActiveUsers() {
        return "Active users: " + clients.keySet();
    }
}