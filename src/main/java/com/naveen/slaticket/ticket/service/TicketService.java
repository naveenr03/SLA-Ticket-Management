package com.naveen.slaticket.ticket.service;

import com.naveen.slaticket.ticket.dto.CreateTicketRequest;
import com.naveen.slaticket.ticket.dto.PagedResponse;
import com.naveen.slaticket.ticket.dto.TicketResponse;
import com.naveen.slaticket.ticket.entity.Ticket;
import com.naveen.slaticket.ticket.entity.TicketStatus;
import com.naveen.slaticket.ticket.repository.TicketRepository;
import com.naveen.slaticket.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketResponse createTicket(CreateTicketRequest request, UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(user);

        Ticket saved = ticketRepository.save(ticket);
        return mapToResponse(saved);
    }

    public PagedResponse<TicketResponse> getMyTickets(UserDetails userDetails, int page, int size) {
        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var ticketPage = ticketRepository.findByCreatedBy(user, pageable);

        var content = ticketPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PagedResponse<>(
                content,
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalElements(),
                ticketPage.getTotalPages(),
                ticketPage.isLast()
        );
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus().name(),
                ticket.getPriority().name(),
                ticket.getCreatedBy() != null ? ticket.getCreatedBy().getId() : null,
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}