package com.sujon.spring_data_analysis_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sujon.spring_data_analysis_api.controller.response.DataAnalysisResponse;
import com.sujon.spring_data_analysis_api.repository.DataAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.MockMvc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DataAnalysisApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataAnalysisRepository dataAnalysisRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        dataAnalysisRepository.deleteAll();
    }

    /**
     * Verifies that the Spring application context loads successfully.
     */
    @Test
    void contextLoads() {
    }

    /**
     * Analyzes a simple CSV with no null values.
     */
    @Test
    void shouldAnalyzeSimpleCsv(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.numberOfRows()).isEqualTo(3);
        assertThat(response.numberOfColumns()).isEqualTo(3);
        assertThat(response.totalCharacters()).isEqualTo(csvData.length());
        assertThat(response.columnStatistics()).hasSize(3);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.nullCount() == 0);
    }

    /**
     * Counts null values per column correctly.
     */
    @Test
    void shouldCountNullValuesCorrectly(
            @Value("classpath:test-data/with-nulls.csv")
            Resource withNullsCsv
    ) throws Exception {
        String csvData = withNullsCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.numberOfRows()).isEqualTo(4);
        assertThat(response.numberOfColumns()).isEqualTo(4);
        assertThat(response.columnStatistics()).hasSize(4);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.nullCount() == 2)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.nullCount() == 2)
                .anyMatch(stat -> stat.columnName().equals("nationality") && stat.nullCount() == 0);
    }

    /**
     * Handles CSV with header only and no data rows.
     */
    @Test
    void shouldHandleEmptyCsvWithHeaderOnly(
            @Value("classpath:test-data/empty.csv")
            Resource emptyCsv
    ) throws Exception {
        String csvData = emptyCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.numberOfRows()).isEqualTo(0);
        assertThat(response.numberOfColumns()).isEqualTo(3);
        assertThat(response.columnStatistics()).hasSize(3);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.nullCount() == 0);
    }

    /**
     * CSV with only a single data row.
     */
    @Test
    void shouldHandleSingleRowCsv(
            @Value("classpath:test-data/single-row.csv")
            Resource singleRowCsv
    ) throws Exception {
        String csvData = singleRowCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.numberOfRows()).isEqualTo(1);
        assertThat(response.numberOfColumns()).isEqualTo(3);
        assertThat(response.columnStatistics()).hasSize(3);
    }

    /**
     * Tests parsing a larger CSV dataset with multiple rows and columns.
     */
    @Test
    void shouldHandleLargeCsv(
            @Value("classpath:test-data/large.csv")
            Resource largeCsv
    ) throws Exception {
        String csvData = largeCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.numberOfRows()).isEqualTo(10);
        assertThat(response.numberOfColumns()).isEqualTo(6);
        assertThat(response.totalCharacters()).isEqualTo(csvData.length());
        assertThat(response.columnStatistics()).hasSize(6);
    }

    /**
     * Tests CSV parsing with a realistic dataset containing scattered null values.
     */
    @Test
    void shouldHandleMixedNullValues(
            @Value("classpath:test-data/mixed-nulls.csv")
            Resource mixedNullsCsv
    ) throws Exception {
        String csvData = mixedNullsCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.numberOfRows()).isEqualTo(6);
        assertThat(response.numberOfColumns()).isEqualTo(5);
        assertThat(response.columnStatistics()).hasSize(5);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.nullCount() == 1)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.nullCount() == 3)
                .anyMatch(stat -> stat.columnName().equals("nationality") && stat.nullCount() == 1)
                .anyMatch(stat -> stat.columnName().equals("podiums") && stat.nullCount() == 0);
    }

    /**
     * Verifies that the analysis results are persisted to the H2 database.
     */
    @Test
    void shouldPersistDataToDatabase(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        assertThat(dataAnalysisRepository.count()).isEqualTo(1);
    }

    /**
     * Verifies that the persisted database entity contains correct analysis values.
     */
    @Test
    void shouldPersistCorrectAnalysisData(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        var entities = dataAnalysisRepository.findAll();
        assertThat(entities).hasSize(1);

        var entity = entities.get(0);
        assertThat(entity.getNumberOfRows()).isEqualTo(3);
        assertThat(entity.getNumberOfColumns()).isEqualTo(3);
        assertThat(entity.getTotalCharacters()).isEqualTo(csvData.length());
        assertThat(entity.getOriginalData()).isEqualTo(csvData);
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    /**
     * Verifies that column statistics child entities are persisted correctly.
     */
    @Test
    void shouldPersistColumnStatisticsEntities(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        var entities = dataAnalysisRepository.findAll();
        assertThat(entities).hasSize(1);

        var entity = entities.get(0);
        assertThat(entity.getColumnStatistics()).hasSize(3);
        assertThat(entity.getColumnStatistics())
                .anyMatch(stat -> stat.getColumnName().equals("driver") && stat.getNullCount() == 0)
                .anyMatch(stat -> stat.getColumnName().equals("number") && stat.getNullCount() == 0)
                .anyMatch(stat -> stat.getColumnName().equals("team") && stat.getNullCount() == 0);

        entity.getColumnStatistics().forEach(stat ->
                assertThat(stat.getDataAnalysis()).isEqualTo(entity)
        );
    }

    /**
     * Tests that multiple CSV files can be ingested sequentially.
     */
    @Test
    void shouldHandleMultipleIngestRequests(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv,
            @Value("classpath:test-data/with-nulls.csv")
            Resource withNullsCsv
    ) throws Exception {
        String csvData1 = simpleCsv.getContentAsString(UTF_8);
        String csvData2 = withNullsCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData2))
                .andExpect(status().isOk());

        assertThat(dataAnalysisRepository.count()).isEqualTo(2);
    }

    /**
     * Tests error handling for malformed CSV input.
     */
    @Test
    void shouldReturnBadRequestForInvalidCsv(
            @Value("classpath:test-data/invalid.csv")
            Resource invalidCsv
    ) throws Exception {
        String csvData = invalidCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests content validation: rejecting CSV data containing "Sonny Hayes".
     */
    @Test
    void shouldRejectCsvContainingSonnyHayes(
            @Value("classpath:test-data/sonny-hayes.csv")
            Resource sonnyHayesCsv
    ) throws Exception {
        String csvData = sonnyHayesCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isBadRequest());
        assertThat(dataAnalysisRepository.count()).isEqualTo(0);
    }

    /**
     * Tests error handling for completely empty input.
     */
    @Test
    void shouldReturnBadRequestForEmptyInput() throws Exception {
        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(""))
                .andExpect(status().isBadRequest());
    }
}
