package com.naveen.slaticket.ticket.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTicketRequest(
        @NotNull(message = "Assignee user id is required")
        Long assigneeUserId
) {}