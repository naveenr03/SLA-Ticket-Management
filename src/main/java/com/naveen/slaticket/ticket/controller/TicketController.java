package com.naveen.slaticket.ticket.controller;

import com.naveen.slaticket.ticket.dto.CreateTicketRequest;
import com.naveen.slaticket.ticket.dto.PagedResponse;
import com.naveen.slaticket.ticket.dto.TicketResponse;
import com.naveen.slaticket.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ticketService.createTicket(request, userDetails);
    }

    @GetMapping("/me")
    public PagedResponse<TicketResponse> getMyTickets(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ticketService.getMyTickets(userDetails, page, size);
    }
}