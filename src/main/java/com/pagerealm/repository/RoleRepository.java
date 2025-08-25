package com.pagerealm.repository;

import com.pagerealm.entity.AppRole;
import com.pagerealm.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role,Long> {

    Optional<Role> findByRoleName(AppRole appRole);
}
