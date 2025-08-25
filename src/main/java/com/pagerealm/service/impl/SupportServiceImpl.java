package com.pagerealm.service.impl;

import com.pagerealm.entity.*;
import com.pagerealm.repository.SupportMessageRepository;
import com.pagerealm.repository.SupportTicketRepository;
import com.pagerealm.repository.UserRepository;
import com.pagerealm.service.SupportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SupportServiceImpl implements SupportService {

    private final SupportTicketRepository ticketRepo;
    private final SupportMessageRepository msgRepo;
    private final UserRepository userRepo;

    public SupportServiceImpl(SupportTicketRepository ticketRepo, SupportMessageRepository msgRepo, UserRepository userRepo) {
        this.ticketRepo = ticketRepo;
        this.msgRepo = msgRepo;
        this.userRepo = userRepo;
    }

    @Override
    @Transactional
    public SupportTicket createTicket(Long userId, String contactName, String contactEmail, String subject, String category, String firstMessageContent) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        AppTicketCategory cat;
        try { cat = AppTicketCategory.valueOf(category); } catch (Exception e) { throw new IllegalArgumentException("Invalid category"); }

        SupportTicket t = new SupportTicket();
        t.setUser(user);
        t.setContactName(contactName);
        t.setContactEmail(contactEmail);
        t.setCategory(cat);
        t.setSubject(subject);
        t.setStatus(AppTicketStatus.OPEN);
        LocalDateTime now = LocalDateTime.now();
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        t.setLastMessageAt(now);
        t = ticketRepo.save(t);

        SupportMessage m = new SupportMessage();
        m.setTicket(t);
        m.setSender(AppSenderType.USER);
        m.setContent(firstMessageContent);
        m.setSenderUserId(userId);
        m.setCreatedAt(LocalDateTime.now());
        msgRepo.save(m);

        // 加入至工單訊息串，讓本交易回傳詳情時可見
        t.getMessages().add(m);
        t.setLastMessageAt(m.getCreatedAt());
        t.setUpdatedAt(LocalDateTime.now());
        return ticketRepo.save(t);
    }

    @Override
    public List<SupportTicket> listTicketsByUser(Long userId) {
        return ticketRepo.findByUser_UserIdOrderByUpdatedAtDesc(userId);
    }

    @Override
    public Optional<SupportTicket> findUserTicket(Long userId, Long ticketId) {
        return ticketRepo.findByIdAndUser_UserId(ticketId, userId);
    }

    @Override
    @Transactional
    public SupportMessage addUserMessage(Long userId, Long ticketId, String content) {
        SupportTicket t = ticketRepo.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        if (!t.getUser().getUserId().equals(userId)) throw new RuntimeException("Forbidden");
        if (t.getStatus() == AppTicketStatus.CLOSED) throw new RuntimeException("Ticket is closed");

        SupportMessage m = new SupportMessage();
        m.setTicket(t);
        m.setSender(AppSenderType.USER);
        m.setContent(content);
        m.setSenderUserId(userId);
        m.setCreatedAt(LocalDateTime.now());
        msgRepo.save(m);

        // 若工單是 RESOLVED，再次留言轉回 IN_PROGRESS
        if (t.getStatus() == AppTicketStatus.RESOLVED) {
            t.setStatus(AppTicketStatus.IN_PROGRESS);
        }
        t.setLastMessageAt(m.getCreatedAt());
        t.setUpdatedAt(LocalDateTime.now());
        ticketRepo.save(t);
        return m;
    }

    // Admin
    @Override
    public List<SupportTicket> adminListTickets(AppTicketStatus status) {
        if (status != null) return ticketRepo.findByStatusOrderByUpdatedAtDesc(status);
        return ticketRepo.findAllByOrderByUpdatedAtDesc();
    }

    @Override
    public Optional<SupportTicket> adminFindTicket(Long ticketId) {
        return ticketRepo.findById(ticketId);
    }

    @Override
    @Transactional
    public SupportMessage adminReply(Long adminUserId, Long ticketId, String content, AppTicketStatus nextStatus) {
        SupportTicket t = ticketRepo.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        SupportMessage m = new SupportMessage();
        m.setTicket(t);
        m.setSender(AppSenderType.AGENT);
        m.setContent(content);
        m.setSenderUserId(adminUserId);
        m.setCreatedAt(LocalDateTime.now());
        msgRepo.save(m);

        // 預設：首次回覆或狀態為 OPEN -> IN_PROGRESS
        if (nextStatus != null) {
            t.setStatus(nextStatus);
        } else if (t.getStatus() == AppTicketStatus.OPEN) {
            t.setStatus(AppTicketStatus.IN_PROGRESS);
        }
        t.setLastMessageAt(m.getCreatedAt());
        t.setUpdatedAt(LocalDateTime.now());
        ticketRepo.save(t);
        return m;
    }

    @Override
    @Transactional
    public SupportTicket updateStatus(Long ticketId, AppTicketStatus status) {
        SupportTicket t = ticketRepo.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        t.setStatus(status);
        t.setUpdatedAt(LocalDateTime.now());
        return ticketRepo.save(t);
    }

    @Override
    public List<SupportTicket> listTicketsWithMessagesByUser(Long userId) {
        return ticketRepo.findAllWithMessagesByUserId(userId);
    }
}
