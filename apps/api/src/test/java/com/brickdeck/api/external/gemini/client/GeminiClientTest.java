package com.brickdeck.api.external.gemini.client;

import com.brickdeck.api.external.gemini.dto.GeminiColorGuess;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    @Test
    void guessColorReturnsMappedGuess() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiClient client = new GeminiClient(builder.build(), new ObjectMapper(), "gemini-flash-latest", "test-key");

        server.expect(requestTo("/models/gemini-flash-latest:generateContent?key=test-key"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        """
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "{\\"colorName\\":\\"Red\\",\\"confidence\\":0.95,\\"reasoning\\":\\"solid red brick\\"}"
                                  }
                                ]
                              }
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        GeminiColorGuess guess = client.guessColor("photo".getBytes(), "image/jpeg", List.of("Red", "Blue"));

        assertThat(guess.colorName()).isEqualTo("Red");
        assertThat(guess.confidence()).isEqualTo(0.95);
        assertThat(guess.reasoning()).isEqualTo("solid red brick");
        server.verify();
    }
}
