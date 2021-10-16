package com.myeshop.OrderCommandService.rest;

import com.myeshop.Core.order.rest.CartItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class CreateOrderRest {

    private BigDecimal totalPrice;
    private int totalQuantity;
    private String customerEmail;
    private String addressId;
    private String paymentId;
    private Set<CartItem> cartItems;
}
