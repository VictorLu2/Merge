package com.pagerealm.service;

import com.pagerealm.entity.AppTicketStatus;
import com.pagerealm.entity.SupportMessage;
import com.pagerealm.entity.SupportTicket;

import java.util.List;
import java.util.Optional;

public interface SupportService {
    SupportTicket createTicket(Long userId, String contactName, String contactEmail, String subject, String category, String firstMessageContent);

    List<SupportTicket> listTicketsByUser(Long userId);

    List<SupportTicket> listTicketsWithMessagesByUser(Long userId);

    Optional<SupportTicket> findUserTicket(Long userId, Long ticketId);

    SupportMessage addUserMessage(Long userId, Long ticketId, String content);

    // Admin side
    List<SupportTicket> adminListTickets(AppTicketStatus status);

    Optional<SupportTicket> adminFindTicket(Long ticketId);

    SupportMessage adminReply(Long adminUserId, Long ticketId, String content, AppTicketStatus nextStatus);

    SupportTicket updateStatus(Long ticketId, AppTicketStatus status);
}
