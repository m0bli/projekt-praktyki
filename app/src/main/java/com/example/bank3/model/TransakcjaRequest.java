package com.example.bank3.model;

import java.math.BigDecimal;

public class TransakcjaRequest {
    private Long uzytkownikId;
    private Long kontoZrodloweId;
    private String numerKontaOdbiorcy;
    private String imieOdbiorcy;
    private String typ = "PRZELEW";
    private BigDecimal kwota;
    private String waluta;
    private String tytul;

    public TransakcjaRequest(long userId, long sourceAccountId, String receiverAccount,
                             String receiverName, BigDecimal amount, String currency, String title) {
        this.uzytkownikId = userId;
        this.kontoZrodloweId = sourceAccountId;
        this.numerKontaOdbiorcy = receiverAccount;
        this.imieOdbiorcy = receiverName;
        this.kwota = amount;
        this.waluta = currency;
        this.tytul = title;
    }
}
