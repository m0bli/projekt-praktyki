package com.example.bank3.model;

import java.math.BigDecimal;

public class TransakcjaHistoriaResponse {
    private Long id;
    private Long kontoId;
    private String kierunek;
    private String numerKontaKontrahenta;
    private String typ;
    private BigDecimal kwota;
    private String waluta;
    private String tytul;
    private String dataOperacji;

    public Long getId() { return id; }
    public String getKierunek() { return kierunek; }
    public String getNumerKontaKontrahenta() { return numerKontaKontrahenta; }
    public String getTyp() { return typ; }
    public BigDecimal getKwota() { return kwota; }
    public String getWaluta() { return waluta; }
    public String getTytul() { return tytul; }
    public String getDataOperacji() { return dataOperacji; }
}
