package com.example.bank3.data.api;

import com.example.bank3.model.TransakcjaRequest;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface TransakcjaAPI {
    @POST("api/transakcje")
    Call<ResponseBody> wykonajPrzelew(@Body TransakcjaRequest request);
}
