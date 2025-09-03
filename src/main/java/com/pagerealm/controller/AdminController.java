package com.pagerealm.controller;

import com.pagerealm.dto.UserDTO;
import com.pagerealm.dto.request.SignupRequest;
import com.pagerealm.dto.response.MessageResponse;
import com.pagerealm.entity.AppRole;
import com.pagerealm.entity.MembershipTier;
import com.pagerealm.entity.Role;
import com.pagerealm.entity.User;
import com.pagerealm.repository.RoleRepository;
import com.pagerealm.repository.UserRepository;
import com.pagerealm.service.UserService;
import jakarta.validation.Valid;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminController {

    UserService userService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    RoleRepository roleRepository;

    public AdminController(UserService userService) {
        this.userService = userService;
    }
    //----------------------------------

    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/getusers")
    public ResponseEntity<List<User>> getAllUsers() {
        return new ResponseEntity<List<User>>(userService.getAllUsers(), HttpStatus.OK);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return new ResponseEntity<>(userService.getUserById(id), HttpStatus.OK);
    }


    @GetMapping("/roles")
    public List<Role> getAllRoles() {
        return userService.getAllRoles();
    }


    @PutMapping("/update-enabled-status")
    public ResponseEntity<String> updateAccountEnabledStatus(@RequestParam Long userId, @RequestParam boolean enabled) {
        userService.updateAccountEnabledStatus(userId, enabled);
        return ResponseEntity.ok("Account enabled status updated");
    }

    //--------------------------------------------------------------

    @PostMapping("/add-user")
    public ResponseEntity<?> addUserOrAdmin(@Valid @RequestBody SignupRequest signupRequest) {

        //判斷Admin輸入的username和email是否已存在
        if (userRepository.existsByUserName(signupRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: 此使用者名稱已有人使用"));
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: 此Email已註冊過"));
        }

        //創建新的使用者帳號(User | Admin)
        User user = new User(
                signupRequest.getUsername(),
                signupRequest.getEmail(),
                passwordEncoder.encode(signupRequest.getPassword()),
                signupRequest.getGender(),
                signupRequest.getBirthdate()
        );

        user.setEnabled(true);
        user.setMembershipTier(MembershipTier.LV1);
        user.setTotpEnabled(false);
        user.setSignedMethod("Admin");
        user.setAvatarUrl("/images/Avatar_default.png");
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);

        // 角色判斷：user | admin
        Set<String> strRoles = signupRequest.getRole();

        String roleStr = (strRoles != null && !strRoles.isEmpty()) ? strRoles.iterator().next() : "user";

        AppRole appRole = "admin".equals(roleStr) ? AppRole.ROLE_ADMIN : AppRole.ROLE_USER;

        Role role = roleRepository.findByRoleName(appRole)
                .orElseThrow(() -> new RuntimeException("Error: Role not found"));

        user.setRole(role);

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("新增完成"));
    }


}
