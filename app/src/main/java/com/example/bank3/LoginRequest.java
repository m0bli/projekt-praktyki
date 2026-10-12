package com.example.bank3;

public class LoginRequest {

    private String login;
    private String haslo;

    public LoginRequest(String login, String haslo) {
        this.login = login;
        this.haslo = haslo;
    }

    public String getLogin() {
        return login;
    }

    public String getHaslo() {
        return haslo;
    }
}