package com.example.bank3.data.api;

import com.example.bank3.model.SaldoResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface KontoAPI {

    @GET("api/konta/uzytkownik/{uzytkownikId}/saldo")
    Call<SaldoResponse> getSaldoUzytkownika(
            @Path("uzytkownikId") long uzytkownikId
    );

    @GET("api/konta/uzytkownik/{uzytkownikId}")
    Call<List<Map<String, Object>>> getAccounts(
            @Path("uzytkownikId") long uzytkownikId
    );
}
