package com.naveen.slaticket.ticket.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(
        @NotBlank(message = "Message cannot be blank")
        @Size(max = 5000, message = "Message must be less than 5000 characters")
        String message) {


}
