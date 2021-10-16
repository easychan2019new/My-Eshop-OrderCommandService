package com.myeshop.OrderCommandService.controller;

import com.myeshop.OrderCommandService.command.CreateOrderCommand;
import com.myeshop.OrderCommandService.rest.CreateOrderRest;
import com.myeshop.OrderCommandService.rest.PlaceOrderResponse;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/order") // http://localhost:8082/order-command-service/order
public class OrderController {

    private final CommandGateway commandGateway;

    public OrderController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping("/createOrder")
    public PlaceOrderResponse createOrder(@RequestBody CreateOrderRest createOrderRest) {
        CreateOrderCommand createOrderCommand = CreateOrderCommand.builder()
                .orderId(UUID.randomUUID().toString())
                .totalPrice(createOrderRest.getTotalPrice())
                .totalQuantity(createOrderRest.getTotalQuantity())
                .status("Created (Awaiting for paying)")
                .dateCreated(new Date())
                .lastUpdated(new Date())
                .customerEmail(createOrderRest.getCustomerEmail())
                .addressId(createOrderRest.getAddressId())
                .paymentId(createOrderRest.getPaymentId())
                .cartItems(createOrderRest.getCartItems())
                .build();

        String returnValue;

        try {
            returnValue = commandGateway.sendAndWait(createOrderCommand);
        } catch (Exception ex) {
            returnValue = ex.getLocalizedMessage();
        }

        PlaceOrderResponse response = new PlaceOrderResponse(returnValue);
        return response;
    }
}
