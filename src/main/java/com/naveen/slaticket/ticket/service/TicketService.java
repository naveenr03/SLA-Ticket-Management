package com.naveen.slaticket.ticket.service;

import com.naveen.slaticket.common.exception.BadRequestException;
import com.naveen.slaticket.common.exception.ResourceNotFoundException;
import com.naveen.slaticket.ticket.dto.*;
import com.naveen.slaticket.ticket.entity.*;
import com.naveen.slaticket.ticket.repository.TicketAttachmentRepository;
import com.naveen.slaticket.ticket.repository.TicketCommentRepository;
import com.naveen.slaticket.ticket.repository.TicketHistoryRepository;
import com.naveen.slaticket.ticket.repository.TicketRepository;
import com.naveen.slaticket.user.entity.User;
import com.naveen.slaticket.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    private final TicketCommentRepository ticketCommentRepository;
    private final TicketHistoryRepository ticketHistoryRepository;

    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final FileStorageService fileStorageService;

    public TicketResponse createTicket(CreateTicketRequest request, UserDetails userDetails) {
        var user = getUserByEmail(userDetails.getUsername());

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
        var user = getUserByEmail(userDetails.getUsername());
        var pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        var ticketPage = ticketRepository.findByCreatedBy(user, pageable);

        return buildPagedResponse(ticketPage);
    }

    public PagedResponse<TicketResponse> getAssignedTickets(UserDetails userDetails, int page, int size) {
        var user = getUserByEmail(userDetails.getUsername());
        var pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        var ticketPage = ticketRepository.findByAssignedTo(user, pageable);

        return buildPagedResponse(ticketPage);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public TicketResponse assignTicket(Long ticketId, AssignTicketRequest request) {
        Ticket ticket = getTicketById(ticketId);
        User assignee = getUserById(request.assigneeUserId());

        if (assignee.getRole() == null || !assignee.getRole().name().equals("ROLE_AGENT")) {
            throw new BadRequestException("Ticket can only be assigned to an agent");
        }

        ticket.setAssignedTo(assignee);

        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        Ticket saved = ticketRepository.save(ticket);
        return mapToResponse(saved);
    }

    public TicketResponse updateTicketStatus(Long ticketId, UpdateTicketStatusRequest request, UserDetails userDetails) {
        Ticket ticket = getTicketById(ticketId);
        User currentUser = getUserByEmail(userDetails.getUsername());

        boolean isPrivileged = currentUser.getRole().name().equals("ROLE_MANAGER")
                || currentUser.getRole().name().equals("ROLE_ADMIN");

        boolean isAssignedAgent = ticket.getAssignedTo() != null
                && ticket.getAssignedTo().getId().equals(currentUser.getId());

        if (!isPrivileged && !isAssignedAgent) {
            throw new BadRequestException("You are not allowed to update this ticket");
        }

        if (!isValidTransition(ticket.getStatus(), request.status())) {
            throw new BadRequestException(
                    "Invalid status transition from " + ticket.getStatus() + " to " + request.status()
            );
        }

        ticket.setStatus(request.status());
        Ticket saved = ticketRepository.save(ticket);
        return mapToResponse(saved);
    }

    private boolean isValidTransition(TicketStatus currentStatus, TicketStatus newStatus) {
        return switch (currentStatus) {
            case OPEN -> newStatus == TicketStatus.IN_PROGRESS || newStatus == TicketStatus.CLOSED;
            case IN_PROGRESS -> newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.OPEN;
            case RESOLVED -> newStatus == TicketStatus.CLOSED || newStatus == TicketStatus.IN_PROGRESS;
            case CLOSED -> false;
        };
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    private PagedResponse<TicketResponse> buildPagedResponse(org.springframework.data.domain.Page<Ticket> ticketPage) {
        var content = ticketPage.getContent().stream().map(this::mapToResponse).toList();

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

    public TicketCommentResponse addComment(Long ticketId,AddCommentRequest request, UserDetails userDetails) {

        Ticket ticket = getTicketById(ticketId);
        User user = getUserByEmail(userDetails.getUsername());

        boolean isPrivileged = user.getRole().name().equals("ROLE_MANAGER") ||
                user.getRole().name().equals("ROLE_ADMIN");

        boolean isOwner = ticket.getCreatedBy().getId().equals(user.getId());
        boolean isAssignee = ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(user.getId());

        if (!isPrivileged && !isOwner && !isAssignee) {
            throw new BadRequestException("You are not allowed to comment on this ticket");
        }

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(user);
        comment.setMessage(request.message().trim());

        TicketComment saved = ticketCommentRepository.save(comment);

        saveHistory(
                ticket.getId(),
                "COMMENT_ADDED",
                "Comment Added by " +
                        user.getEmail(),user);

        return new TicketCommentResponse(
                saved.getId(),
                ticket.getId(),
                user.getId(),
                user.getName(),
                request.message(),
                saved.getCreatedAt()
        );
    }

    private void saveHistory(Long ticketId, String eventType, String details, User user) {
        TicketHistory history = new TicketHistory();
        history.setTicketId(ticketId);
        history.setEventType(eventType);
        history.setDetails(details);
        history.setPerformedBy(user);
        ticketHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getComments(Long ticketId) {
        Ticket ticket = getTicketById(ticketId);

        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(comment -> new TicketCommentResponse(
                        comment.getId(),
                        comment.getTicket().getId(),
                        comment.getAuthor().getId(),
                        comment.getAuthor().getName(),
                        comment.getMessage(),
                        comment.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> getHistory(Long ticketId) {
        Ticket ticket = getTicketById(ticketId);

        return ticketHistoryRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(history -> new TicketHistoryResponse(
                        history.getId(),
                        history.getTicketId(),
                        history.getEventType(),
                        history.getDetails(),
                        history.getPerformedBy().getId(),
                        history.getPerformedBy().getName(),
                        history.getCreatedAt()
                ))
                .toList();
    }


    @Transactional
    public TicketAttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, UserDetails userDetails) {
        Ticket ticket = getTicketById(ticketId);
        User user = getUserByEmail(userDetails.getUsername());

        validateTicketAccess(ticket, user);

        String storedFileName = fileStorageService.storeFile(file);

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicketId(ticket.getId());
        attachment.setUploadedBy(user);
        attachment.setOriginalFileName(file.getOriginalFilename());
        attachment.setStoredFileName(storedFileName);
        attachment.setContentType(file.getContentType());
        attachment.setSize(file.getSize());

        TicketAttachment saved = ticketAttachmentRepository.save(attachment);

        saveHistory(ticket.getId(), "ATTACHMENT_UPLOADED", "Attachment uploaded: " + saved.getOriginalFileName(), user);

        return new TicketAttachmentResponse(
                saved.getId(),
                saved.getTicketId(),
                saved.getUploadedBy().getId(),
                saved.getUploadedBy().getName(),
                saved.getOriginalFileName(),
                saved.getContentType(),
                saved.getSize(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<TicketAttachmentResponse> getAttachments(Long ticketId, UserDetails userDetails) {
        Ticket ticket = getTicketById(ticketId);
        User user = getUserByEmail(userDetails.getUsername());

        validateTicketAccess(ticket, user);

        return ticketAttachmentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(attachment -> new TicketAttachmentResponse(
                        attachment.getId(),
                        attachment.getTicketId(),
                        attachment.getUploadedBy().getId(),
                        attachment.getUploadedBy().getName(),
                        attachment.getOriginalFileName(),
                        attachment.getContentType(),
                        attachment.getSize(),
                        attachment.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource downloadAttachment(Long ticketId, Long attachmentId, UserDetails userDetails) {
        Ticket ticket = getTicketById(ticketId);
        User user = getUserByEmail(userDetails.getUsername());

        validateTicketAccess(ticket, user);

        TicketAttachment attachment = ticketAttachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        return fileStorageService.loadAsResource(attachment.getStoredFileName());
    }

    @Transactional(readOnly = true)
    public TicketAttachment getAttachmentMetadata(Long ticketId, Long attachmentId, UserDetails userDetails) {
        Ticket ticket = getTicketById(ticketId);
        User user = getUserByEmail(userDetails.getUsername());

        validateTicketAccess(ticket, user);

        return ticketAttachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
    }

    private void validateTicketAccess(Ticket ticket, User user) {
        boolean isPrivileged = user.getRole().name().equals("ROLE_MANAGER")
                || user.getRole().name().equals("ROLE_ADMIN");

        boolean isOwner = ticket.getCreatedBy() != null
                && ticket.getCreatedBy().getId().equals(user.getId());

        boolean isAssignee = ticket.getAssignedTo() != null
                && ticket.getAssignedTo().getId().equals(user.getId());

        if (!isPrivileged && !isOwner && !isAssignee) {
            throw new BadRequestException("You are not allowed to access this ticket");
        }
    }



}