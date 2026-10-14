package com.example.bank3.data.repository;

import com.example.bank3.data.api.AuthAPI;
import com.example.bank3.data.api.UserAPI;
import com.example.bank3.model.LoginRequest;
import com.example.bank3.model.AuthResponse;
import com.example.bank3.model.UserRequest;
import com.example.bank3.model.UserResponse;
import com.example.bank3.network.RetrofitClient;

import retrofit2.Call;

public class AuthRepository {

    private final AuthAPI authAPI;
    private final UserAPI userAPI;

    public AuthRepository() {
        authAPI = RetrofitClient.getInstance().create(AuthAPI.class);
        userAPI = RetrofitClient.getInstance().create(UserAPI.class);
    }

    public Call<AuthResponse> login(LoginRequest request) {
        return authAPI.login(request);
    }

    public Call<UserResponse> register(UserRequest request) {
        return userAPI.createUser(request);
    }
}