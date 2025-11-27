package kr.co.api.flobankapi.service.admin;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.api.flobankapi.dto.AdminInfoDTO;
import kr.co.api.flobankapi.jwt.JwtTokenProvider;
import kr.co.api.flobankapi.mapper.admin.AdminInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final AdminInfoMapper adminInfoMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void login(String adminId,
                      String rawPassword,
                      String adminPh,
                      String code,
                      HttpServletResponse response) {



        int count = adminInfoMapper.countAdmins();
        // 1. TB_ADMIN_INFO 에서 관리자 조회
        AdminInfoDTO admin = adminInfoMapper.findById(adminId);
        if (admin == null) {
            throw new BadCredentialsException("존재하지 않는 관리자입니다.");
        }

        // 2. 비밀번호 검증
        if (!rawPassword.equals(admin.getAdminPw())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        // 4. ADMIN_TYPE 1 = 슈퍼, 2 = 일반 → 둘 다 관리자 권한 부여
        //    인가 기준은 모두 ROLE_ADMIN 으로 통일
        String role = "ROLE_ADMIN";

        // JwtTokenProvider에 맞는 createToken(...) 사용
        // (이미 userId, role, custName 구조로 만들기로 했으니 그대로 사용)
        String token = jwtTokenProvider.createToken(
                admin.getAdminId(),  // userId
                role,                // "ROLE_ADMIN"
                admin.getAdminId()   // ??
        );

        // 5. accessToken 쿠키에 JWT 저장 (JwtTokenProvider.resolveToken 이 이 이름을 찾아야 함)
        Cookie cookie = new Cookie("accessToken", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");        // 전체 경로에서 사용 가능
        cookie.setMaxAge(60 * 60);  // 1시간

        response.addCookie(cookie);
    }
}
