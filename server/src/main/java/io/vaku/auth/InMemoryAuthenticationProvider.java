package io.vaku.auth;

import io.vaku.chat.ClientHandler;
import io.vaku.chat.Server;
import io.vaku.model.User;

import java.util.ArrayList;
import java.util.List;

import static io.vaku.util.Utils.getUUID;

public class InMemoryAuthenticationProvider implements AuthenticatedProvider {
    private Server server;
    private List<User> users;

    public InMemoryAuthenticationProvider(Server server) {
        this.server = server;
        this.users = new ArrayList<>();
        this.users.add(new User(getUUID(),"login1", "password1", "username1", false));
        this.users.add(new User(getUUID(), "qwe", "qwe", "qwe1", false));
        this.users.add(new User(getUUID(), "asd", "asd", "asd1", false));
        this.users.add(new User(getUUID(), "zxc", "zxc", "zxc1",false));
        this.users.add(new User(getUUID(), "admin", "admin", "administrator",true));
    }

    @Override
    public void initialize() {
        System.out.println("Сервис аутентификации запущен: In memory режим");
    }

    private String getUsernameByLoginAndPassword(String login, String password) {
        for (User user : users) {
            if (user.getLogin().equals(login) && user.getPassword().equals(password)) {
                return user.getUsername();
            }
        }

        return null;
    }

    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String authName = getUsernameByLoginAndPassword(login, password);

        if (authName == null) {
            clientHandler.sendMessage("Некорректный логин/пароль");
            return false;
        }

        if (server.isClientExists(authName)) {
            clientHandler.sendMessage("Учетная запись уже занята");
            return false;
        }

        clientHandler.setUsername(authName);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authName);

        return true;
    }

    private boolean isLoginAlreadyExist(String login) {
        for (User user : users) {
            if (user.getLogin().equals(login)) {
                return true;
            }
        }

        return false;
    }

    private boolean isUsernameAlreadyExist(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3 || password.trim().length() < 6
                || username.trim().length() < 2) {
            clientHandler.sendMessage("Требования логин 3+ символа, пароль 6+ символа," +
                    "имя пользователя 2+ символа не выполнены");
            return false;
        }

        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }

        if (isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }

        users.add(new User(getUUID(), login, password, username, false));
        clientHandler.setUsername(username);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + username);

        return true;
    }

    @Override
    public boolean isAdmin(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username) && user.isAdmin()) {
                return true;
            }
        }

        return false;
    }
}
