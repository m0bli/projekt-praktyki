package com.example.bank3.network;

import com.example.bank3.BuildConfig;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static final Retrofit RETROFIT = new Retrofit.Builder()
            .baseUrl(BuildConfig.BANK_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private RetrofitClient() {
    }

    public static Retrofit getInstance() {
        return RETROFIT;
    }
}
