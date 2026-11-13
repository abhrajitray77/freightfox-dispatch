package com.abhrajit.dispatch.dto;

import com.abhrajit.dispatch.domain.DeliveryOrder;
import com.abhrajit.dispatch.domain.Priority;

public record OrderView(String orderId, double latitude, double longitude, String address, double packageWeight, Priority priority) {

    public static OrderView of(DeliveryOrder o) {
        return new OrderView(o.getOrderId(), o.getLatitude(), o.getLongitude(), o.getAddress(), o.getPackageWeight(), o.getPriority());
    }
}
