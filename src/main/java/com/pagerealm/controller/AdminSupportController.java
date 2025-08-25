package com.pagerealm.controller;

import com.pagerealm.dto.request.AdminReplyRequest;
import com.pagerealm.dto.response.SupportMessageDTO;
import com.pagerealm.dto.response.SupportTicketDTO;
import com.pagerealm.dto.response.SupportTicketDetailDTO;
import com.pagerealm.entity.AppTicketStatus;
import com.pagerealm.entity.SupportMessage;
import com.pagerealm.entity.SupportTicket;
import com.pagerealm.service.SupportService;
import com.pagerealm.utils.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/support")
public class AdminSupportController {

    private final SupportService supportService;
    private final AuthUtil authUtil;

    public AdminSupportController(SupportService supportService, AuthUtil authUtil) {
        this.supportService = supportService;
        this.authUtil = authUtil;
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<SupportTicketDTO>> list(@RequestParam(value = "status", required = false) AppTicketStatus status) {
        List<SupportTicketDTO> list = supportService.adminListTickets(status).stream()
                .map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<SupportTicketDetailDTO> detail(@PathVariable Long id) {
        SupportTicket t = supportService.adminFindTicket(id).orElseThrow(() -> new RuntimeException("Ticket not found"));
        return ResponseEntity.ok(toDetailDTO(t));
    }

    @PostMapping("/tickets/{id}/reply")
    public ResponseEntity<SupportMessageDTO> reply(@PathVariable Long id, @RequestBody AdminReplyRequest req) {
        Long adminUserId = authUtil.LoggedInUserId();
        SupportMessage m = supportService.adminReply(adminUserId, id, req.getContent(), req.getNextStatus());
        return ResponseEntity.ok(toMsgDTO(m));
    }

    @PostMapping("/tickets/{id}/status")
    public ResponseEntity<SupportTicketDTO> updateStatus(@PathVariable Long id, @RequestParam AppTicketStatus status) {
        SupportTicket t = supportService.updateStatus(id, status);
        return ResponseEntity.ok(toDTO(t));
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
