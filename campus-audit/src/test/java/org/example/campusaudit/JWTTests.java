package org.example.campusaudit;
import org.example.campusaudit.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JWTTests {
    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void testJWTGeneration() {
        String token = jwtUtils.createToken("3bccb109eb0411f0850f7ced8dfee038", "ADMIN");

        System.out.println("Generated JWT Token: " + token);
    }
}
