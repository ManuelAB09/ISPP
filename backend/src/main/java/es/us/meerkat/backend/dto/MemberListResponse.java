package es.us.meerkat.backend.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record MemberListResponse(
    List<MemberResponse> content,
    PageInfo page
) {
    public MemberListResponse(Page<MemberResponse> page) {
        this(page.getContent(), new PageInfo(page));
    }
}
