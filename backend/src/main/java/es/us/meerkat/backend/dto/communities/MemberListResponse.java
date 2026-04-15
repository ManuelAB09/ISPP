package es.us.meerkat.backend.dto.communities;

import java.util.List;

import org.springframework.data.domain.Page;

import es.us.meerkat.backend.dto.users.PageInfo;

public record MemberListResponse(List<MemberResponse> content, PageInfo page) {
    public MemberListResponse(Page<MemberResponse> page) {
        this(page.getContent(), new PageInfo(page));
    }
}
