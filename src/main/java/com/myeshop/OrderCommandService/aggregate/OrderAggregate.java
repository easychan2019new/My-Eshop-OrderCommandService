package com.myeshop.OrderCommandService.aggregate;

import com.myeshop.Core.order.command.CancelOrderCommand;
import com.myeshop.Core.order.event.OrderApprovedEvent;
import com.myeshop.Core.order.event.OrderCanceledEvent;
import com.myeshop.Core.order.event.OrderCreatedEvent;
import com.myeshop.Core.order.event.OrderRejectedEvent;
import com.myeshop.Core.order.rest.CartItem;
import com.myeshop.OrderCommandService.command.ApproveOrderCommand;
import com.myeshop.OrderCommandService.command.CreateOrderCommand;
import com.myeshop.OrderCommandService.command.RejectOrderCommand;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

@Aggregate
@NoArgsConstructor
public class OrderAggregate {

    @AggregateIdentifier
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

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderAggregate.class);

    @CommandHandler
    public OrderAggregate(CreateOrderCommand createOrderCommand) {
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent();
        BeanUtils.copyProperties(createOrderCommand, orderCreatedEvent);
        AggregateLifecycle.apply(orderCreatedEvent);
        LOGGER.info("Publish orderCreatedEvent!");
    }

    @EventSourcingHandler
    public void on(OrderCreatedEvent orderCreatedEvent) {
        this.orderId = orderCreatedEvent.getOrderId();
        this.totalPrice = orderCreatedEvent.getTotalPrice();
        this.totalQuantity = orderCreatedEvent.getTotalQuantity();
        this.status = orderCreatedEvent.getStatus();
        this.dateCreated = orderCreatedEvent.getDateCreated();
        this.lastUpdated = orderCreatedEvent.getLastUpdated();
        this.customerEmail = orderCreatedEvent.getCustomerEmail();
        this.addressId = orderCreatedEvent.getAddressId();
        this.paymentId = orderCreatedEvent.getPaymentId();
        this.cartItems = orderCreatedEvent.getCartItems();

        LOGGER.info("Persist orderCreatedEvent");
    }

    @CommandHandler
    public void handle(CancelOrderCommand cancelOrderCommand) {
        OrderCanceledEvent orderCanceledEvent = new OrderCanceledEvent();
        orderCanceledEvent.setOrderId(cancelOrderCommand.getOrderId());
        orderCanceledEvent.setStatus("Canceled (Product out of Stock)");
        AggregateLifecycle.apply(orderCanceledEvent);
        LOGGER.info("Publish orderCanceledEvent!");
    }

    @EventSourcingHandler
    public void on(OrderCanceledEvent orderCanceledEvent) {
        this.status = orderCanceledEvent.getStatus();
        LOGGER.info("Persist orderCanceledEvent");
    }

    @CommandHandler
    public void handle(ApproveOrderCommand approveOrderCommand) {
        // Create and publish the OrderApprovedEvent
        LOGGER.info("Handle ApproveOrderCommand!");
        OrderApprovedEvent orderApprovedEvent = new OrderApprovedEvent();
        orderApprovedEvent.setOrderId(approveOrderCommand.getOrderId());
        orderApprovedEvent.setStatus("Approved");
        AggregateLifecycle.apply(orderApprovedEvent);
    }

    @EventSourcingHandler
    public void on(OrderApprovedEvent orderApprovedEvent) {
        this.status = orderApprovedEvent.getStatus();
        LOGGER.info("Persist orderApprovedEvent!");
    }

    @CommandHandler
    public void handle(RejectOrderCommand rejectOrderCommand) {
        // Create and publish the RejectOrderCommand
        LOGGER.info("Handle RejectOrderCommand!");
        OrderRejectedEvent orderRejectedEvent = new OrderRejectedEvent();
        orderRejectedEvent.setOrderId(rejectOrderCommand.getOrderId());
        orderRejectedEvent.setStatus("Rejected (Fail to process payment)");
        AggregateLifecycle.apply(orderRejectedEvent);
    }

    @EventSourcingHandler
    public void on(OrderRejectedEvent orderRejectedEvent) {
        this.status = orderRejectedEvent.getStatus();
        LOGGER.info("Persist orderRejectedEvent!");
    }
}
