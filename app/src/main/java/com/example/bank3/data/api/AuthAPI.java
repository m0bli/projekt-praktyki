package com.example.bank3.data.api;

import com.example.bank3.model.AuthResponse;
import com.example.bank3.model.LoginRequest;
import com.example.bank3.model.UserRequest;
import com.example.bank3.model.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface
AuthAPI {

    // Logowanie
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // Rejestracja nowego użytkownika
    @POST("api/uzytkownicy")
    Call<UserResponse> register(@Body UserRequest request);
}