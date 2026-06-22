package ev_charger.be.notice;

import ev_charger.be.notice.dto.request.NoticeCreateRequest;
import ev_charger.be.notice.dto.response.NoticeListResponse;
import ev_charger.be.notice.dto.response.NoticeResponse;
import ev_charger.be.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notices")
public class NoticeController {

    private final NoticeService noticeService;

    // 공지 등록
    @PreAuthorize("hasRole('ADMIN')") // 관리자 권한 확인
    @PostMapping("")
    public ResponseEntity<Void> createNotice(
            @RequestBody NoticeCreateRequest request // title, content
    ) {
        noticeService.createNotice(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 단일 공지 조회
     * @param userDetails
     * @param noticeId
     * @return
     */
    @GetMapping("{noticeId}")
    public ResponseEntity<NoticeResponse> getNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("noticeId") Long noticeId) {
        return ResponseEntity.ok(noticeService.getNotice(userDetails.getUser(), noticeId));
    }

    /**
     * 공지 리스트 조회
     * @param userDetails
     * @param pageable
     * @return
     */
    @GetMapping("")
    public ResponseEntity<Page<NoticeListResponse>> getAllNotices(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(sort="createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(noticeService.getNotices(userDetails.getUser(), pageable));
    }
}
