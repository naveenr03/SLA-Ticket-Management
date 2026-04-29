package com.naveen.slaticket.ticket.dto;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        Long createdById,
        Long assignedToId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}