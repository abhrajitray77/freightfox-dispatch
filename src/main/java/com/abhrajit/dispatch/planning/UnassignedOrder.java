package com.abhrajit.dispatch.planning;

import com.abhrajit.dispatch.domain.DeliveryOrder;

/** An order no vehicle could take, with the reason. */
public record UnassignedOrder(DeliveryOrder order, String reason) {
}
