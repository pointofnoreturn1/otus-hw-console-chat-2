package io.vaku.model;

import java.util.UUID;

public class User {
    private UUID id;
    private String login;
    private String password;
    private String username;
    private boolean isAdmin;

    public User(UUID id, String login, String password, String username, boolean isAdmin) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.username = username;
        this.isAdmin = isAdmin;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}
