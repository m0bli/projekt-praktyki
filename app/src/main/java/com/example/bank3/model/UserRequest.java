
package com.example.bank3.model;

public class UserRequest {

    private String imie;
    private String nazwisko;
    private String login;
    private String email;
    private String haslo;

    public UserRequest(
            String imie,
            String nazwisko,
            String login,
            String email,
            String haslo
    ) {
        this.imie = imie;
        this.nazwisko = nazwisko;
        this.login = login;
        this.email = email;
        this.haslo = haslo;
    }

    public String getImie() {
        return imie;
    }

    public String getNazwisko() {
        return nazwisko;
    }

    public String getLogin() {
        return login;
    }

    public String getEmail() {
        return email;
    }

    public String getHaslo() {
        return haslo;
    }
}
