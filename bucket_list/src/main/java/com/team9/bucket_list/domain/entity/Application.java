package com.team9.bucket_list.domain.entity;

import com.team9.bucket_list.domain.dto.application.ApplicationDecisionRequest;
import com.team9.bucket_list.domain.dto.application.ApplicationRequest;
import com.team9.bucket_list.domain.dto.application.ApplicationResponse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Application {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    private String content;

    //신청 상태 (선택안함 : 0, 승낙 : 1, 거절 : 2)
    private byte status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    public static Application save(ApplicationRequest applicationRequest, Post post, Member member) {
        return Application.builder()
                .content(applicationRequest.getContent())
                .status((byte)0)
                .member(member)
                .post(post)
                .build();
    }

    public static Application updateStatus(Application application, ApplicationDecisionRequest applicationDecisionRequest) {
        return Application.builder()
                .id(application.getId())
                .content(application.getContent())
                .status(applicationDecisionRequest.getStatus())
                .member(application.getMember())
                .post(application.getPost())
                .build();
    }
}
