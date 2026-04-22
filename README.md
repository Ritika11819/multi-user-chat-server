# 💬 Multi-User Chat Server in Java

A real-time, multi-client chat application built from scratch in Java, demonstrating core concepts of **Computer Networking**, **Multithreading**, and **Object-Oriented Design**. Multiple users can connect simultaneously, exchange public messages, send private messages, and manage sessions — all over TCP sockets.

---

## 📌 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Project Structure](#project-structure)
- [Architecture & Design](#architecture--design)
- [Class Breakdown](#class-breakdown)
- [Concepts Used](#concepts-used)
- [How to Run](#how-to-run)
- [Chat Commands](#chat-commands)
- [Sample Session](#sample-session)
- [Design Decisions](#design-decisions)

---

## Overview

This project simulates a real-world group chat server where:

- A **central server** listens for incoming client connections on port `5000`
- Each **client** gets its own dedicated thread managed via a thread pool
- All clients share a **single ChatService instance**, making it a true group chat
- Users can broadcast to everyone, whisper privately, list active users, and exit cleanly

---

## Features

| Feature | Description |
|---|---|
| Multi-client support | Up to 10 concurrent users via a fixed thread pool |
| Real-time messaging | Full-duplex communication using threads |
| Private messaging | Send direct messages with `/msg <user> <message>` |
| Active user list | See who's online with `/users` |
| Rate limiting (Token Bucket) | Spam protection — each user has 5 tokens, refilled every 2 seconds |
| Graceful exit | Clean disconnect with `/exit` |
| Join/leave announcements | Server broadcasts when users join or leave |

---

## Project Structure

```
ChatApp/
│
├── client/
│   └── ChatClient.java          # Client-side logic (connect, send, receive)
│
├── server/
│   ├── ChatServer.java          # Server entry point (accept connections, spawn threads)
│   └── ClientHandler.java       # Per-client thread (reads input, routes commands)
│
└── service/
    ├── ChatService.java         # Interface defining chat operations
    └── ChatServiceImpl.java     # Implementation using ConcurrentHashMap
```

---

## Architecture & Design

```
                        ┌─────────────────────────────┐
                        │         ChatServer           │
                        │  ServerSocket (port 5000)    │
                        │  ExecutorService (pool=10)   │
                        └────────────┬────────────────┘
                                     │ accepts connections
               ┌─────────────────────┼─────────────────────┐
               │                     │                     │
        ┌──────▼──────┐       ┌──────▼──────┐      ┌──────▼──────┐
        │ClientHandler│       │ClientHandler│      │ClientHandler│
        │  (Thread 1) │       │  (Thread 2) │      │  (Thread N) │
        └──────┬──────┘       └──────┬──────┘      └──────┬──────┘
               │                     │                     │
               └─────────────────────┼─────────────────────┘
                                     │ shared single instance
                              ┌──────▼──────┐
                              │ ChatService │
                              │    Impl     │
                              │ (ConcurrentHashMap) │
                              └─────────────┘
```

The key architectural decision: **`ChatService` is instantiated once in `ChatServer` and passed to every `ClientHandler`**. This shared state is what enables group broadcasting — all threads write to and read from the same map of connected clients.

---

## Class Breakdown

### `ChatClient.java` — The Client

Connects to the server at `localhost:5000` and sets up two-way communication.

**Why two threads?**
Without a dedicated read thread, the client would block on `readLine()` waiting for server messages and would be unable to send at the same time. Spawning a separate `readThread` allows **simultaneous sending and receiving** — true full-duplex communication.

```java
// Receiver thread — runs independently in background
Thread readThread = new Thread(() -> {
    while ((msg = in.readLine()) != null) {
        System.out.println(msg);
    }
});
readThread.start();

// Main thread handles sending
while ((input = userInput.readLine()) != null) {
    out.println(input);
}
```

---

### `ChatServer.java` — The Server

The entry point. Starts the `ServerSocket`, creates the shared `ChatService`, and loops forever accepting new clients.

**Thread Pool:** Uses `Executors.newFixedThreadPool(10)` — this limits concurrency to 10 threads. Instead of spawning a new `Thread` for every client (which would crash under heavy load), tasks are submitted to a managed pool. This is standard production practice for scalability.

```java
ChatService chatService = new ChatServiceImpl(); // ONE instance, shared
ExecutorService pool = Executors.newFixedThreadPool(10);

while (true) {
    Socket socket = server.accept();
    ClientHandler client = new ClientHandler(socket, chatService);
    pool.execute(client); // runs in a pooled thread
}
```

---

### `ClientHandler.java` — Per-Client Logic

Implements `Runnable`. Each instance handles one connected client: reads their messages, parses commands, and delegates to `ChatService`.

**Token Bucket Rate Limiter:**
Prevents spam without blocking legitimate users. Each user starts with 5 tokens. Every message consumes 1 token. Tokens refill at 2 per 2 seconds (max cap: 5).

```java
private int tokens = 5;
private long lastRefillTime = System.currentTimeMillis();

private boolean allowMessage() {
    long now = System.currentTimeMillis();
    if (now - lastRefillTime > 2000) {
        tokens = Math.min(tokens + 2, 5); // refill, cap at 5
        lastRefillTime = now;
    }
    if (tokens > 0) { tokens--; return true; }
    return false;
}
```

**Command Routing:**

| Input | Action |
|---|---|
| `/exit` | Break loop → `finally` block closes socket and removes user |
| `/users` | Returns active user list from ChatService |
| `/msg <user> <text>` | Private message routed via ChatService |
| Fails `allowMessage()` | Warned about spam, message dropped |
| Anything else | Broadcast to all other users |

---

### `ChatService.java` — The Interface

Defines the contract for all chat operations. By coding to an interface, the server and handlers are **decoupled** from the implementation — you could swap `ChatServiceImpl` with a database-backed or persistent version without touching any other class.

```java
public interface ChatService {
    void broadcast(String message, ClientHandler sender);
    void addClient(String username, ClientHandler client);
    void removeClient(String username);
    void sendPrivateMessage(String toUser, String message, String fromUser);
    String getActiveUsers();
}
```

---

### `ChatServiceImpl.java` — The Implementation

The shared brain of the chat server. Stores all connected clients in a `ConcurrentHashMap<String, ClientHandler>`.

**Why `ConcurrentHashMap`?**
Multiple threads (one per client) call `addClient`, `removeClient`, and `broadcast` concurrently. A regular `HashMap` would cause `ConcurrentModificationException` or data corruption under concurrent access. `ConcurrentHashMap` is thread-safe without needing explicit `synchronized` blocks.

```java
private ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

@Override
public void broadcast(String message, ClientHandler sender) {
    for (ClientHandler client : clients.values()) {
        if (client != sender) {   // don't echo back to sender
            client.sendMessage(message);
        }
    }
}
```

**`addClient` / `removeClient`** automatically broadcast join/leave announcements to all users by calling `broadcast` internally.

---

## Concepts Used

### 🔌 Networking
- **`ServerSocket`** — binds to port 5000, waits for incoming TCP connections
- **`Socket`** — represents a connection endpoint on both server and client
- **`BufferedReader` / `PrintWriter`** — text-based I/O streams over the socket

### 🧵 Multithreading
- **`ExecutorService` (Thread Pool)** — reuses a fixed set of threads rather than creating unlimited ones
- **`Runnable`** — each `ClientHandler` is a task submitted to the pool
- **Daemon read thread in client** — allows simultaneous I/O on the client side

### 🔒 Thread Safety
- **`ConcurrentHashMap`** — thread-safe map for storing active clients
- Shared `ChatService` safely accessed by multiple `ClientHandler` threads

### 🏗️ OOP & Design
- **Interface + Implementation** (`ChatService` / `ChatServiceImpl`) — separation of contract from logic
- **Single Responsibility** — each class has one clear job
- **Dependency Injection** — `ChatService` is injected into `ClientHandler` via constructor

---

## How to Run

### Prerequisites
- Java 8 or above
- A terminal/IDE that supports running multiple processes

### Step 1 — Compile

```bash
javac -d out server/ChatServer.java server/ClientHandler.java service/ChatService.java service/ChatServiceImpl.java client/ChatClient.java
```

### Step 2 — Start the Server

```bash
java -cp out server.ChatServer
```

Output:
```
Server started...
Waiting for clients...
```

### Step 3 — Connect Clients (in separate terminals)

```bash
java -cp out client.ChatClient
```

Each terminal is an independent user. Open 2–3 terminals to simulate a group chat.

---

## Chat Commands

| Command | Description |
|---|---|
| *(any text)* | Send a message to everyone |
| `/msg <username> <message>` | Send a private message to a specific user |
| `/users` | List all currently connected users |
| `/exit` | Disconnect from the server |

---

## Sample Session

**Terminal 1 (Alice):**
```
Enter username:
Alice
Bob joined the chat
Bob: hey alice!
/msg Bob hi bob, just you and me here
```

**Terminal 2 (Bob):**
```
Enter username:
Bob
Bob: hey alice!
[PRIVATE] Alice: hi bob, just you and me here
/users
Active users: [Alice, Bob]
```

---

## Design Decisions

**Why is `ChatService` created in `ChatServer` and not inside `ClientHandler`?**
If each `ClientHandler` created its own `ChatServiceImpl`, every user would have a private isolated chat — they'd never see each other's messages. The single shared instance is what forms the group chat room.

**Why use an interface for `ChatService`?**
Decoupling the contract from the implementation makes the code testable and extensible. You can write a mock `ChatService` for unit tests, or a `PersistentChatServiceImpl` that logs messages to a file, without changing a line in `ClientHandler` or `ChatServer`.

**Why `ConcurrentHashMap` over `HashMap` with `synchronized`?**
`ConcurrentHashMap` uses fine-grained segment locking internally, allowing multiple threads to read/write different segments simultaneously. A fully `synchronized` `HashMap` would serialize all access, creating a bottleneck as users scale up.

**Why Token Bucket over simple message count?**
A per-message counter would permanently throttle an active user. Token Bucket is time-aware — it automatically recovers, rewarding legitimate users who slow down while still blocking burst spammers.

---

> Built with Java — networking, multithreading, and clean OOP from the ground up.
