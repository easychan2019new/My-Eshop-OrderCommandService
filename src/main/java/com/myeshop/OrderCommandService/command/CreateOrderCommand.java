package com.myeshop.OrderCommandService.command;

import com.myeshop.Core.order.rest.CartItem;
import lombok.Builder;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

@Builder
@Data
public class CreateOrderCommand {

    @TargetAggregateIdentifier
    private String orderId;
    private BigDecimal totalPrice;
    private int totalQuantity;
    private String status;
    private Date dateCreated;
    private Date lastUpdated;
    private String customerEmail;
    private String addressId;
    private String paymentId;
    private Set<CartItem> cartItems;
}
