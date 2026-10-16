package com.tarcisiolzbraga.codeflix.subscription.domain;

// Marcador: value object é igual pelos valores, então todos são record. O que ele acrescenta é o
// AssertionConcern, para o construtor compacto recusar o valor inválido.
public interface ValueObject extends AssertionConcern {}
