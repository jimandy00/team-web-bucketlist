package com.team9.bucket_list.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.team9.bucket_list.domain.dto.member.MemberCheckUserNameRequest;
import com.team9.bucket_list.domain.dto.member.MemberDto;
import com.team9.bucket_list.domain.dto.member.MemberJoinRequest;
import com.team9.bucket_list.domain.dto.member.MemberLoginRequest;
import com.team9.bucket_list.domain.entity.Member;
import com.team9.bucket_list.domain.entity.RefreshToken;
import com.team9.bucket_list.execption.ApplicationException;
import com.team9.bucket_list.execption.ErrorCode;
import com.team9.bucket_list.repository.MemberRepository;
import com.team9.bucket_list.repository.RefreshTokenRepository;
import com.team9.bucket_list.domain.dto.token.TokenDto;
import com.team9.bucket_list.security.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final ProfileService profileService;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public Boolean checkUserName(MemberCheckUserNameRequest request) {
        String userName = request.getUserName();
        Optional<Member> findMember = memberRepository.findByUserName(userName);
        if (findMember.isPresent()) {
            throw new ApplicationException(ErrorCode.DUPLICATED_USERNAME);
        } else {
            return true;
        }
    }

    public Boolean checkEmail(String email) {
        Optional<Member> findMember = memberRepository.findByEmail(email);
        if (findMember.isPresent()) {
            throw new ApplicationException(ErrorCode.DUPLICATED_EMAIL);
        } else {
            return true;
        }
    }

    public MemberDto findByEmail(String email) {
        return memberRepository.findByEmail(email).orElseThrow(() -> {
            throw new ApplicationException(ErrorCode.DUPLICATED_EMAIL);
        }).toDto();
    }

    @Transactional
    public MemberDto join(MemberJoinRequest request) {
        //???????????? ????????? ?????? ??? ????????? ????????? ????????? ???????????? ????????? ??????????????? ?????????..?

        String email = request.getEmail();
        String password = request.getPassword();
        String passwordCorrect = request.getPasswordCorrect();

        //?????? ?????? ?????? ??????
        memberRepository.findByEmail(email).ifPresent(member -> {
            throw new ApplicationException(ErrorCode.DUPLICATED_EMAIL);
        });

        //???????????? ?????? ?????? ??????
        if (!password.equals(passwordCorrect)) {
            throw new ApplicationException(ErrorCode.INCORRECT_PASSWORD_CORRECT);
        }

        //?????? ?????? ?????? ??????
        Member member = request.toEntity(encoder.encode(request.getPassword()));
        Member savedMember = memberRepository.save(member);

        return savedMember.toDto();
    }

    @Transactional
    public TokenDto login(MemberLoginRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();

        //email??? DB??? ???????????????
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> {
            throw new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED);
        });

        //email-password ???????????? ??????
        if (!encoder.matches(password, member.getPassword())) {
            throw new ApplicationException(ErrorCode.INVALID_PASSWORD);
        }

        //?????? ????????? ?????? - ?????? ??????
        TokenDto token = jwtUtil.createToken(member.getId(), member.getUserName(), member.getMemberRole());

        //Refresh Token ????????????
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

        return token;
    }

    @Transactional
    public TokenDto reissue(HttpServletRequest request) throws JsonProcessingException {

        final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authorizationHeader.replace("Bearer ", "");

        Long memberId = JwtUtil.getMemberId(token);
        Member member = memberRepository.findById(memberId).orElseThrow(() -> {
            throw new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED);
        });

        //???????????? ?????? ??????
        RefreshToken refreshToken = refreshTokenRepository.findByMemberId(memberId).orElseThrow(() -> {
            throw new ApplicationException(ErrorCode.INVALID_TOKEN);
        });

        boolean isTokenValid = jwtUtil.validateToken(refreshToken.getToken());
        if (isTokenValid) {
            //?????? ?????????
            return jwtUtil.createToken(member.getId(), member.getUserName(), member.getMemberRole());
        } else {
            // ????????? ??????
            throw new ApplicationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}
