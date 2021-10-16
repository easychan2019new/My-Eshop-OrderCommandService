package com.myeshop.OrderCommandService.saga;

import com.myeshop.Core.Product.command.ReserveProductCommand;
import com.myeshop.Core.Product.command.RollbackProductCommand;
import com.myeshop.Core.order.command.CancelOrderCommand;
import com.myeshop.Core.order.event.OrderApprovedEvent;
import com.myeshop.Core.order.event.OrderCreatedEvent;
import com.myeshop.Core.order.event.OrderRejectedEvent;
import com.myeshop.Core.order.rest.CartItem;
import com.myeshop.Core.payment.command.ProcessPaymentCommand;
import com.myeshop.Core.payment.event.PaymentProcessedEvent;
import com.myeshop.Core.payment.query.FetchPaymentDetailQuery;
import com.myeshop.Core.payment.rest.PaymentDetail;
import com.myeshop.OrderCommandService.command.ApproveOrderCommand;
import com.myeshop.OrderCommandService.command.RejectOrderCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Saga
public class OrderSage {

    private String id;
    private Set<CartItem> reserved;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSage.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient QueryGateway queryGateway;

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderCreatedEvent orderCreatedEvent) {
        this.id = orderCreatedEvent.getOrderId();
        this.reserved = new HashSet<>();

        // 1. Try to reserve product
        try {
            LOGGER.info("Try to reserve products!");
            for(CartItem item: orderCreatedEvent.getCartItems()) {
                ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .orderId(orderCreatedEvent.getOrderId())
                        .build();

                commandGateway.sendAndWait(reserveProductCommand);
                LOGGER.info("Safely got here!");
                //toReserve.remove(item);
                reserved.add(item);
            }
        } catch (Exception ex) {
            // Compensating
            LOGGER.info(ex.getLocalizedMessage());
            LOGGER.info(ex.getMessage());
            LOGGER.info("Start compensating operation!");

//            for(CartItem rollbackProduct: reserved) {
//                RollbackProductCommand rollbackProductCommand = RollbackProductCommand.builder()
//                        .productId(rollbackProduct.getProductId())
//                        .quantity(rollbackProduct.getQuantity())
//                        .orderId(orderCreatedEvent.getOrderId())
//                        .build();
//
//                commandGateway.send(rollbackProductCommand);
//            }
//            LOGGER.info("Rollback product success!");
            cancelProductReservation(reserved, orderCreatedEvent.getOrderId());

            CancelOrderCommand cancelOrderCommand = new CancelOrderCommand(orderCreatedEvent.getOrderId());
            commandGateway.send(cancelOrderCommand);
            LOGGER.info("cancelOrderCommand sent!");
            SagaLifecycle.end();
            return;
        }

        // 2. Try to fetch payment detail
        PaymentDetail paymentDetail = null;

        try {
            LOGGER.info("Try to fetch payment detail!");
            FetchPaymentDetailQuery fetchPaymentDetailQuery =
                    new FetchPaymentDetailQuery(orderCreatedEvent.getPaymentId());

            paymentDetail = queryGateway.query(fetchPaymentDetailQuery,
                                                ResponseTypes.instanceOf(PaymentDetail.class)).join();
        } catch (Exception ex) {
            // Start compensating transaction
            LOGGER.error(ex.getMessage());
            LOGGER.info("Fail to fetch payment detail, start compensating transaction!");
            cancelProductReservation(orderCreatedEvent.getCartItems(), orderCreatedEvent.getOrderId());
            rejectOrder(orderCreatedEvent.getOrderId());
            return;
        }

        if (paymentDetail == null) {
            // Start compensating transaction
            LOGGER.info("Fetched payment detail, but the payment is not exist, start compensating transaction!");
            cancelProductReservation(orderCreatedEvent.getCartItems(), orderCreatedEvent.getOrderId());
            rejectOrder(orderCreatedEvent.getOrderId());
            return;
        }

        LOGGER.info("Successfully fetched payment detail! The payment Id is: " + paymentDetail.getId() +
                ". Start processing payment!");

        // 3. Try to process payment
        ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
                .recordId(UUID.randomUUID().toString())
                .orderId(orderCreatedEvent.getOrderId())
                .paymentId(orderCreatedEvent.getPaymentId())
                .paymentDetail(paymentDetail)
                .build();

        String result = null;

        try {
            result = commandGateway.sendAndWait(processPaymentCommand, 10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            // start compensating transaction
            LOGGER.error(ex.getMessage());
            cancelProductReservation(orderCreatedEvent.getCartItems(), orderCreatedEvent.getOrderId());
            rejectOrder(orderCreatedEvent.getOrderId());
            return;
        }

        if (result == null) {
            // start compensating transaction
            LOGGER.info("The ProcessPaymentCommand resulted in null, Initiating a compensating transaction!");
            cancelProductReservation(orderCreatedEvent.getCartItems(), orderCreatedEvent.getOrderId());
            rejectOrder(orderCreatedEvent.getOrderId());
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(PaymentProcessedEvent paymentProcessedEvent) {
        // Send an ApproveOrderCommand
        ApproveOrderCommand approveOrderCommand = new ApproveOrderCommand(paymentProcessedEvent.getOrderId());
        LOGGER.info("Sent approveOrderCommand!");
        commandGateway.send(approveOrderCommand);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderApprovedEvent orderApprovedEvent) {
        LOGGER.info("Order is approved. Order Saga is complete for orderId: " + orderApprovedEvent.getOrderId());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderRejectedEvent orderRejectedEvent) {
        LOGGER.info("Order is rejected. Failed to process payment " +
                "Order : " + orderRejectedEvent.getOrderId());
    }

    private void cancelProductReservation(Set<CartItem> cartItems, String theOrderId) {
        LOGGER.info("Start rollbackProductReservation!");
        for(CartItem item: cartItems) {

            RollbackProductCommand rollbackProductCommand = RollbackProductCommand.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .orderId(theOrderId)
                    .build();
            commandGateway.send(rollbackProductCommand);
        }
        LOGGER.info("Rollback product success!");
    }

    private void rejectOrder(String rejectOrderId) {
        RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(rejectOrderId);
        commandGateway.send(rejectOrderCommand);
        LOGGER.info("rejectOrderCommand sent!");
    }
}
