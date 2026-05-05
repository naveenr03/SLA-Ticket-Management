package com.naveen.slaticket.ticket.controller;

import com.naveen.slaticket.common.dto.ApiErrorResponse;
import com.naveen.slaticket.ticket.dto.*;
import com.naveen.slaticket.ticket.entity.TicketAttachment;
import com.naveen.slaticket.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create ticket",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Ticket created",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public TicketResponse createTicket(
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ticketService.createTicket(request, userDetails);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get my tickets",
            description = "Paginated list of tickets created by the current user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paged tickets",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public PagedResponse<TicketResponse> getMyTickets(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ticketService.getMyTickets(userDetails, page, size);
    }

    @GetMapping("/assigned")
    @Operation(
            summary = "Get assigned tickets",
            description = "Paginated list of tickets assigned to the current user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paged tickets",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public PagedResponse<TicketResponse> getAssignedTickets(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ticketService.getAssignedTickets(userDetails, page, size);
    }

    @PatchMapping("/{ticketId}/assign")
    @Operation(
            summary = "Assign ticket",
            description = "Assigns a ticket to an agent. Requires MANAGER or ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ticket updated",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid assignee or request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden (e.g. not manager/admin)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket or user not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public TicketResponse assignTicket(
            @PathVariable Long ticketId,
            @Valid @org.springframework.web.bind.annotation.RequestBody AssignTicketRequest request
    ) {
        return ticketService.assignTicket(ticketId, request);
    }

    @PatchMapping("/{ticketId}/status")
    @Operation(
            summary = "Update ticket status",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status updated",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid transition or not allowed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public TicketResponse updateStatus(
            @PathVariable Long ticketId,
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateTicketStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ticketService.updateTicketStatus(ticketId, request, userDetails);
    }

    @PostMapping("/{ticketId}/comments")
    @Operation(
            summary = "Add comment",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Comment created",
                    content = @Content(schema = @Schema(implementation = TicketCommentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or not allowed to comment",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public TicketCommentResponse addComment(
            @PathVariable Long ticketId,
            @Valid @org.springframework.web.bind.annotation.RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ticketService.addComment(ticketId, request, userDetails);
    }

    @GetMapping("/{ticketId}/comments")
    @Operation(
            summary = "Get comments",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of comments",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TicketCommentResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public List<TicketCommentResponse> getComments(@PathVariable Long ticketId) {
        return ticketService.getComments(ticketId);
    }

    @GetMapping("/{ticketId}/history")
    @Operation(
            summary = "Get ticket history",
            description = "Audit-style history entries for the ticket",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "History entries",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TicketHistoryResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public List<TicketHistoryResponse> getHistory(@PathVariable Long ticketId) {
        return ticketService.getHistory(ticketId);
    }

    @PostMapping(value = "/{ticketId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Upload attachment",
            description = "Uploads a file attachment for a ticket",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RequestBody(
            required = true,
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Attachment stored",
                    content = @Content(schema = @Schema(implementation = TicketAttachmentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid file or access denied",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public TicketAttachmentResponse uploadAttachment(
            @PathVariable Long ticketId,
            @Parameter(
                    description = "File to upload",
                    required = true,
                    schema = @Schema(type = "string", format = "binary")
            )
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ticketService.uploadAttachment(ticketId, file, userDetails);
    }

    @GetMapping("/{ticketId}/attachments")
    @Operation(
            summary = "List attachments",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Attachments for the ticket",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TicketAttachmentResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public List<TicketAttachmentResponse> getAttachments(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ticketService.getAttachments(ticketId, userDetails);
    }

    @GetMapping("/{ticketId}/attachments/{attachmentId}/download")
    @Operation(
            summary = "Download attachment",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "File bytes (Content-Type from stored metadata)",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket or attachment not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
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
