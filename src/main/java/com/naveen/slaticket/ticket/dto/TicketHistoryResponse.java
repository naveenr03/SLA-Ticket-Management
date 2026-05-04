package com.naveen.slaticket.ticket.dto;

import java.time.LocalDateTime;

public record TicketHistoryResponse(
        Long id,
        Long ticketId,
        String eventType,
        String details,
        Long performedById,
        String performedByName,
        LocalDateTime createdAt
) {}