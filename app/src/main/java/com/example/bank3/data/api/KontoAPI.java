package com.example.bank3.data.api;

import com.example.bank3.model.KontoResponse;
import com.example.bank3.model.SaldoResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface KontoAPI {
    @GET("api/konta/uzytkownik/{uzytkownikId}/saldo")
    Call<SaldoResponse> getSaldoUzytkownika(@Path("uzytkownikId") long uzytkownikId);

    @GET("api/konta/uzytkownik/{uzytkownikId}")
    Call<List<KontoResponse>> getKontaUzytkownika(@Path("uzytkownikId") long uzytkownikId);
}
