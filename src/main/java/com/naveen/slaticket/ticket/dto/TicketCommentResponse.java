package com.naveen.slaticket.ticket.dto;

import java.time.LocalDateTime;

public record TicketCommentResponse(
        Long id,
        Long ticketId,
        Long authorId,
        String authorName,
        String message,
        LocalDateTime createdAt
) {}