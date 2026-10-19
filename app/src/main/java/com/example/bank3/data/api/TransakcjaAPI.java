package com.example.bank3.data.api;

import com.example.bank3.model.TransakcjaHistoriaResponse;
import com.example.bank3.model.TransakcjaRequest;
import com.example.bank3.model.TransakcjaResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface TransakcjaAPI {
    @GET("api/transakcje/konto/{kontoId}")
    Call<List<TransakcjaHistoriaResponse>> getHistoriaKonta(@Path("kontoId") long kontoId);

    @POST("api/transakcje")
    Call<TransakcjaResponse> wykonajPrzelew(@Body TransakcjaRequest request);
}
