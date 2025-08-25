package com.pagerealm.repository;

import com.pagerealm.entity.AppTicketStatus;
import com.pagerealm.entity.SupportTicket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUser_UserIdOrderByUpdatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "messages")
    Optional<SupportTicket> findByIdAndUser_UserId(Long id, Long userId);

    @Query("select distinct t from SupportTicket t left join fetch t.messages where t.user.userId = :userId order by t.updatedAt desc")
    List<SupportTicket> findAllWithMessagesByUserId(@Param("userId") Long userId);

    List<SupportTicket> findByStatusOrderByUpdatedAtDesc(AppTicketStatus status);

    List<SupportTicket> findAllByOrderByUpdatedAtDesc();

    @Override
    @EntityGraph(attributePaths = "messages")
    Optional<SupportTicket> findById(Long id);
}
