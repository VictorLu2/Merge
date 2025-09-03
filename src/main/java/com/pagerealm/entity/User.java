package com.pagerealm.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pagerealm.security.converter.StringCryptoConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = "email")})
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username")
    private String userName;

    @NotBlank
    @Email
    @Column(name = "email")
    private String email;

    @Column(name = "password_hash")
    @JsonIgnore
    private String password;

    private String gender;

    private LocalDate birthdate;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_expiration")
    private LocalDateTime verificationCodeExpiresAt;

    private boolean enabled;

    @Column(name = "totp_secret_enc")
    @Convert(converter = StringCryptoConverter.class)
    private String totpSecret;

    @Column(name = "totp_enabled")
    private boolean totpEnabled = false;

    @Column(name = "signed_method")
    private String signedMethod;

    //----------會員等級------------
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_tier")
    private MembershipTier membershipTier = MembershipTier.LV1;

    @Column(name = "membership_window_start")
    private LocalDateTime membershipWindowStart;

    @Column(name = "membership_window_end")
    private LocalDateTime membershipWindowEnd;

    @Column(name = "membership_window_total")
    private BigDecimal membershipWindowTotal = BigDecimal.ZERO;


    //-------------------------------------
    @ManyToOne(fetch = FetchType.EAGER,cascade = CascadeType.MERGE)
    @JoinColumn(name = "role_id",referencedColumnName = "role_id")
    @ToString.Exclude
    private Role role;

    //-------------------------------------
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    public User(String userName, String email, String password, String gender, LocalDate birthdate) {
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.gender = gender;
        this.birthdate = birthdate;
    }

    public User(String userName, String email) {
        this.userName = userName;
        this.email = email;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof User)) return false;
        return userId != null && userId.equals(((User) o ).getUserId());
    }

    @Override
    public int hashCode(){
        return getClass().hashCode();
    }
}