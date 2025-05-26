package com.chat_server.authentication.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * packageName    : com.chat_server.authentication.provide
 * fileName       : JwtProvide
 * author         : parkminsu
 * date           : 25. 5. 25.
 * description    : pem 다중 키 관리
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 25.        parkminsu       최초 생성
 */
@Component
public class RsaKeyProvider {

    private final PublicKey publicKey;

    public RsaKeyProvider(@Value("${jwt.public-key-path}") String pemPath) {
        this.publicKey = loadPublicKey(pemPath);
    }

    private PublicKey loadPublicKey(String path) {
        try (InputStream is = ResourceUtils.getURL(path).openStream()) {
            String key = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("공개키 로딩 실패", e);
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}


