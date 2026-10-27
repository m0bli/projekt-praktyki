
package com.example.bank3.data.api;

import com.example.bank3.model.UserRequest;
import com.example.bank3.model.UserResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface UserAPI {
    @GET("api/uzytkownicy")
    Call<List<UserResponse>> getUsers();

    @GET("api/uzytkownicy/{id}")
    Call<UserResponse> getUserById(@Path("id") long id);

    @POST("api/uzytkownicy")
    Call<UserResponse> createUser(
            @Body UserRequest request
    );

    @PUT("api/uzytkownicy/{id}")
    Call<UserResponse> updateUser(
            @Path("id") long id,
            @Body UserRequest request
    );
}
