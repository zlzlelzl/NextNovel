package com.a509.service_member.service;

import com.a509.service_member.component.MemberImageComponent;
import com.a509.service_member.dto.request.*;
import com.a509.service_member.dto.response.MemberTokenResponseDto;
import com.a509.service_member.dto.response.MemberMyPageResponseDto;
import com.a509.service_member.dto.response.MessageResponseDto;
import com.a509.service_member.enums.MemberState;
import com.a509.service_member.exception.DuplicatedMemberException;
import com.a509.service_member.exception.EmptyValueException;
import com.a509.service_member.exception.InvalidedAccessTokenException;
import com.a509.service_member.exception.NoSuchMemberException;
import com.a509.service_member.jpa.member.Member;
import com.a509.service_member.jpa.member.MemberRepository;
import com.a509.service_member.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;

import java.nio.file.NoSuchFileException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final MemberImageComponent memberImageComponent;

    public Member findMember(String email) {
        return memberRepository.findByEmail(email).orElseThrow(NoSuchMemberException::new);
    }

    @Transactional
    public void signUp(MemberSignupRequestDto memberSignupRequestDto) {
        if (memberRepository.existsByEmail(memberSignupRequestDto.getEmail())) {
            throw new DuplicatedMemberException();
        }

        if (memberRepository.existsByNickName(memberSignupRequestDto.getNickName())) {
            throw new DuplicatedMemberException("중복된 닉네임입니다.");
        }

        Member member = Member
                .builder()
                .email(memberSignupRequestDto.getEmail())
                .password(bCryptPasswordEncoder.encode(memberSignupRequestDto.getPassword()))
                .nickName(memberSignupRequestDto.getNickName())
                .profileImage(memberSignupRequestDto.getProfileImage())
                .build();
        memberRepository.save(member);

        try {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public MessageResponseDto checkEmail(MemberSignupCheckRequestDto memberSignupCheckRequestDto) {
        String res = "";
        String msg = "";
        if (memberRepository.existsByEmail(memberSignupCheckRequestDto.getEmail())) {
            res = "fail";
            msg = "중복된 이메일입니다.";
        } else {
            res = "success";
            msg = "사용 가능한 이메일입니다.";
        }
        MessageResponseDto message = MessageResponseDto.builder()
                .result(res)
                .message(msg)
                .build();
        return message;
    }

    public MessageResponseDto checkNickName(MemberSignupCheckRequestDto memberSignupCheckRequestDto) {
        String res = "";
        String msg = "";
        if (memberRepository.existsByNickName(memberSignupCheckRequestDto.getNickName())) {
            res = "fail";
            msg = "중복된 닉네임입니다.";
        } else {
            res = "success";
            msg = "사용 가능한 닉네임입니다.";
        }
        MessageResponseDto message = MessageResponseDto.builder()
                .result(res)
                .message(msg)
                .build();
        return message;
    }

    @Transactional
    public MemberTokenResponseDto login(MemberLoginRequestDto memberLoginRequestDto) {
        Member member = findMember(memberLoginRequestDto.getEmail());
        if (member.getState().equals(MemberState.RESIGNED.name())) throw new NoSuchMemberException("탈퇴한 사용자입니다.");

        // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
        // 이때 authentication 는 인증 여부를 확인하는 authenticated 값이 false
        UsernamePasswordAuthenticationToken authenticationToken = memberLoginRequestDto.toAuthentication();

        // 2. 실제 검증 (사용자 비밀번호 체크)이 이루어지는 부분
        // authenticate 매서드가 실행될 때 CustomUserDetailsService 에서 만든 loadUserByUsername 메서드가 실행
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        MemberTokenResponseDto tokenInfo = jwtTokenProvider.generateToken(authentication, member);

        // 4. RefreshToken Redis 저장 (expirationTime 설정을 통해 자동 삭제 처리)
        // "RT:{AccessToken}" : "{RefreshToken}"
        stringRedisTemplate.opsForValue()
                .set("RT:" + tokenInfo.getAccessToken(), tokenInfo.getRefreshToken(), tokenInfo.getRefreshTokenExpirationTime(), TimeUnit.MILLISECONDS);

        return tokenInfo;
    }

    public void logout(String token) {
        updateRedisItems(token, "logout");
    }

    public MemberMyPageResponseDto findMyPage(String token) {
        Member member = findMember(jwtTokenProvider.getMember(token));
        return new MemberMyPageResponseDto().fromMeEntity(member);
    }

    public String findMyPageImage(String nickName) {
        Optional<Member> member = memberRepository.findByNickName(nickName);
        if(member.isPresent()) {
            return member.get().getProfileImage();
        } else {
            return "defaultProfileImg.png";
        }
    }

    @Transactional
    public void update(String token, MemberUpdateRequestDto memberUpdateRequestDto, MultipartFile multipartFile) {
        Member member = findMember(jwtTokenProvider.getMember(token));

        String nickName = memberUpdateRequestDto.getNickName().trim();
        if ("".equals(nickName)) {    // 닉네임 공백 체크
            throw new NoSuchMemberException("닉네임은 필수 입력 사항입니다.");
        }
        if (!member.getNickName().equals(nickName) &&
                memberRepository.existsByNickName(nickName)) { // 닉네임 중복체크
            throw new DuplicatedMemberException("중복된 닉네임입니다.");
        }
        member.setNickName(nickName);

        if(!multipartFile.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
            String UID = now.format(formatter);
            try{
                memberImageComponent.save(multipartFile,UID);
                member.setProfileImage(UID+"_"+multipartFile.getOriginalFilename());
            }
            catch(Exception e) {
                throw new RuntimeException();
            }
        }
    }

    @Transactional
    public void updatePassword(String token, MemberUpdatePasswordRequestDto memberUpdatePasswordRequestDto) {
        Member member = findMember(jwtTokenProvider.getMember(token));

        String password = memberUpdatePasswordRequestDto.getPassword().trim();
        if("".equals(password)) {
            throw new EmptyValueException("변경할 비밀번호를 입력해주세요.");
        }
        if (member.getProvider() == null) {
            member.setPassword(bCryptPasswordEncoder.encode(password));
        }
    }

    @Transactional
    public void delete(String token) {
        Member member = findMember(jwtTokenProvider.getMember(token));
        member.setState(MemberState.RESIGNED.name());

        updateRedisItems(token, "resign");
    }

    @Transactional
    public void updateRedisItems(String token, String status) {

        String accessToken = token.split(" ")[1];

        // 1. Access Token 검증
        if (!jwtTokenProvider.validateToken(accessToken).equals("ok")) throw new InvalidedAccessTokenException();

        // 2. Access Token 에서 Member email 을 가져옵니다.
        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);

        // 3. Redis Access Token 으로 저장된 Refresh Token 이 있는지 여부를 확인 후 있을 경우 삭제합니다.
        // "RT:{AccessToken}" : "{RefreshToken}"
        if (stringRedisTemplate.opsForValue().get("RT:" + accessToken) != null) {
            // Refresh Token 삭제
            stringRedisTemplate.delete("RT:" + accessToken);
        }

        // 4. 해당 Access Token 유효시간 가지고 와서 BlackList 로 저장하기
        Long expiration = jwtTokenProvider.getExpiration(accessToken);
        stringRedisTemplate.opsForValue().set(accessToken, status, expiration, TimeUnit.MILLISECONDS);
    }
}
