package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

// Sem o id da conta: quem chama muda o próprio endereço, e quem ele é vem do token.
public record BillingAddressRequest(String zipcode, String number, String complement, String country) {}
