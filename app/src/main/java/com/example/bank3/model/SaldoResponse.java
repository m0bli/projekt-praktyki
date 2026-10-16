package com.example.bank3.model;

import java.math.BigDecimal;

public class SaldoResponse {
    private Long kontoId;
    private BigDecimal saldo;
    private String waluta;

    public Long getKontoId() {
        return kontoId;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public String getWaluta() {
        return waluta;
    }
}