package com.example.bank3.data.repository;

import com.example.bank3.data.api.KontoAPI;
import com.example.bank3.model.SaldoResponse;
import com.example.bank3.network.RetrofitClient;

import retrofit2.Call;

public class KontoRepository {

    private final KontoAPI kontoAPI;

    public KontoRepository() {
        kontoAPI = RetrofitClient.getInstance().create(KontoAPI.class);
    }

    public Call<SaldoResponse> getSaldoUzytkownika(long uzytkownikId) {
        return kontoAPI.getSaldoUzytkownika(uzytkownikId);
    }
}
