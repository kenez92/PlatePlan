package com.kenez92.plateplan.plan.controller.dto;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanFilesDtoTest {

    @Test
    void shouldSerializeSuccessAsTwoBase64PdfFieldsWithoutError() {
        final byte[] dietPdf = "%PDF-1.4 diet".getBytes(StandardCharsets.ISO_8859_1);
        final byte[] shoppingListPdf = "%PDF-1.4 list".getBytes(StandardCharsets.ISO_8859_1);
        final JsonMapper mapper = JsonMapper.builder().build();

        final JsonNode actual = mapper.readTree(mapper.writeValueAsString(PlanFilesDto.success(dietPdf, shoppingListPdf)));

        assertThat(actual.has("error")).isFalse();
        assertThat(actual.propertyNames()).containsExactly("dietPdf", "shoppingListPdf");
        assertThat(Base64.getDecoder().decode(actual.get("dietPdf").asString()))
                .startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(Base64.getDecoder().decode(actual.get("shoppingListPdf").asString()))
                .startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void shouldSerializeFailureAsOnlyTheErrorField() {
        final JsonMapper mapper = JsonMapper.builder().build();

        assertThat(mapper.writeValueAsString(PlanFilesDto.unavailable())).isEqualTo("{\"error\":\"UNAVAILABLE\"}");
        assertThat(mapper.writeValueAsString(PlanFilesDto.profileRequired()))
                .isEqualTo("{\"error\":\"PROFILE_REQUIRED\"}");
        assertThat(mapper.writeValueAsString(PlanFilesDto.caloriesRequired()))
                .isEqualTo("{\"error\":\"CALORIES_REQUIRED\"}");
    }

    @Test
    void shouldMatchTheSuccessAndFailureRecordsByRecursiveComparison() {
        final byte[] dietPdf = "%PDF-1.4 diet".getBytes(StandardCharsets.ISO_8859_1);
        final byte[] shoppingListPdf = "%PDF-1.4 list".getBytes(StandardCharsets.ISO_8859_1);

        assertThat(PlanFilesDto.success(dietPdf, shoppingListPdf)).usingRecursiveComparison()
                .isEqualTo(new PlanFilesDto(dietPdf, shoppingListPdf, null));
        assertThat(PlanFilesDto.unavailable()).usingRecursiveComparison()
                .isEqualTo(new PlanFilesDto(null, null, "UNAVAILABLE"));
        assertThat(PlanFilesDto.profileRequired()).usingRecursiveComparison()
                .isEqualTo(new PlanFilesDto(null, null, "PROFILE_REQUIRED"));
        assertThat(PlanFilesDto.caloriesRequired()).usingRecursiveComparison()
                .isEqualTo(new PlanFilesDto(null, null, "CALORIES_REQUIRED"));
    }

    @Test
    void shouldOmitBytesFromToString() {
        final byte[] dietPdf = new byte[] {1, 2, 3};
        final byte[] shoppingListPdf = new byte[] {4, 5, 6};

        final String actual = PlanFilesDto.success(dietPdf, shoppingListPdf).toString();

        assertThat(actual).doesNotContain(Arrays.toString(dietPdf), Arrays.toString(shoppingListPdf));
        assertThat(PlanFilesDto.unavailable().toString())
                .contains("UNAVAILABLE")
                .doesNotContain(Arrays.toString(dietPdf), Arrays.toString(shoppingListPdf));
    }

    @Test
    void shouldRejectSuccessWithoutBothPdfFields() {
        final byte[] dietPdf = "%PDF-1.4 diet".getBytes(StandardCharsets.ISO_8859_1);

        assertThatThrownBy(() -> new PlanFilesDto(dietPdf, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlanFilesDto(null, dietPdf, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
