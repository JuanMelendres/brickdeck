package com.brickdeck.api.external.brickognize.client;

import com.brickdeck.api.external.brickognize.dto.BrickognizePredictResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrickognizeClientTest {

    @Test
    void identifyPartReturnsMappedCandidates() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrickognizeClient client = new BrickognizeClient(builder.build());

        server.expect(requestTo("/predict/parts/"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        """
                        {
                          "items": [
                            {
                              "id": "3001",
                              "name": "Brick 2 x 4",
                              "score": 0.84,
                              "category": "Brick"
                            },
                            {
                              "id": "3001special",
                              "name": "Brick 2 x 4 special",
                              "score": 0.52,
                              "category": "Brick"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        BrickognizePredictResponse response = client.identifyPart("photo.jpg".getBytes());

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).id()).isEqualTo("3001");
        assertThat(response.items().get(0).name()).isEqualTo("Brick 2 x 4");
        assertThat(response.items().get(0).score()).isEqualTo(0.84);
        assertThat(response.items().get(0).category()).isEqualTo("Brick");
        server.verify();
    }
}
