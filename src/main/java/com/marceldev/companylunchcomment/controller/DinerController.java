package com.marceldev.companylunchcomment.controller;

import com.marceldev.companylunchcomment.dto.diner.AddDinerTagsDto;
import com.marceldev.companylunchcomment.dto.diner.CreateDinerDto;
import com.marceldev.companylunchcomment.dto.diner.DinerDetailOutputDto;
import com.marceldev.companylunchcomment.dto.diner.DinerOutputDto;
import com.marceldev.companylunchcomment.dto.diner.GetDinerListDto;
import com.marceldev.companylunchcomment.dto.diner.RemoveDinerTagsDto;
import com.marceldev.companylunchcomment.dto.diner.UpdateDinerDto;
import com.marceldev.companylunchcomment.dto.error.ErrorResponse;
import com.marceldev.companylunchcomment.exception.diner.DinerMaxImageCountExceedException;
import com.marceldev.companylunchcomment.exception.diner.DuplicateDinerTagException;
import com.marceldev.companylunchcomment.exception.diner.ImageWithNoExtensionException;
import com.marceldev.companylunchcomment.service.DinerImageService;
import com.marceldev.companylunchcomment.service.DinerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Tag(name = "3 Diner", description = "식당 관련")
public class DinerController {

  private final DinerService dinerService;

  private final DinerImageService dinerImageService;

  @Operation(
      summary = "식당 생성",
      description = "식당 이름, 식당 웹사이트 링크, 위도, 경도를 입력한다.<br>"
          + "식당 태그도 입력 가능하다. (#한식, #양식, #깔끔, #간단, #매움, #양많음 등 사용자가 임의 등록 가능)"
  )
  @PostMapping("/diners")
  public ResponseEntity<Void> createDiner(
      @Validated @RequestBody CreateDinerDto createDinerDto
  ) {
    dinerService.createDiner(createDinerDto);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "식당 목록 조회",
      description = "사용자는 식당의 목록을 조회할 수 있다.<br>"
          + "식당 이름, 태그, 거리, 코멘트 갯수를 볼 수 있다.<br>"
          + "회사의 위도와 경도, 식당의 위도와 경도를 사용해 회사로부터 식당의 거리를 표시한다.<br>"
          + "식당 이름, 거리, 코멘트 갯수로 정렬할 수 있다."
  )
  @GetMapping("/diners")
  public ResponseEntity<Page<DinerOutputDto>> getDinerList(
      @Validated @ModelAttribute GetDinerListDto getDinerListDto
  ) {
    Page<DinerOutputDto> diners = dinerService.getDinerList(getDinerListDto);
    return ResponseEntity.ok(diners);
  }

  @Operation(
      summary = "식당 상세 조회",
      description = "사용자는 식당을 상세 조회할 수 있다.<br>"
          + "이름, 태그, 거리, 코멘트 갯수, 코멘트 목록, 사진 썸네일 목록을 볼 수 있다.<br>"
          + "필요한 경우 원본 사진을 가져올 수 있다."
  )
  @GetMapping("/diners/{id}")
  public ResponseEntity<DinerDetailOutputDto> getDinerDetail(
      @PathVariable long id
  ) {
    DinerDetailOutputDto diner = dinerService.getDinerDetail(id);
    return ResponseEntity.ok(diner);
  }

  @Operation(
      summary = "식당 정보 수정",
      description = "사용자는 식당 웹사이트 링크, 위도, 경도 정보를 수정할 수 있다.<br>"
          + "자신이 작성하지 않은 식당도 수정할 수 있다."
  )
  @PutMapping("/diners/{id}")
  public ResponseEntity<Void> updateDiner(
      @PathVariable long id,
      @Validated @RequestBody UpdateDinerDto updateDinerDto
  ) {
    dinerService.updateDiner(id, updateDinerDto);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "식당 제거",
      description = "사용자는 식당을 제거할 수 있다."
  )
  @DeleteMapping("/diners/{id}")
  public ResponseEntity<Void> removeDiner(
      @PathVariable long id
  ) {
    dinerService.removeDiner(id);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "식당 태그 추가"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "errorCode: 3002 - 중복 태그 존재")
  })
  @PutMapping("/diners/{id}/tags")
  public ResponseEntity<Void> addDinerTags(
      @PathVariable long id,
      @Validated @RequestBody AddDinerTagsDto addDinerTagsDto
  ) {
    dinerService.addDinerTag(id, addDinerTagsDto);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "식당 태그 제거"
  )
  @DeleteMapping("/diners/{id}/tags")
  public ResponseEntity<Void> removeDinerTags(
      @PathVariable long id,
      @Validated @RequestBody RemoveDinerTagsDto removeDinerTagsDto
  ) {
    dinerService.removeDinerTag(id, removeDinerTagsDto);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "식당 이미지 추가"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "errorCode: 3001 - 최대 이미지 갯수 초과<br>"
          + "errorCode: 3003 - 파일의 확장자가 없음")
  })
  @PostMapping(value = "/diners/{id}/images", consumes = "multipart/form-data")
  public ResponseEntity<Void> addDinerImage(
      @PathVariable long id,
      @RequestParam("image") MultipartFile image
  ) {
    dinerImageService.addDinerImage(id, image);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "식당 이미지 제거"
  )
  @DeleteMapping("/diners/images/{id}")
  public ResponseEntity<Void> removeDinerImage(
      @PathVariable long id
  ) {
    dinerImageService.removeDinerImage(id);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "식당 구독"
  )
  @PostMapping("/diners/{id}/subscribe")
  public ResponseEntity<Void> subscribeDiner(
      @PathVariable long id
  ) {
    dinerService.subscribeDiner(id);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "식당 구독 취소"
  )
  @PostMapping("/diners/{id}/unsubscribe")
  public ResponseEntity<Void> unsubscribeDiner(
      @PathVariable long id
  ) {
    dinerService.unsubscribeDiner(id);
    return ResponseEntity.ok().build();
  }

  @ExceptionHandler(DinerMaxImageCountExceedException.class)
  public ResponseEntity<ErrorResponse> handle(DinerMaxImageCountExceedException e) {
    return ErrorResponse.badRequest(3001, e.getMessage());
  }

  @ExceptionHandler(DuplicateDinerTagException.class)
  public ResponseEntity<ErrorResponse> handle(DuplicateDinerTagException e) {
    return ErrorResponse.badRequest(3002, e.getMessage());
  }

  @ExceptionHandler(ImageWithNoExtensionException.class)
  public ResponseEntity<ErrorResponse> handle(ImageWithNoExtensionException e) {
    return ErrorResponse.badRequest(3003, e.getMessage());
  }
}
