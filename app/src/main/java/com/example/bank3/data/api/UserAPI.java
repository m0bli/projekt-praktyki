
package com.example.bank3.data.api;

import com.example.bank3.model.UserRequest;
import com.example.bank3.model.UserResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface UserAPI {
    @GET("api/uzytkownicy")
    Call<List<UserResponse>> getUsers();

    @POST("api/uzytkownicy")
    Call<UserResponse> createUser(
            @Body UserRequest request
    );
}
