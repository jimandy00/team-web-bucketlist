package com.team9.bucket_list.security.oauth;

import com.team9.bucket_list.domain.dto.member.MemberProfile;
import com.team9.bucket_list.domain.dto.token.TokenDto;
import com.team9.bucket_list.domain.entity.Member;
import com.team9.bucket_list.domain.entity.RefreshToken;
import com.team9.bucket_list.domain.enumerate.MemberRole;
import com.team9.bucket_list.execption.ApplicationException;
import com.team9.bucket_list.execption.ErrorCode;
import com.team9.bucket_list.repository.MemberRepository;
import com.team9.bucket_list.repository.RefreshTokenRepository;
import com.team9.bucket_list.security.utils.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**주석들은 redirect 구현 시 사용예정**/
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        MemberProfile memberProfile = (MemberProfile) authentication.getPrincipal();
        String email = (String) memberProfile.getAttributes().get("email");
        email = email.replace("jr.", "");

        Optional<Member> duplicatedMember = memberRepository.findByEmailAndOauthIdIsNull(email);
        if(duplicatedMember.isPresent()){
            try {
                response.setContentType("text/html; charset=utf-8");
                PrintWriter w = response.getWriter();
                w.write("<script>alert('해당 이메일로 회원가입한 계정이 있습니다.');location.href='/';</script>");
                w.flush();
                w.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            String targetUrl = determineTargetUrl(request, response, authentication);
            if(response.isCommitted()){
                log.debug("Response has already been commited");
                return;
            }
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication){
        String targetUrl = getDefaultTargetUrl();
//        String targetUrl = "http://localhost:8080/oauth2/redirect";
        log.debug(targetUrl);

        //JWT 생성
        MemberProfile memberProfile = (MemberProfile) authentication.getPrincipal();
        String email = (String) memberProfile.getAttributes().get("email");

        //email이 DB에 존재하는지
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> {
            throw new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED);
        });

        MemberRole role = MemberRole.USER; //일단 다 user로 설정
        TokenDto token = jwtUtil.createToken(member.getId(), member.getUserName(), role);

        // 리프레시 토큰 저장 로직 추가 예정
        //Refresh Token 업데이트
        if (refreshTokenRepository.findByMemberId(member.getId()).isEmpty()) {
            RefreshToken refreshToken = RefreshToken.builder()
                    .memberId(member.getId())
                    .token(token.getRefreshToken())
                    .build();

            refreshTokenRepository.save(refreshToken);
        } else {
            RefreshToken refreshToken = refreshTokenRepository.findByMemberId(member.getId()).get();
            refreshToken.updateToken(token.getRefreshToken());
        }

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("accessToken", token.getAccessToken())
                .build().toUriString();
    }
}

