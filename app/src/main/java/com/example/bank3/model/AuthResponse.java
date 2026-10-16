package com.example.bank3.model;

public class AuthResponse {

    public boolean sukces;
    public String komunikat;
    public Long uzytkownikId;

    public boolean isSukces() {
        return sukces;
    }

    public String getKomunikat() {
        return komunikat;
    }

    public Long getUzytkownikId() {
        return uzytkownikId;
    }
}
