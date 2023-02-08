package com.team9.bucket_list.service;

import com.team9.bucket_list.domain.dto.memberReview.MemberReviewRequest;
import com.team9.bucket_list.domain.entity.Alarm;
import com.team9.bucket_list.domain.entity.Member;
import com.team9.bucket_list.domain.entity.MemberReview;
import com.team9.bucket_list.execption.ApplicationException;
import com.team9.bucket_list.execption.ErrorCode;
import com.team9.bucket_list.repository.AlarmRepository;
import com.team9.bucket_list.repository.MemberRepository;
import com.team9.bucket_list.repository.MemberReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberReviewService {

    private final MemberRepository memberRepository;
    private final MemberReviewRepository memberReviewRepository;
    private final AlarmRepository alarmRepository;

    public Member checkMemberId(Long targetUserId) {
        return memberRepository.findById(targetUserId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED));
    }

    public Member checkMemberName(String fromUserName) {
        return memberRepository.findByUserName(fromUserName)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED));
    }

    public MemberReview checkMemberReview(Long reviewId) {
        return memberReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND));
    }

    public Page<MemberReview> list (String targetUserName, Pageable pageable) {
        Member member = checkMemberName(targetUserName);
        return memberReviewRepository.findAllByMember(member, pageable);
    }

//    public void score (Long targetUserId) {
//        MemberReview memberReview = memberReviewRepository.findByUserId(targetUserId)
//                .orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND));
//
//        double avg = 0;
//        List<MemberReview> memberReviewList = memberReviewRepository.findAllByUserId(targetUserId);
//        for ( MemberReview m : memberReviewList) {
//            avg += m.getRate();
//        }
//        avg = (avg / memberReviewList.size());
//        avg = Math.round(avg*100)/100.0;
//
////        memberReviewRepository.findByUserId(targetUserId);
//
//    }


    public String create(Long memberId, MemberReviewRequest memberReviewRequest) {

        Member targetMember = checkMemberId(memberReviewRequest.getTargetMemberId());
        Member fromMember = checkMemberId(memberId);

        MemberReview memberReview = memberReviewRepository.save(memberReviewRequest.toEntity(targetMember, fromMember));
        // 리뷰 작성하라는 알람 삭제

        // alarmRepository.save(Alarm.of(targetMember, targetMember.getUserName()+"에 대한 리뷰가 작성 되었습니다."));

        return "true";
    }
}
