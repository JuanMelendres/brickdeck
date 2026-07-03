package com.brickdeck.api.external.rebrickable.client;

import com.brickdeck.api.external.rebrickable.dto.RebrickableThemeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class RebrickableClientTest {

    @Test
    void getThemeByIdReturnsMappedTheme() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RebrickableClient client = new RebrickableClient(builder.build());

        server.expect(requestTo("/lego/themes/158/"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        "{\"id\":158,\"name\":\"Star Wars\",\"parent_id\":null}",
                        MediaType.APPLICATION_JSON));

        RebrickableThemeResponse response = client.getThemeById(158);

        assertThat(response.id()).isEqualTo(158);
        assertThat(response.name()).isEqualTo("Star Wars");
        assertThat(response.parentId()).isNull();
        server.verify();
    }
}
