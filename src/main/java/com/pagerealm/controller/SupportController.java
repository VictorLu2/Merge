package com.pagerealm.controller;

import com.pagerealm.dto.request.CreateMessageRequest;
import com.pagerealm.dto.request.CreateTicketRequest;
import com.pagerealm.dto.response.SupportMessageDTO;
import com.pagerealm.dto.response.SupportTicketDTO;
import com.pagerealm.dto.response.SupportTicketDetailDTO;
import com.pagerealm.entity.AppSenderType;
import com.pagerealm.entity.SupportMessage;
import com.pagerealm.entity.SupportTicket;
import com.pagerealm.service.SupportService;
import com.pagerealm.utils.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;
    private final AuthUtil authUtil;

    public SupportController(SupportService supportService, AuthUtil authUtil) {
        this.supportService = supportService;
        this.authUtil = authUtil;
    }

    @PostMapping("/tickets")
    public ResponseEntity<SupportTicketDetailDTO> create(@Valid @RequestBody CreateTicketRequest req) {
        Long userId = authUtil.LoggedInUserId();
        SupportTicket t = supportService.createTicket(
                userId,
                req.getContactName(),
                req.getContactEmail(),
                req.getSubject(),
                req.getCategory().name(),
                req.getContent()
        );
        return ResponseEntity.ok(toDetailDTO(t));
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<SupportTicketDTO>> myTickets() {
        Long userId = authUtil.LoggedInUserId();
        List<SupportTicketDTO> list = supportService.listTicketsByUser(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<SupportTicketDetailDTO> myTicket(@PathVariable Long id) {
        Long userId = authUtil.LoggedInUserId();
        SupportTicket t = supportService.findUserTicket(userId, id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        return ResponseEntity.ok(toDetailDTO(t));
    }

    @PostMapping("/tickets/{id}/messages")
    public ResponseEntity<SupportMessageDTO> addMessage(@PathVariable Long id, @Valid @RequestBody CreateMessageRequest req) {
        Long userId = authUtil.LoggedInUserId();
        SupportMessage m = supportService.addUserMessage(userId, id, req.getContent());
        return ResponseEntity.ok(toMsgDTO(m));
    }

    // 一次取回所有工單（含訊息）
    @GetMapping("/tickets/details")
    public ResponseEntity<List<SupportTicketDetailDTO>> myTicketsWithMessages() {
        Long userId = authUtil.LoggedInUserId();
        List<SupportTicketDetailDTO> list = supportService.listTicketsWithMessagesByUser(userId)
                .stream().map(this::toDetailDTO).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private SupportTicketDTO toDTO(SupportTicket t) {
        return new SupportTicketDTO(
                t.getId(),
                t.getSubject(),
                t.getCategory(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getLastMessageAt()
        );
    }

    private SupportTicketDetailDTO toDetailDTO(SupportTicket t) {
        return new SupportTicketDetailDTO(
                t.getId(),
                t.getContactName(),
                t.getContactEmail(),
                t.getSubject(),
                t.getCategory(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getLastMessageAt(),
                t.getMessages().stream().map(this::toMsgDTO).collect(Collectors.toList())
        );
    }

    private SupportMessageDTO toMsgDTO(SupportMessage m) {
        return new SupportMessageDTO(m.getId(), m.getSender(), m.getContent(), m.getCreatedAt());
    }
}
