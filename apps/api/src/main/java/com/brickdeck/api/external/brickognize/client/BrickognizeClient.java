package com.brickdeck.api.external.brickognize.client;

import com.brickdeck.api.external.brickognize.dto.BrickognizePredictResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BrickognizeClient {

    private final RestClient brickognizeRestClient;

    public BrickognizeClient(RestClient brickognizeRestClient) {
        this.brickognizeRestClient = brickognizeRestClient;
    }

    public BrickognizePredictResponse identifyPart(byte[] imageBytes) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("query_image", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "photo.jpg";
            }
        });

        return brickognizeRestClient
                .post()
                .uri("/predict/parts/")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(bodyBuilder.build())
                .retrieve()
                .body(BrickognizePredictResponse.class);
    }
}
