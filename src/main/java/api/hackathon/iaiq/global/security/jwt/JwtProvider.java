package api.hackathon.iaiq.global.security.jwt;

import static api.hackathon.iaiq.global.exception.ErrorType._JWT_EXPIRED;
import static api.hackathon.iaiq.global.exception.ErrorType._JWT_PARSING_ERROR;
import static api.hackathon.iaiq.global.exception.ErrorType._USER_NOT_FOUND_BY_TOKEN;

import api.hackathon.iaiq.domain.Member.domain.Member;
import api.hackathon.iaiq.domain.Member.repository.MemberRepository;
import api.hackathon.iaiq.global.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtProvider {

    private final MemberRepository memberRepository;
    public static final long ACCESS_TOKEN_VALID_TIME = 15 * 60 * 1000L;

    @Value("${jwt.secret.key}")
    private String SECRET_KEY;

    public String generateJwtToken(final String id) {
        Claims claims = createClaims(id);
        Date now = new Date();
        long expiredDate = calculateExpirationDate(now);
        SecretKey secretKey = generateKey();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(expiredDate))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT claims 생성
    private Claims createClaims(final String id) {
        return Jwts.claims().setSubject(id);
    }

    // JWT 만료 시간 계산
    private long calculateExpirationDate(final Date now) {
        return now.getTime() + ACCESS_TOKEN_VALID_TIME;
    }

    // Key 생성
    private SecretKey generateKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    // 토큰의 유효성 검사
    public void isValidToken(final String jwtToken) {
        try {
            SecretKey key = generateKey();
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwtToken);

        } catch (ExpiredJwtException e) { // 어세스 토큰 만료
            throw new ApiException(_JWT_EXPIRED);
        } catch (Exception e) {
            throw new ApiException(_JWT_PARSING_ERROR);
        }
    }

    // jwtToken 으로 Authentication 에 사용자 등록
    public void getAuthenticationFromToken(final String jwtToken) {

        log.info("-------------- getAuthenticationFromToken jwt token: " + jwtToken);
        Member loginMember = getEmail(jwtToken);

        setContextHolder(jwtToken, loginMember);
    }

    // token 으로부터 유저 정보 확인
    private Member getEmail(final String jwtToken) {
        String email = getUserIdFromToken(jwtToken);
        return memberRepository.findMemberByEmail(email)
                .orElseThrow(() -> new ApiException(_USER_NOT_FOUND_BY_TOKEN));
    }

    private void setContextHolder(String jwtToken, Member loginMember) {

        // ToDO 현재 비어있는 권한 등록, 추후에 수정
        List<GrantedAuthority> authorities = new ArrayList<>();
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginMember, jwtToken, authorities);

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    // 토큰에서 유저 아이디 얻기
    public String getUserIdFromToken(final String jwtToken) {
        SecretKey key = generateKey();

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwtToken)
                .getBody();

        log.info("-------------- JwtProvider.getUserIdFromAccessToken: " + claims.getSubject());
        return claims.getSubject();
    }
}
