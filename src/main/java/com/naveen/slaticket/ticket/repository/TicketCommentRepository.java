package com.naveen.slaticket.ticket.repository;

import com.naveen.slaticket.ticket.entity.TicketComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    @EntityGraph(attributePaths = {"author", "ticket"})
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}