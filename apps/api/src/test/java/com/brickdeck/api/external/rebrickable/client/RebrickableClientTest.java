package com.brickdeck.api.external.rebrickable.client;

import com.brickdeck.api.external.rebrickable.dto.RebrickablePageResponse;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetPartResponse;
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

    @Test
    void getSetPartsReturnsMappedInventory() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RebrickableClient client = new RebrickableClient(builder.build());

        server.expect(requestTo("/lego/sets/75375-1/parts/?page=1&page_size=50"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        """
                        {
                          "count": 1,
                          "next": null,
                          "previous": null,
                          "results": [
                            {
                              "part": {
                                "part_num": "3001",
                                "name": "Brick 2 x 4",
                                "part_cat_id": 11,
                                "part_url": "https://rebrickable.com/parts/3001/",
                                "part_img_url": "https://cdn.rebrickable.com/media/parts/3001.jpg"
                              },
                              "color": {
                                "id": 4,
                                "name": "Red",
                                "rgb": "C91A09",
                                "is_trans": false
                              },
                              "quantity": 6,
                              "is_spare": false,
                              "element_id": "300121"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        RebrickablePageResponse<RebrickableSetPartResponse> response =
                client.getSetParts("75375-1", 1, 50);

        assertThat(response.count()).isEqualTo(1);
        assertThat(response.results()).hasSize(1);

        RebrickableSetPartResponse line = response.results().get(0);
        assertThat(line.quantity()).isEqualTo(6);
        assertThat(line.isSpare()).isFalse();
        assertThat(line.elementId()).isEqualTo("300121");
        assertThat(line.part().partNum()).isEqualTo("3001");
        assertThat(line.part().name()).isEqualTo("Brick 2 x 4");
        assertThat(line.part().partCatId()).isEqualTo(11);
        assertThat(line.color().id()).isEqualTo(4);
        assertThat(line.color().name()).isEqualTo("Red");
        assertThat(line.color().rgb()).isEqualTo("C91A09");
        assertThat(line.color().transparent()).isFalse();
        server.verify();
    }
}
