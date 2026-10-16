package com.example.bank3.data.repository;

import com.example.bank3.data.api.TransakcjaAPI;
import com.example.bank3.model.TransakcjaRequest;
import com.example.bank3.network.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class TransakcjaRepository {
    private final TransakcjaAPI api;

    public TransakcjaRepository() {
        api = RetrofitClient.getInstance().create(TransakcjaAPI.class);
    }

    public Call<ResponseBody> wykonajPrzelew(TransakcjaRequest request) {
        return api.wykonajPrzelew(request);
    }
}
