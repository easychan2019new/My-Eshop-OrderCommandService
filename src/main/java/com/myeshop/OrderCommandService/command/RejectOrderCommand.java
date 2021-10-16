package com.myeshop.OrderCommandService.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
public class RejectOrderCommand {

    @TargetAggregateIdentifier
    private final String orderId;
}
