package com.marceldev.companylunchcomment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marceldev.companylunchcomment.dto.reply.CreateReplyDto;
import com.marceldev.companylunchcomment.dto.reply.GetReplyListDto;
import com.marceldev.companylunchcomment.dto.reply.UpdateReplyDto;
import com.marceldev.companylunchcomment.entity.Comment;
import com.marceldev.companylunchcomment.entity.Company;
import com.marceldev.companylunchcomment.entity.Member;
import com.marceldev.companylunchcomment.entity.Reply;
import com.marceldev.companylunchcomment.exception.comment.CommentNotFoundException;
import com.marceldev.companylunchcomment.exception.reply.ReplyNotFoundException;
import com.marceldev.companylunchcomment.repository.comment.CommentRepository;
import com.marceldev.companylunchcomment.repository.company.CompanyRepository;
import com.marceldev.companylunchcomment.repository.member.MemberRepository;
import com.marceldev.companylunchcomment.repository.reply.ReplyRepository;
import com.marceldev.companylunchcomment.type.Role;
import com.marceldev.companylunchcomment.type.ShareStatus;
import com.marceldev.companylunchcomment.util.LocationUtil;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("댓글 서비스")
class ReplyServiceTest {

  @Mock
  private ReplyRepository replyRepository;

  @Mock
  private MemberRepository memberRepository;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private CompanyRepository companyRepository;

  @InjectMocks
  private ReplyService replyService;

  // 테스트에서 목으로 사용될 company. diner를 가져올 때, member가 속한 company의 diner가 아니면 가져올 수 없음
  private final Company company1 = Company.builder()
      .id(1L)
      .name("좋은회사")
      .address("서울특별시 강남구 강남대로 200")
      .location(LocationUtil.createPoint(127.123123, 37.123123))
      .domain("example.com")
      .build();

  // 테스트에서 목으로 사용될 member. diner를 가져올 때, 적절한 member가 아니면 가져올 수 없음
  private final Member member1 = Member.builder()
      .id(1L)
      .email("kys@example.com")
      .name("김영수")
      .role(Role.VIEWER)
      .password("somehashedvalue")
      .company(company1)
      .build();

  @BeforeEach
  public void setupMember() {
    GrantedAuthority authority = new SimpleGrantedAuthority("VIEWER");
    Authentication authentication = new UsernamePasswordAuthenticationToken(member1.getEmail(),
        null, List.of(authority));

    SecurityContext securityContext = mock(SecurityContext.class);
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

    SecurityContextHolder.setContext(securityContext);

    lenient().when(memberRepository.findByEmail(any()))
        .thenReturn(Optional.of(member1));

    lenient().when(memberRepository.findById(any()))
        .thenReturn(Optional.of(member1));
  }

  @AfterEach
  public void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("댓글 작성 - 성공")
  void create_reply() {
    //given
    CreateReplyDto dto = CreateReplyDto.builder()
        .content("댓글입니다.")
        .build();
    when(commentRepository.findById(any()))
        .thenReturn(Optional.of(
            Comment.builder().id(2L).build()
        ));
    when(companyRepository.findCompanyByCommentId(2L))
        .thenReturn(Optional.of(company1));

    //when
    replyService.createReply(2L, dto);
    ArgumentCaptor<Reply> captor = ArgumentCaptor.forClass(Reply.class);

    //then
    verify(replyRepository).save(captor.capture());
    assertEquals(1L, captor.getValue().getMember().getId());
    assertEquals(2L, captor.getValue().getComment().getId());
    assertEquals("댓글입니다.", captor.getValue().getContent());
  }

  @Test
  @DisplayName("댓글 작성 - 실패(코멘트가 존재하지 않음)")
  void create_reply_fail_no_comments() {
    //given
    CreateReplyDto dto = CreateReplyDto.builder()
        .content("댓글입니다.")
        .build();
    when(companyRepository.findCompanyByCommentId(1L))
        .thenReturn(Optional.of(company1));
    when(commentRepository.findById(any()))
        .thenReturn(Optional.empty());

    //when
    //then
    assertThrows(CommentNotFoundException.class,
        () -> replyService.createReply(1L, dto));
  }

  @Test
  @DisplayName("댓글 수정 - 성공")
  void update_reply() {
    //given
    UpdateReplyDto dto = UpdateReplyDto.builder()
        .content("댓글 수정")
        .build();
    when(replyRepository.findById(2L))
        .thenReturn(Optional.of(
            Reply.builder()
                .id(2L)
                .member(Member.builder().id(1L).build())
                .build()
        ));
    when(companyRepository.findCompanyByReplyId(2L))
        .thenReturn(Optional.of(company1));

    //when
    replyService.updateReply(2L, dto);

    //then
    verify(replyRepository).findById(2L);
  }

  @Test
  @DisplayName("댓글 수정 - 실패(댓글이 존재하지 않음)")
  void update_reply_fail_no_reply() {
    //given
    UpdateReplyDto dto = UpdateReplyDto.builder()
        .content("댓글 수정")
        .build();

    //when
    when(replyRepository.findById(2L))
        .thenReturn(Optional.empty());
    when(companyRepository.findCompanyByReplyId(2L))
        .thenReturn(Optional.of(company1));

    //then
    assertThrows(ReplyNotFoundException.class,
        () -> replyService.updateReply(2L, dto));
  }

  @Test
  @DisplayName("댓글 삭제 - 성공")
  void delete_reply() {
    //given
    //when
    when(replyRepository.findById(1L))
        .thenReturn(Optional.of(
            Reply.builder()
                .id(1L)
                .member(Member.builder().id(1L).build())
                .build()
        ));
    when(companyRepository.findCompanyByReplyId(1L))
        .thenReturn(Optional.of(company1));
    replyService.deleteReply(1L);

    //then
    verify(replyRepository).delete(any());
  }

  @Test
  @DisplayName("댓글 삭제 - 실패(댓글이 존재하지 않음)")
  void delete_reply_fail_no_reply() {
    //given
    //when
    when(replyRepository.findById(2L))
        .thenReturn(Optional.empty());
    when(companyRepository.findCompanyByReplyId(2L))
        .thenReturn(Optional.of(company1));

    //then
    assertThrows(ReplyNotFoundException.class,
        () -> replyService.deleteReply(2L));
  }

  @Test
  @DisplayName("댓글 조회 - 성공")
  void get_reply_list() {
    //given
    Page<Reply> page = new PageImpl<>(List.of(
        Reply.builder()
            .id(1L)
            .content("댓글입니다.")
            .member(member1)
            .build()
    ));
    GetReplyListDto dto = GetReplyListDto.builder()
        .page(0)
        .size(10)
        .build();
    PageRequest pageable = PageRequest.of(0, 10);

    //when
    when(commentRepository.findById(any()))
        .thenReturn(Optional.of(Comment.builder()
            .id(1L)
            .shareStatus(ShareStatus.COMPANY)
            .build()));
    when(replyRepository.findByCommentIdOrderByCreatedAtDesc(anyLong(), any()))
        .thenReturn(page);
    when(companyRepository.findCompanyByCommentId(1L))
        .thenReturn(Optional.of(company1));

    //then
    replyService.getReplyList(1L, dto);
  }
}