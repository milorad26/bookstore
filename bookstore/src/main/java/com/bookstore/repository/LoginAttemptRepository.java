package com.bookstore.repository;

import com.bookstore.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * Count failed login attempts for a username within a time window
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.username = :username " +
           "AND la.successful = false AND la.attemptTime >= :since")
    long countFailedAttemptsByUsernameSince(@Param("username") String username, 
                                            @Param("since") LocalDateTime since);

    /**
     * Count failed login attempts from an IP address within a time window
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.ipAddress = :ipAddress " +
           "AND la.successful = false AND la.attemptTime >= :since")
    long countFailedAttemptsByIpSince(@Param("ipAddress") String ipAddress, 
                                       @Param("since") LocalDateTime since);

    /**
     * Get recent login attempts for a username
     */
    List<LoginAttempt> findTop10ByUsernameOrderByAttemptTimeDesc(String username);

    /**
     * Get recent login attempts from an IP address
     */
    List<LoginAttempt> findTop10ByIpAddressOrderByAttemptTimeDesc(String ipAddress);

    /**
     * Get all failed attempts for a username within a time window
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.username = :username " +
           "AND la.successful = false AND la.attemptTime >= :since " +
           "ORDER BY la.attemptTime DESC")
    List<LoginAttempt> findFailedAttemptsByUsernameSince(@Param("username") String username, 
                                                          @Param("since") LocalDateTime since);

    /**
     * Delete old login attempts (for cleanup)
     */
    void deleteByAttemptTimeBefore(LocalDateTime cutoffTime);

    /**
     * Get suspicious IPs with multiple failed attempts from different usernames
     */
    @Query("SELECT la.ipAddress, COUNT(DISTINCT la.username) as distinctUsers FROM LoginAttempt la " +
           "WHERE la.successful = false AND la.attemptTime >= :since " +
           "GROUP BY la.ipAddress HAVING COUNT(DISTINCT la.username) >= :minUsers")
    List<Object[]> findSuspiciousIpsSince(@Param("since") LocalDateTime since, 
                                          @Param("minUsers") long minUsers);
}
