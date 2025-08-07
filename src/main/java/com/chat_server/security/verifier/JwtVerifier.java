package com.chat_server.security.verifier;

import com.chat_server.security.provider.RsaKeyProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.PublicKey;

/**
 * packageName    : com.chat_server.authentication.verifier
 * fileName       : JwtVerifier
 * author         : parkminsu
 * date           : 25. 5. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 26.        parkminsu       최초 생성
 */
@Component
public class JwtVerifier {
    private final PublicKey publicKey;

    public JwtVerifier(RsaKeyProvider rsaKeyProvider) {
        this.publicKey = rsaKeyProvider.getPublicKey();
    }

    public boolean verify(String token) throws JwtException {
        parseClaims(token);
        return true;
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

    }
}
