package com.pagerealm.utils;

import com.pagerealm.entity.User;
import com.pagerealm.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    UserRepository userRepository;

    public AuthUtil(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    //---------------
    /**
     * @description : 使用Spring Security的SecurityContextHolder 取得登入中使用者Id
     * @return : 登入中使用者的 userId
     * @Note : 已更改UserDetailsImpl中的實現方法邏輯，所以這邊authentication.getName()取得的是email
     */
    public Long LoggedInUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUserId();

    }

    /**
     * @description : 使用Spring Security的SecurityContextHolder 取得登入中使用者物件實例
     * @return : 登入中使用者的Entity實例
     * @Note : 已更改UserDetailsImpl中的實現方法邏輯，所以這邊authentication.getName()取得的是email
     */
    public User loggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(()->new RuntimeException("User not found"));
    }
}
