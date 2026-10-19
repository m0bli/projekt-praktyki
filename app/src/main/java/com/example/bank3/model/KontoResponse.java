package com.example.bank3.model;

import java.math.BigDecimal;

public class KontoResponse {
    private Long id;
    private String numerKonta;
    private BigDecimal saldo;
    private String waluta;

    public Long getId() { return id; }
    public String getNumerKonta() { return numerKonta; }
    public BigDecimal getSaldo() { return saldo; }
    public String getWaluta() { return waluta; }

    @Override
    public String toString() {
        return numerKonta == null ? "Konto" : numerKonta + " · " + (waluta == null ? "" : waluta);
    }
}
