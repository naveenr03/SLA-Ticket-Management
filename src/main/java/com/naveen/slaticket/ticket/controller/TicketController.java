package com.naveen.slaticket.ticket.controller;

import com.naveen.slaticket.ticket.dto.*;
import com.naveen.slaticket.ticket.entity.TicketAttachment;
import com.naveen.slaticket.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @GetMapping("/assigned")
    public PagedResponse<TicketResponse> getAssignedTickets(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ticketService.getAssignedTickets(userDetails, page, size);
    }

    @PatchMapping("/{ticketId}/assign")
    public TicketResponse assignTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody AssignTicketRequest request
    ) {
        return ticketService.assignTicket(ticketId, request);
    }

    @PatchMapping("/{ticketId}/status")
    public TicketResponse updateStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ticketService.updateTicketStatus(ticketId, request, userDetails);
    }

    @PostMapping("/{ticketId}/comments")
    public TicketCommentResponse addComment(
            @PathVariable Long ticketId,
            @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ticketService.addComment(ticketId, request, userDetails);
    }

    @GetMapping("/{ticketId}/comments")
    public List<TicketCommentResponse> getComments(@PathVariable Long ticketId) {
        return ticketService.getComments(ticketId);
    }

    @PostMapping(value = "/{ticketId}/attachments",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TicketAttachmentResponse uploadAttachment(
            @PathVariable Long ticketId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails
            ) {

        return ticketService.uploadAttachment(ticketId, file, userDetails);
    }

    @GetMapping("/{ticketId}/attachments")
    public List<TicketAttachmentResponse> getAttachments
            (@PathVariable Long ticketId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ticketService.getAttachments(ticketId,userDetails);
    }

    @GetMapping("/{ticketId}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TicketAttachment attachment = ticketService.getAttachmentMetadata(ticketId, attachmentId, userDetails);
        Resource resource = ticketService.downloadAttachment(ticketId, attachmentId, userDetails);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        attachment.getContentType() != null ? attachment.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE
                ))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getOriginalFileName() + "\"")
                .body(resource);
    }


}