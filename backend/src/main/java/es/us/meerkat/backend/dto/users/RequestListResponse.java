package es.us.meerkat.backend.dto.users;

import java.util.List;

import org.springframework.data.domain.Page;

public record RequestListResponse(List<RequestResponse> content, PageInfo page) {
    public RequestListResponse(Page<RequestResponse> page) {
        this(page.getContent(), new PageInfo(page));
    }
}
