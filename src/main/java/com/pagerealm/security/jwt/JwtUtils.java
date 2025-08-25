package com.pagerealm.security.jwt;

import com.pagerealm.security.service.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    /**
     * @description : 從Request Header中取得"Authorization"(key)的value，
     * 並移除前綴(Bearer )
     * @param request : client端request
     * @return : JWT Token，若無Token或格式錯誤回傳null
     */
    public String getJwtFromHeader(HttpServletRequest request){

        String bearerToken = request.getHeader("Authorization");

        logger.debug("Authorization Header: {}", bearerToken);

        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);//Remove "Bearer " prefix (7 char)
        }
        return null;
    }

    /**
     * @description : 以Email為subject，建構出JWT Token
     * @param userDetails == UserPrincipal
     * @return : JWT Token
     */
    public String generateTokenFromUserEmail(UserDetailsImpl userDetails){

        String email = userDetails.getEmail();

        String roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(email)
                .claim("roles",roles)
                .claim("isTotpEnabled",userDetails.isTotpEnabled())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    /**
     * @description : 解析、驗證並得出該JWT Token payload(claims)的 sub(主體): Email
     * @param token : JWT Token
     * @return : Email
     */
    public String getUserEmailFromJwtToken(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * @description : 產生 JWT 簽名與驗證所需要的HMAC-SHA密鑰
     * 會將組態檔中的Base64密鑰(jwtSecret)解碼，
     * 並轉換為java的Key物件 供JWT建立與驗證時使用
     * @return : 用於 JWT 簽名與驗證的 SecretKey物件
     */
    private Key key(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /**
     * @description : 解析並驗證JWT Token的claims
     * @param authToken : 要被驗證的JWT Token
     * @return : 驗證成功true，失敗false
     * @Note : 此方法不會拋出例外，僅會在logger記錄並回傳false
     */
    public boolean validateJwtToken(String jwtToken){
        try{
            System.out.println("Validate");

            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(jwtToken);
            return true;
        } catch (MalformedJwtException e){
            logger.error("Invalid JWT token: {}",e.getMessage());
        }catch (ExpiredJwtException e){
            logger.error("JWT token is expired: {}",e.getMessage());
        }catch (UnsupportedJwtException e){
            logger.error("JWT token is unsupported: {}", e.getMessage());
        }catch (IllegalArgumentException e){
            logger.error("JWT claims string is empty: {}",e.getMessage());
        }
        return false;
    }


    //----temp token for pre-2FA verify----
    /**
     * for二階段認證
     * @param userDetails
     * @return
     */
    public String generateTempTokenFor2FA(UserDetailsImpl userDetails) {
        String email = userDetails.getEmail();
        return Jwts.builder()
                .subject(email)
                .claim("2fa", true)
                .claim("purpose", "login_2fa")
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + 5 * 60 * 1000)) // 5分鐘
                .signWith(key())
                .compact();
    }

    /**
     * for二階段認證
     * @param token
     * @return
     */
    public boolean is2FAToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Boolean is2fa = claims.get("2fa", Boolean.class);
            String purpose = claims.get("purpose", String.class);
            return Boolean.TRUE.equals(is2fa) && "login_2fa".equals(purpose);
        } catch (Exception e) {
            return false;
        }
    }

}
