package com.acme.salary.reference;

public record CountryDto(String code, String name) {
    public static CountryDto from(Country country) {
        return new CountryDto(country.getCode(), country.getName());
    }
}
