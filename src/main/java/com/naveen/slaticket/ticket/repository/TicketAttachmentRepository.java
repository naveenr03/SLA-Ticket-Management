package com.naveen.slaticket.ticket.repository;

import com.naveen.slaticket.ticket.entity.TicketAttachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    @EntityGraph(attributePaths = {"uploadedBy"})
    List<TicketAttachment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    Optional<TicketAttachment> findByIdAndTicketId(Long id, Long ticketId);
}
