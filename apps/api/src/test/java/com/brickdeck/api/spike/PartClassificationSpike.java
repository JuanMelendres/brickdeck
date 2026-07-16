package com.brickdeck.api.spike;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Phase 7 spike POC — THROWAWAY. Not production code, not a real test.
 *
 * <p>Validates two things the spike's recommendation rests on:
 * 1. anthropic-java can do vision + structured outputs from Spring Boot (no Python service).
 * 2. Whether accuracy clears the gate: top-5 part >= 60%, color >= 80%.
 *
 * <p>Requires ANTHROPIC_API_KEY and EVAL_SET_DIR (images + ground_truth.csv).
 * Skipped automatically when the key is absent.
 *
 * <p>Run: ./mvnw test -Dtest=PartClassificationSpike
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class PartClassificationSpike {

    private static final int TOP_N = 5;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private record Truth(String filename, String partNum, int colorId, String difficulty) {}

    private record Candidate(String partNumber, String partName, int colorId, double confidence) {}

    @Test
    void classifyEvalSet() throws Exception {
        Path evalDir = Path.of(System.getenv().getOrDefault("EVAL_SET_DIR", "eval_set"));
        List<Truth> truths = readGroundTruth(evalDir.resolve("ground_truth.csv"));
        Map<Integer, String> colors = fetchRebrickableColors();

        System.out.printf("%nEval set: %d images | color enum: %d values | model: claude-opus-4-8%n%n",
                truths.size(), colors.size());

        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        JsonOutputFormat format = buildSchema(colors.keySet());

        int top1Part = 0, topNPart = 0, colorHits = 0;
        long totalIn = 0, totalOut = 0;
        List<String> misses = new ArrayList<>();
        long started = System.currentTimeMillis();

        for (Truth t : truths) {
            byte[] image = Files.readAllBytes(evalDir.resolve(t.filename()));
            String b64 = Base64.getEncoder().encodeToString(image);

            MessageCreateParams params = MessageCreateParams.builder()
                    .model(Model.CLAUDE_OPUS_4_8)
                    .maxTokens(4096L)
                    .thinking(ThinkingConfigAdaptive.builder().build())
                    .outputConfig(OutputConfig.builder()
                            .effort(OutputConfig.Effort.HIGH)
                            .format(format)
                            .build())
                    .system(systemPrompt())
                    .addUserMessageOfBlockParams(List.of(
                            ContentBlockParam.ofImage(ImageBlockParam.builder()
                                    .source(Base64ImageSource.builder()
                                            .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                                            .data(b64)
                                            .build())
                                    .build()),
                            ContentBlockParam.ofText(TextBlockParam.builder()
                                    .text("Identify this LEGO part. Return up to " + TOP_N
                                            + " candidates, most likely first.")
                                    .build())))
                    .build();

            Message response = client.messages().create(params);
            totalIn += response.usage().inputTokens();
            totalOut += response.usage().outputTokens();

            List<Candidate> candidates = parseCandidates(response);
            if (candidates.isEmpty()) {
                misses.add(t.filename() + " -> no candidates returned");
                continue;
            }

            boolean top1 = candidates.get(0).partNumber().equalsIgnoreCase(t.partNum());
            boolean topN = candidates.stream().limit(TOP_N)
                    .anyMatch(c -> c.partNumber().equalsIgnoreCase(t.partNum()));
            boolean color = candidates.get(0).colorId() == t.colorId();

            if (top1) top1Part++;
            if (topN) topNPart++;
            if (color) colorHits++;

            System.out.printf("%-12s %-7s want part=%-7s color=%-4d | got %-7s color=%-4d conf=%.2f  %s%s%n",
                    t.filename(), t.difficulty(), t.partNum(), t.colorId(),
                    candidates.get(0).partNumber(), candidates.get(0).colorId(),
                    candidates.get(0).confidence(),
                    top1 ? "TOP1" : (topN ? "top" + TOP_N : "MISS"),
                    color ? " color-ok" : " COLOR-MISS");

            if (!topN) {
                misses.add("%s: wanted %s, got %s".formatted(
                        t.filename(), t.partNum(),
                        candidates.stream().map(Candidate::partNumber).toList()));
            }
        }

        int n = truths.size();
        double top1Pct = pct(top1Part, n), topNPct = pct(topNPart, n), colorPct = pct(colorHits, n);
        long elapsed = (System.currentTimeMillis() - started) / 1000;

        System.out.printf("%n=== RESULTS (n=%d, %ds) ===%n", n, elapsed);
        System.out.printf("top-1 part : %5.1f%% (%d/%d)%n", top1Pct, top1Part, n);
        System.out.printf("top-%d part : %5.1f%% (%d/%d)   gate: >= 60%%  %s%n",
                TOP_N, topNPct, topNPart, n, topNPct >= 60 ? "PASS" : "FAIL");
        System.out.printf("color      : %5.1f%% (%d/%d)   gate: >= 80%%  %s%n",
                colorPct, colorHits, n, colorPct >= 80 ? "PASS" : "FAIL");
        System.out.printf("tokens     : in=%d out=%d (~%.1f in/image)%n", totalIn, totalOut, (double) totalIn / n);
        System.out.printf("%nGATE: %s%n", (topNPct >= 60 && colorPct >= 80) ? "PASS" : "FAIL");

        if (!misses.isEmpty()) {
            System.out.printf("%nMisses (%d):%n", misses.size());
            misses.forEach(m -> System.out.println("  " + m));
        }
        System.out.println("\nNOTE: renders, not photos. This is the smoke-test upper bound, NOT the gate.");
    }

    private static String systemPrompt() {
        return """
                You identify LEGO parts from images for a catalog keyed on Rebrickable.

                Return up to %d candidates ranked most-likely first. Prefer recall over
                precision: a human confirms your answer, so an extra plausible candidate
                is cheap and a missing correct one is expensive.

                partNumber must be a Rebrickable part number (e.g. "3001", "3020", "3069b") —
                NOT a LEGO element ID or design ID. colorId must be a Rebrickable color id
                from the allowed enum.

                confidence is 0.0-1.0, your genuine belief the candidate is correct.
                Do not inflate it; a well-calibrated 0.4 is more useful than a false 0.9.
                """.formatted(TOP_N);
    }

    /** Freeform JSON-schema map. Color is enum-constrained; part number cannot be (~60k parts). */
    private static JsonOutputFormat buildSchema(Iterable<Integer> colorIds) {
        List<Integer> ids = new ArrayList<>();
        colorIds.forEach(ids::add);

        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("type", "object");
        candidate.put("properties", Map.of(
                "partNumber", Map.of("type", "string", "description", "Rebrickable part number"),
                "partName", Map.of("type", "string"),
                "colorId", Map.of("type", "integer", "enum", ids),
                "confidence", Map.of("type", "number"),
                "reasoning", Map.of("type", "string")));
        candidate.put("required", List.of("partNumber", "partName", "colorId", "confidence", "reasoning"));
        candidate.put("additionalProperties", false);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "object");
        root.put("properties", Map.of("candidates", Map.of(
                "type", "array", "items", candidate, "maxItems", TOP_N)));
        root.put("required", List.of("candidates"));
        root.put("additionalProperties", false);

        JsonOutputFormat.Schema.Builder schema = JsonOutputFormat.Schema.builder();
        root.forEach((k, v) -> schema.putAdditionalProperty(k, JsonValue.from(v)));
        return JsonOutputFormat.builder().schema(schema.build()).build();
    }

    private static List<Candidate> parseCandidates(Message response) throws IOException {
        String json = response.content().stream()
                .flatMap(b -> b.text().stream())
                .map(t -> t.text())
                .reduce("", String::concat);
        if (json.isBlank()) return List.of();

        List<Candidate> out = new ArrayList<>();
        for (JsonNode c : MAPPER.readTree(json).path("candidates")) {
            out.add(new Candidate(
                    c.path("partNumber").asText(),
                    c.path("partName").asText(),
                    c.path("colorId").asInt(-1),
                    c.path("confidence").asDouble(0)));
        }
        return out;
    }

    private static List<Truth> readGroundTruth(Path csv) throws IOException {
        List<Truth> out = new ArrayList<>();
        List<String> lines = Files.readAllLines(csv);
        for (String line : lines.subList(1, lines.size())) {
            String[] f = line.split(",", -1);
            // filename,element_id,part_num,color_id,difficulty,note
            out.add(new Truth(f[0], f[2], Integer.parseInt(f[3]), f[4]));
        }
        return out;
    }

    /** Production would seed this from our colors table; it is empty locally, so use the source. */
    private static Map<Integer, String> fetchRebrickableColors() throws Exception {
        String base = System.getenv("REBRICKABLE_BASE_URL");
        String key = System.getenv("REBRICKABLE_API_KEY");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/lego/colors/?page_size=1000"))
                .header("Authorization", "key " + key)
                .timeout(Duration.ofSeconds(20))
                .build();
        HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        Map<Integer, String> colors = new LinkedHashMap<>();
        for (JsonNode c : MAPPER.readTree(res.body()).path("results")) {
            colors.put(c.path("id").asInt(), c.path("name").asText());
        }
        return colors;
    }

    private static double pct(int hits, int total) {
        return total == 0 ? 0 : (hits * 100.0) / total;
    }
}
