package com.marceldev.companylunchcomment.service;

import com.marceldev.companylunchcomment.dto.reply.CreateReplyDto;
import com.marceldev.companylunchcomment.dto.reply.UpdateReplyDto;
import com.marceldev.companylunchcomment.entity.Comments;
import com.marceldev.companylunchcomment.entity.Member;
import com.marceldev.companylunchcomment.entity.Reply;
import com.marceldev.companylunchcomment.exception.CommentsNotFoundException;
import com.marceldev.companylunchcomment.exception.MemberNotExistException;
import com.marceldev.companylunchcomment.exception.ReplyNotFoundException;
import com.marceldev.companylunchcomment.repository.CommentsRepository;
import com.marceldev.companylunchcomment.repository.MemberRepository;
import com.marceldev.companylunchcomment.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReplyService {

  private final ReplyRepository replyRepository;

  private final CommentsRepository commentsRepository;

  private final MemberRepository memberRepository;

  /**
   * 댓글 작성
   */
  public void createReply(long commentId, CreateReplyDto dto, String email) {
    Member member = memberRepository.findByEmail(email)
        .orElseThrow(MemberNotExistException::new);

    Comments comments = commentsRepository.findById(commentId)
        .orElseThrow(CommentsNotFoundException::new);

    Reply reply = Reply.builder()
        .content(dto.getContent())
        .comments(comments)
        .member(member)
        .build();

    replyRepository.save(reply);
  }

  /**
   * 댓글 수정
   */
  @Transactional
  public void updateReply(long replyId, UpdateReplyDto dto, String email) {
    Member member = memberRepository.findByEmail(email)
        .orElseThrow(MemberNotExistException::new);

    Reply reply = replyRepository.findById(replyId)
        .filter((r) -> r.getMember().getId().equals(member.getId()))
        .orElseThrow(ReplyNotFoundException::new);

    reply.setContent(dto.getContent());
  }

  /**
   * 댓글 삭제
   */
  public void deleteReply(long replyId, String name) {
    Member member = memberRepository.findByEmail(name)
        .orElseThrow(MemberNotExistException::new);

    Reply reply = replyRepository.findById(replyId)
        .filter((r) -> r.getMember().getId().equals(member.getId()))
        .orElseThrow(ReplyNotFoundException::new);

    replyRepository.delete(reply);
  }
}
