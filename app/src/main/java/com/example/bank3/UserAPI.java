package com.example.bank3;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface UserAPI {

    @GET("api/uzytkownicy")
    Call<List<UserResponse>> getUsers();
}