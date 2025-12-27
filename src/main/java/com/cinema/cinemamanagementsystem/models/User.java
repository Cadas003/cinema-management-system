package com.cinema.cinemamanagementsystem.models;

public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private int roleId;
    private String roleName;
    private String login;
    private String fullName;

    // Конструкторы
    public User() {}

    public User(String username, String login, String fullName, int roleId) {
        this.username = username;
        this.login = login;
        this.fullName = fullName;
        this.roleId = roleId;
    }

    // Геттеры и сеттеры
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public boolean isAdmin() {
        return roleId == 2; // 2 = Администратор в вашей БД
    }

    public boolean isCashier() {
        return roleId == 1; // 1 = Кассир в вашей БД
    }

    @Override
    public String toString() {
        return fullName + " (" + roleName + ")";
    }
}