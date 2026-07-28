package com.shamsma.api.payment;

public record CmiCheckoutSession(String cmiTransactionId, String checkoutUrl) {}
