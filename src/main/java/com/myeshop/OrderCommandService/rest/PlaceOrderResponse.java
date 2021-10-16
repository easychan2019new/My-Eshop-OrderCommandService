package com.myeshop.OrderCommandService.rest;

import lombok.Data;
import lombok.NonNull;

@Data
public class PlaceOrderResponse {

    @NonNull
    private String response;
}
