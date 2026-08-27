package com.acme.salary.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "currency")
public class Currency {

    @Id
    @Column(length = 3)
    private String code;

    @Column(name = "fx_to_usd", nullable = false)
    private BigDecimal fxToUsd;

    protected Currency() {
    }

    public Currency(String code, BigDecimal fxToUsd) {
        this.code = code;
        this.fxToUsd = fxToUsd;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getFxToUsd() {
        return fxToUsd;
    }
}
