package com.example.escapeGame;

public class User {
    private String username;
    private String password;
    private String email;
    private String role;
    private String avatar;

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = "user"; // Default role
        this.avatar = "boy_1.png"; // Default avatar
    }

    public User(String username, String password, String email, String role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.avatar = "boy_1.png"; // Default avatar
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getAvatar() { return avatar; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
