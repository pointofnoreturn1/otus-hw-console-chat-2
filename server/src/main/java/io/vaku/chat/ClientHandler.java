package io.vaku.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        init();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private void init() {
        new Thread(() -> {
            try {
                System.out.println("Клиент подключился");
                // цикл аутентификации
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.startsWith("/exit")) {
                            sendMessage("/exitok");
                            break;
                        }
                        // /auth login password
                        if (message.startsWith("/auth ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 3) {
                                sendMessage("Неверный формат команды /auth");
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .authenticate(this, elements[1], elements[2])) {
                                break;
                            }
                            continue;
                        }

                        // /reg login password username
                        if (message.startsWith("/reg ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 4) {
                                sendMessage("Неверный формат команды /reg");
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .registration(this, elements[1], elements[2], elements[3])) {
                                break;
                            }
                            continue;
                        }
                    }
                    sendMessage("Перед работой необходимо пройти аутентификацию командой " +
                            "/auth login password или регистрацию командой /reg login password username");
                }
                System.out.println("Клиент " + username + " успешно прошел аутентификацию");

                // цикл работы
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.startsWith("/exit")) {
                            sendMessage("/exitok");
                            break;
                        }

                        if (message.startsWith("/w")) {
                            String[] arr = message.split(" ");
                            if (arr.length < 3) {
                                sendMessage("Invalid input, try again");
                                continue;
                            }
                            String addresseeName = arr[1];
                            if (server.isClientExists(addresseeName)) {
                                server.sendMessageTo(
                                        getUsername(),
                                        addresseeName,
                                        message.substring(message.indexOf(addresseeName) + addresseeName.length())
                                );
                            } else {
                                sendMessage("Error: there is no client with username " + addresseeName);
                                continue;
                            }
                        }

                        if (message.startsWith("/kick")) {
                            String[] arr = message.split(" ");
                            String usernameToKick = arr[1];
                            AuthenticatedProvider authProvider = server.getAuthenticatedProvider();
                            if (arr.length == 2 && authProvider.isAdmin(username)) {
                                if (server.isClientExists(usernameToKick)) {
                                    server.kick(usernameToKick);
                                } else {
                                    sendMessage("Error: there is no client with username " + usernameToKick);
                                }
                            } else {
                                sendMessage("Invalid input, try again");
                            }
                        }
                    } else {
                        server.broadcastMessage(this, username + " : " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
