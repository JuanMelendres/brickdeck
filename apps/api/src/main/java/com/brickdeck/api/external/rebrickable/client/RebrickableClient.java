package com.brickdeck.api.external.rebrickable.client;

import com.brickdeck.api.external.rebrickable.dto.RebrickablePageResponse;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetPartResponse;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetResponse;
import com.brickdeck.api.external.rebrickable.dto.RebrickableThemeResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RebrickableClient {

    private final RestClient rebrickableRestClient;

    public RebrickableClient(RestClient rebrickableRestClient) {
        this.rebrickableRestClient = rebrickableRestClient;
    }

    public RebrickableSetResponse getSetByNumber(String setNumber) {
        return rebrickableRestClient
                .get()
                .uri("/lego/sets/{setNumber}/", setNumber)
                .retrieve()
                .body(RebrickableSetResponse.class);
    }

    public RebrickableThemeResponse getThemeById(Integer themeId) {
        return rebrickableRestClient
                .get()
                .uri("/lego/themes/{themeId}/", themeId)
                .retrieve()
                .body(RebrickableThemeResponse.class);
    }

    public RebrickablePageResponse<RebrickableSetResponse> searchSets(String search, int page, int pageSize) {
        return rebrickableRestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/lego/sets/")
                        .queryParam("search", search)
                        .queryParam("page", page)
                        .queryParam("page_size", pageSize)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public RebrickablePageResponse<RebrickableSetPartResponse> getSetParts(String setNumber, int page, int pageSize) {
        return rebrickableRestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/lego/sets/{setNumber}/parts/")
                        .queryParam("page", page)
                        .queryParam("page_size", pageSize)
                        .build(setNumber))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}