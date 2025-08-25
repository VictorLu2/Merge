package com.pagerealm.security.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pagerealm.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 此類別等同於UserPrincipal，核心功能是：
 * 1. 將User entity的欄位資料(id,username,email,password,權限等)封裝進一個Spring Security可以識別的物件(UserPrincipal)
 * 2. 實作UserDetails介面，讓Spring Security的驗證流程能正確取得使用者的各項資訊
 *----------------------------
 *
 * 關於 private static final long "serialVersionUID" = 1L (數據類型必須聲明為long，值隨意)
 * UserDetails這個介面繼承於Serializable，因此可以直接聲明此屬性，其作用是
 * 讓物件被序列化和反序列化時，JVM可以比對此UID，以避免版本不一致
 * ----------------------------
 *
 * 關於 List.of():
 * 主要用來建立一個不可變的List，常用於只需要存放少量元素，且不希望被修改的情況
 * ex: 1.初始化只有一個或幾個元素的List
 *     2.傳遞常量集合給方法
 *     3.保證集合內容不會被外部更改
 *     4. List<String> authorities = List.of("USER","ADMIN");
 */

@Data
@NoArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private String email;

    @JsonIgnore
    private String password;

    private boolean totpEnabled;

    private boolean enabled;

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String username, String email, String password, boolean totpEnabled, boolean enabled, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.totpEnabled = totpEnabled;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    //---------------------------
    //此方法用來將User entity的資料進行封裝，並建構出含有User資料的(UserPrincipal)
    public static UserDetailsImpl build(User user){
        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().getRoleName().name());

        return new UserDetailsImpl(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getPassword(),
                user.isTotpEnabled(),
                user.isEnabled(),
                List.of(authority)); //將單一或兩個權限裝進不可變的List中
    }

    public String getUserName(){
        return this.username;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    //專案中登入與註冊的唯一識別是email，所以此實現方法回傳email
    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}