package com.example.bank3;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthAPI {

    // Logowanie
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // Rejestracja nowego użytkownika
    @POST("api/uzytkownicy")
    Call<UserResponse> register(@Body UserRequest request);
}