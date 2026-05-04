package com.naveen.slaticket.ticket.repository;

import com.naveen.slaticket.ticket.entity.Ticket;
import com.naveen.slaticket.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Page<Ticket> findByCreatedBy(User createdBy, Pageable pageable);
    Page<Ticket> findByAssignedTo(User assignedTo, Pageable pageable);
}