package com.example.bank3.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class TransakcjaRequest {
    @SerializedName("uzytkownikId")
    private final long uzytkownikId;
    @SerializedName("kontoZrodloweId")
    private final long kontoZrodloweId;
    @SerializedName("numerKontaOdbiorcy")
    private final String numerKontaOdbiorcy;
    @SerializedName("imieOdbiorcy")
    private final String imieOdbiorcy;
    @SerializedName("typ")
    private final String typ = "PRZELEW";
    @SerializedName("kwota")
    private final BigDecimal kwota;
    @SerializedName("waluta")
    private final String waluta;
    @SerializedName("tytul")
    private final String tytul;

    public TransakcjaRequest(long uzytkownikId, long kontoZrodloweId,
                             String numerKontaOdbiorcy, String imieOdbiorcy,
                             BigDecimal kwota, String waluta, String tytul) {
        this.uzytkownikId = uzytkownikId;
        this.kontoZrodloweId = kontoZrodloweId;
        this.numerKontaOdbiorcy = numerKontaOdbiorcy;
        this.imieOdbiorcy = imieOdbiorcy;
        this.kwota = kwota;
        this.waluta = waluta;
        this.tytul = tytul;
    }
}
