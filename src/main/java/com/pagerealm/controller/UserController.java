package com.pagerealm.controller;

import com.pagerealm.dto.response.UserInfoResponse;
import com.pagerealm.entity.MembershipTier;
import com.pagerealm.entity.User;
import com.pagerealm.security.service.UserDetailsImpl;
import com.pagerealm.service.UserService;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //--------------------------------------------

    /**
     * @param userDetails
     * @return : UserInfoResponse(DTO)
     * @description : for會員詳情頁面
     */
    @GetMapping("/")
    public ResponseEntity<?> getUserDetails(@AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // 取出等級並轉為顯示字串
        String tierLabel = (user.getMembershipTier() == null)
                ? "會員等級1"
                : user.getMembershipTier().displayName();

        // 傳入 DTO（UserInfoResponse.membershipTier 是 String）
        UserInfoResponse response = new UserInfoResponse(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getGender(),
                user.getBirthdate(),
                tierLabel,
                user.getAvatarUrl(),
                user.isEnabled(),
                user.isTotpEnabled(),
                roles,
                user.getSignedMethod()
        );
        return ResponseEntity.ok().body(response);
    }

    /**
     * 需要將所有FK關聯一併刪除，此功能最後完成(可能需要額外認證totp or email)
     */
    @DeleteMapping("/")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .map(user -> {
                    userService.deleteByEmail(userDetails.getUsername());
                    return ResponseEntity.ok("帳號已刪除");
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("找不到用戶"));
    }

    /**
     * @param userDetails
     * @return : username
     * @description : for 取得當前登入使用者的username (導覽列、權限判斷、歡行訊息 + username 等等)
     */
    @GetMapping("/username")
    public String currentUsername(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return (userDetails != null) ? userDetails.getUserName() : " ";
    }

    //修改密碼
    @PutMapping("/password")
    public ResponseEntity<String> updatePassword(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                 @RequestParam String newPassword,
                                                 @RequestParam String checkPassword) {
        try {
            userService.updatePassword(userDetails.getId(), newPassword, checkPassword);
            return ResponseEntity.ok("Password updated");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    //修改生日
    @PutMapping("/birthdate")
    public ResponseEntity<String> updateBirthdate(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthdate) {
        userService.updateBirthdate(userDetails.getId(), birthdate);
        return ResponseEntity.ok("生日已更新");
    }

    //修改性別
    @PutMapping("/gender")
    public ResponseEntity<String> updateGender(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String gender) {
        userService.updateGender(userDetails.getId(), gender);
        return ResponseEntity.ok("性別已更新");
    }

    //修改username
    @PutMapping("/username")
    public ResponseEntity<String> updateUsername(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String username) {
        userService.updateUsername(userDetails.getId(), username);
        return ResponseEntity.ok("使用者名稱已更新");
    }

    //修改avatar
    @PostMapping(
            value = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            // 可選的大小檢查（與 Spring 上限互補）
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("檔案不可為空");
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("檔案超過 5MB 上限");
            }

            String url = userService.uploadAvatar(userDetails.getId(), file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }


//    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<String> uploadAvatar(@AuthenticationPrincipal UserDetailsImpl userDetails,
//                                               @RequestPart("file") MultipartFile file) {
//        String url = userService.uploadAvatar(userDetails.getId(), file);
//        return ResponseEntity.ok(url);
//    }


}