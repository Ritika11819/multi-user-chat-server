package service;

import server.ClientHandler;

public interface ChatService {
    void broadcast(String message, ClientHandler sender);
    void addClient(String username, ClientHandler client);
    void removeClient(String username);
    void sendPrivateMessage(String toUser, String message, String fromUser);
    String getActiveUsers();
}