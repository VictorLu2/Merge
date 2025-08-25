package com.pagerealm.security.service;

import com.pagerealm.entity.User;
import com.pagerealm.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    //-----------------------------
    //此方法用來：
    //1.在資料庫中以username搜尋對應的User
    //2.將找到的user物件傳給UserDetailsImpl中的build(User user)方法，並以此物件資訊建構出UserPrincipal
    //Note: (1)UserDetailsImpl繼承於UserDetails，所以符合回傳值
    //      (2)開發中可以將username參數改為email，因為當有多個oauth2授權功能時，登入授權後如偵測到DB中已有此email
    //         就會直接登入該email的user，此狀態下該oauth2 source的使用者名稱可能會衝突，造成NoUserNameException
    @Override
    @Transactional //確保資料庫操作一致性，避免Lazy Loading產生的例外
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return UserDetailsImpl.build(user);
    }
}
