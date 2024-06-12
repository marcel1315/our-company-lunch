package com.marceldev.companylunchcomment.service;

import com.marceldev.companylunchcomment.dto.reply.CreateReplyDto;
import com.marceldev.companylunchcomment.dto.reply.ReplyOutputDto;
import com.marceldev.companylunchcomment.dto.reply.UpdateReplyDto;
import com.marceldev.companylunchcomment.entity.Comment;
import com.marceldev.companylunchcomment.entity.Member;
import com.marceldev.companylunchcomment.entity.Reply;
import com.marceldev.companylunchcomment.exception.CommentNotFoundException;
import com.marceldev.companylunchcomment.exception.MemberNotExistException;
import com.marceldev.companylunchcomment.exception.ReplyNotFoundException;
import com.marceldev.companylunchcomment.repository.comment.CommentRepository;
import com.marceldev.companylunchcomment.repository.member.MemberRepository;
import com.marceldev.companylunchcomment.repository.reply.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplyService {

  private final ReplyRepository replyRepository;

  private final CommentRepository commentRepository;

  private final MemberRepository memberRepository;

  /**
   * 댓글 작성
   */
  @Transactional
  public void createReply(long commentId, CreateReplyDto dto) {
    Member member = getMember();

    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(CommentNotFoundException::new);

    Reply reply = Reply.builder()
        .content(dto.getContent())
        .comment(comment)
        .member(member)
        .build();

    replyRepository.save(reply);
  }

  /**
   * 댓글 조회
   */
  // TODO: Sort by createdAt desc
  public Page<ReplyOutputDto> getReplyList(long commentId, Pageable pageable) {
    // TODO: Filter out if member can't see the comments
    return replyRepository.findByCommentId(commentId, pageable)
        .map(ReplyOutputDto::of);
  }

  /**
   * 댓글 수정
   */
  @Transactional
  public void updateReply(long replyId, UpdateReplyDto dto) {
    Member member = getMember();

    Reply reply = replyRepository.findById(replyId)
        .filter((r) -> r.getMember().getId().equals(member.getId()))
        .orElseThrow(ReplyNotFoundException::new);

    reply.setContent(dto.getContent());
  }

  /**
   * 댓글 삭제
   */
  @Transactional
  public void deleteReply(long replyId) {
    Member member = getMember();

    Reply reply = replyRepository.findById(replyId)
        .filter((r) -> r.getMember().getId().equals(member.getId()))
        .orElseThrow(ReplyNotFoundException::new);

    replyRepository.delete(reply);
  }

  /**
   * member를 찾아 반환함
   */
  private Member getMember() {
    UserDetails user = (UserDetails) SecurityContextHolder.getContext()
        .getAuthentication()
        .getPrincipal();
    String email = user.getUsername();
    return memberRepository.findByEmail(email)
        .orElseThrow(MemberNotExistException::new);
  }
}
