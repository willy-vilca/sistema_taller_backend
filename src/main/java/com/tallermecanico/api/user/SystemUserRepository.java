package com.tallermecanico.api.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemUserRepository extends JpaRepository<SystemUser, UUID> {
    @EntityGraph(attributePaths = "role")
    Optional<SystemUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<SystemUser> findAllByActiveTrueOrderByFullNameAsc();

    List<SystemUser> findAllByOrderByFullNameAsc();

    @EntityGraph(attributePaths = "role")
    @Query("""
            select user from SystemUser user
            join user.role role
            where user.active = true
              and role.name = com.tallermecanico.api.user.RoleName.ADMIN
              and user.scheduleNotificationsEnabled = true
              and user.email is not null
            order by user.fullName asc
            """)
    List<SystemUser> findActiveAdminsWithScheduledServiceNotifications();
}
