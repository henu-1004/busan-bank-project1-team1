package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.CustInfoDTO;
import kr.co.api.flobankapi.jwt.JwtTokenProvider;
import kr.co.api.flobankapi.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustInfoService {
    private final PasswordEncoder passwordEncoder; // SecurityConfig에서 등록한 빈
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberMapper memberMapper;

    /*
    로그인 처리
     */
    public CustInfoDTO login(String custId, String rawPassword) {
        CustInfoDTO custInfoDTO = memberMapper.findByIdCustInfo(custId); // DB에서 ID 있는지 확인
        log.info("login custInfoDTO={}", custInfoDTO);

        if(custInfoDTO == null){ // 없으면
            log.info("로그인 실패: 존재하지 않는 아이디 - {}", custId);
            return null;
        }

        // matches(평문, 암호문) 메서드 사용
        if(passwordEncoder.matches(rawPassword, custInfoDTO.getCustPw())){ // 있으면 비밀번호 확인

            log.warn("로그인 실패: 비밀번호 불일치 - {}", custId);
            return null; // 컨트롤러에서 로그인 실패 처리
        }

        custInfoDTO.setCustPw(null); // 보안상 비밀번호를 null로 보냄. 알 필요 없음.
        // 인증 성공
        return custInfoDTO;
    }
}
