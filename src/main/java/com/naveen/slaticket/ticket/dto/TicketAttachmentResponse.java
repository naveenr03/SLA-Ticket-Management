package com.naveen.slaticket.ticket.dto;

import java.time.LocalDateTime;

public record TicketAttachmentResponse(
        Long id,
        Long ticketId,
        Long uploadedById,
        String uploadedByName,
        String originalFileName,
        String contentType,
        Long size,
        LocalDateTime createdAt
) {}