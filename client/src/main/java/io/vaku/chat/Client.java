package io.vaku.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Scanner scanner;

    public Client() throws IOException {
        this.scanner = new Scanner(System.in);
        this.socket = new Socket("localhost", 8189);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        init();
        work();
    }

    private void init() {
        new Thread(() -> {
            try {
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.startsWith("/exitok")) {
                            break;
                        }
                        if (message.startsWith("/authok ")) {
                            System.out.println("Аутентификация прошла успешно с именем пользователя: " +
                                    message.split(" ")[1]);
                        }
                        if (message.startsWith("/regok ")) {
                            System.out.println("регистрация прошла успешно с именем пользователя: " +
                                    message.split(" ")[1]);
                        }
                    } else {
                        System.out.println(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    private void work() throws IOException {
        while (true) {
            String message = scanner.nextLine();
            out.writeUTF(message);
            if (message.startsWith("/exit")) {
                break;
            }
        }
    }

    public void disconnect() {
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