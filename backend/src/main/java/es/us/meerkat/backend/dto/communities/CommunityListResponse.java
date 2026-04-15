package es.us.meerkat.backend.dto.communities;

import java.util.List;

import org.springframework.data.domain.Page;

import es.us.meerkat.backend.dto.users.PageInfo;

public record CommunityListResponse(List<CommunityDetailResponse> content, PageInfo page) {
    public CommunityListResponse(Page<CommunityDetailResponse> page) {
        this(page.getContent(), new PageInfo(page));
    }
}
