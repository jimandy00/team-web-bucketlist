package com.team9.bucket_list.repository;

import com.team9.bucket_list.domain.entity.Post;

import com.team9.bucket_list.domain.enumerate.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findById (Long id);

    @Query("select p.member.id from Post p where p.member.id = :memberId")
    Long findByMemberId(@Param("memberId") Long memberId);

    Page<Post> findByMember_Id(Long memberId, Pageable pageable);

    Page<Post> findByIdIn(Set<Long> postId, Pageable pageable);

    Page<Post> findByIdInAndStatus(Set<Long> postId, PostStatus status, Pageable pageable);

    Page<Post> findByCategory(String category, Pageable pageable);

    Page<Post> findByTitleContaining(String keyword,Pageable pageable);

    Page<Post> findByCategoryAndTitleContaining(String category,String keyword,Pageable pageable);

    Page<Post> findByCategoryAndEventStartBetweenAndEventEndBetweenAndCostBetween( String category, String eventStart, String eventmax, String eventmin, String eventEnd, Integer lowCost, Integer upCost,Pageable pageable);

    Page<Post> findByTitleContainingAndEventStartBetweenAndEventEndBetweenAndCostBetween( String keyword, String eventStart, String eventmax, String eventmin, String eventEnd, Integer lowCost, Integer upCost,Pageable pageable);

    Page<Post> findByEventStartBetweenAndEventEndBetweenAndCostBetween(String eventStart, String eventmax, String eventmin, String eventEnd, Integer lowCost, Integer upCost,Pageable pageable);

    Page<Post> findByCategoryAndTitleContainingAndEventStartBetweenAndEventEndBetweenAndCostBetween( String category, String keyword, String eventStart, String eventmax, String eventmin, String eventEnd, Integer lowCost, Integer upCost,Pageable pageable);

    List<Post> findByEventEnd(String eventEnt);

    Set<Post> findByMember_Id(Long memberId);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE post p set p.status = 'JOIN' WHERE p.post_id = :postId", nativeQuery = true)
    void updateStatusJoin(Long postId);
}
