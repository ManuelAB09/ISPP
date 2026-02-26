package es.us.meerkat.backend.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record CommunityListResponse(List<CommunityDetailResponse> content, PageInfo page) {
    public CommunityListResponse(Page<CommunityDetailResponse> page) {
        this(page.getContent(), new PageInfo(page));
    }
}
