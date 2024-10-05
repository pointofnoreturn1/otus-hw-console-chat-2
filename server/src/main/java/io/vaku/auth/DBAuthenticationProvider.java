package io.vaku.auth;

import io.vaku.chat.ClientHandler;
import io.vaku.chat.Server;
import io.vaku.model.User;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

import static io.vaku.util.Utils.getUUID;

public class DBAuthenticationProvider implements AuthenticatedProvider {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/console-chat";
    private static final String DB_USERNAME = "postgres";
    private static final String DB_PASSWORD = "mysecretpassword";

    private final Server server;

    public DBAuthenticationProvider(Server server) {
        this.server = server;
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }

    @Override
    public void initialize() {
        System.out.println("Сервис аутентификации запущен: DB режим");
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        Optional<User> optionalUser = findUserByLogin(login);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            if (!user.getLogin().equals(login) || !user.getPassword().equals(password)) {
                clientHandler.sendMessage("Некорректный логин/пароль");

                return false;
            }

            String username = user.getUsername();
            if (server.isClientExists(username)) {
                clientHandler.sendMessage("Учетная запись уже занята");

                return false;
            }

            clientHandler.setUsername(username);
            server.subscribe(clientHandler);
            clientHandler.sendMessage("/authok " + username);

            return true;
        }

        return false;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3 || password.trim().length() < 6
                || username.trim().length() < 2) {
            clientHandler.sendMessage("Требования логин 3+ символа, пароль 6+ символа, " +
                    "имя пользователя 2+ символа не выполнены");
            return false;
        }

        if (findUserByLogin(login).isPresent()) {
            clientHandler.sendMessage("Указанный логин уже занят");

            return false;
        }

        if (findUserByUsername(username).isPresent()) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");

            return false;
        }

        saveUser(new User(getUUID(), login, password, username, false));
        clientHandler.setUsername(username);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + username);

        return true;
    }

    @Override
    public boolean isAdmin(String username) {
        return findUserByUsername(username).map(User::isAdmin).orElse(false);
    }

    private Optional<User> findUserByUsername(String username) {
        try (
                Connection connection = getConnection();
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM \"user\" WHERE username = ?")
        ) {
            stmt.setString(1, username);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(createUser(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<User> findUserByLogin(String login) {
        try (
                Connection connection = getConnection();
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM \"user\" WHERE login = ?")
        ) {
            stmt.setString(1, login);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(createUser(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveUser(User user) {
        try (
                Connection connection = getConnection();
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO \"user\" VALUES (?, ?, ?, ?, ?)")
        ) {
            stmt.setObject(1, user.getId());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getUsername());
            stmt.setBoolean(5, user.isAdmin());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User createUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getObject(1, UUID.class),
                resultSet.getString(2),
                resultSet.getString(3),
                resultSet.getString(4),
                resultSet.getBoolean(5)
        );
    }
}
