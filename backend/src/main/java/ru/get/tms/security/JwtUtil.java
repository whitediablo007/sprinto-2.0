package ru.get.tms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private final SecretKey secretKey;
  private final long accessTokenExpiration;
  private final long refreshTokenExpiration;

  public JwtUtil(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
      @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  public String generateAccessToken(UUID userId, String email) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId.toString());
    claims.put("email", email);
    return createToken(claims, email, accessTokenExpiration);
  }

  public String generateRefreshToken(UUID userId, String email) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId.toString());
    claims.put("email", email);
    claims.put("type", "refresh");
    return createToken(claims, email, refreshTokenExpiration);
  }

  private String createToken(Map<String, Object> claims, String subject, long expiration) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(secretKey)
        .compact();
  }

  public UUID getUserIdFromToken(String token) {
    Claims claims = extractAllClaims(token);
    String userIdStr = claims.get("userId", String.class);
    return UUID.fromString(userIdStr);
  }

  public String getEmailFromToken(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date getExpirationDateFromToken(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
  }

  public Boolean isTokenExpired(String token) {
    try {
      final Date expiration = getExpirationDateFromToken(token);
      return expiration.before(new Date());
    } catch (Exception e) {
      return true;
    }
  }

  public Boolean validateToken(String token, String email) {
    try {
      final String tokenEmail = getEmailFromToken(token);
      return (tokenEmail.equals(email) && !isTokenExpired(token));
    } catch (Exception e) {
      return false;
    }
  }

  public Boolean isRefreshToken(String token) {
    try {
      Claims claims = extractAllClaims(token);
      String type = claims.get("type", String.class);
      return "refresh".equals(type);
    } catch (Exception e) {
      return false;
    }
  }
}
