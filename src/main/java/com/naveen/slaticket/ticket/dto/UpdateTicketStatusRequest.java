package com.naveen.slaticket.ticket.dto;

import com.naveen.slaticket.ticket.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(
        @NotNull(message = "Status is required")
        TicketStatus status
) {}