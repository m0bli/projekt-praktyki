package com.example.bank3.data.repository;

import com.example.bank3.data.api.TransakcjaAPI;
import com.example.bank3.model.TransakcjaHistoriaResponse;
import com.example.bank3.model.TransakcjaResponse;
import com.example.bank3.model.TransakcjaRequest;
import com.example.bank3.network.RetrofitClient;

import java.util.List;
import retrofit2.Call;

public class TransakcjaRepository {
    private final TransakcjaAPI api = RetrofitClient.getInstance().create(TransakcjaAPI.class);

    public Call<List<TransakcjaHistoriaResponse>> getHistoriaKonta(long kontoId) {
        return api.getHistoriaKonta(kontoId);
    }

    public Call<TransakcjaResponse> wykonajPrzelew(TransakcjaRequest request) {
        return api.wykonajPrzelew(request);
    }
}
