package rsnrdk.websec.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import rsnrdk.websec.entity.Role;
import rsnrdk.websec.entity.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-which-is-long-enough-420";

    private JwtService jwtService;

    private final UserDetails alice = User.builder().username("alice").password("x").role(Role.USER).build();
    private final UserDetails bob = User.builder().username("bob").password("x").role(Role.USER).build();

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    @Test
    void generateToken_validUser_tokenContainsUsername() {
        String token = jwtService.generateToken(alice);

        assertEquals("alice", jwtService.extractUsername(token));
    }

    @Test
    void isValid_freshTokenForSameUser_returnsTrue() {
        String token = jwtService.generateToken(alice);

        boolean valid = jwtService.isValid(token, alice);

        assertTrue(valid);
    }

    @Test
    void isValid_tokenOfAnotherUser_returnsFalse() {
        String token = jwtService.generateToken(alice);

        boolean valid = jwtService.isValid(token, bob);

        assertFalse(valid);
    }

    @Test
    void isValid_expiredToken_throwsExpiredJwtException() {
        // Arrange: negative lifetime makes the token expired at creation, no sleeping needed
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1_000L);
        String token = jwtService.generateToken(alice);

        // Act + Assert
        assertThrows(ExpiredJwtException.class, () -> jwtService.isValid(token, alice));
    }

    @Test
    void extractUsername_tokenSignedWithAnotherKey_throwsSignatureException() {
        String token = jwtService.generateToken(alice);
        ReflectionTestUtils.setField(jwtService, "secret", "another-secret-key-which-is-also-long-enough-999");

        assertThrows(SignatureException.class, () -> jwtService.extractUsername(token));
    }

    @Test
    void generateToken_secretShorterThan32Chars_throwsIllegalState() {
        ReflectionTestUtils.setField(jwtService, "secret", "too-short");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> jwtService.generateToken(alice)
        );

        assertEquals("jwt.secret must be at least 32 characters", exception.getMessage());
    }
}