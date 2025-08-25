package com.pagerealm.controller;

import com.pagerealm.dto.UserDTO;
import com.pagerealm.entity.Role;
import com.pagerealm.entity.User;
import com.pagerealm.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminController {

    UserService userService;

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

//    @PutMapping("/update-lock-status")
//    public ResponseEntity<String> updateAccountLockStatus(@RequestParam Long userId, @RequestParam boolean lock) {
//        userService.updateAccountLockStatus(userId, lock);
//        return ResponseEntity.ok("Account lock status updated");
//    }

    @GetMapping("/roles")
    public List<Role> getAllRoles() {
        return userService.getAllRoles();
    }


    @PutMapping("/update-enabled-status")
    public ResponseEntity<String> updateAccountEnabledStatus(@RequestParam Long userId, @RequestParam boolean enabled) {
        userService.updateAccountEnabledStatus(userId, enabled);
        return ResponseEntity.ok("Account enabled status updated");
    }

}
