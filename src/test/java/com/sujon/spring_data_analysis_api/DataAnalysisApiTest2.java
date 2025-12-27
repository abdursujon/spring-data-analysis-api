package com.sujon.spring_data_analysis_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sujon.spring_data_analysis_api.controller.response.DataAnalysisResponse;
import com.sujon.spring_data_analysis_api.repository.DataAnalysisRepository;
import com.sujon.spring_data_analysis_api.repository.entity.DataAnalysisEntity;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extend API Functionality
 */
@SpringBootTest
@AutoConfigureMockMvc
class DataAnalysisApiTest2 {

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
     * Tests that unique value counts are calculated correctly for a simple CSV.
     */
    @Test
    void shouldCalculateUniqueCountsForSimpleCsv(
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

        assertThat(response.columnStatistics()).hasSize(3);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.uniqueCount() == 3)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.uniqueCount() == 3)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.uniqueCount() == 3);
    }

    /**
     * Tests unique count calculation when there are duplicate values.
     */
    @Test
    void shouldCalculateUniqueCountsWithDuplicates(
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

        assertThat(response.columnStatistics()).hasSize(6);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.uniqueCount() == 10)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.uniqueCount() == 6);
    }

    /**
     * Tests that null/empty values are excluded from unique counts.
     */
    @Test
    void shouldExcludeNullsFromUniqueCount(
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

        assertThat(response.columnStatistics()).hasSize(4);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.uniqueCount() == 4)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.uniqueCount() == 2)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.uniqueCount() == 2)
                .anyMatch(stat -> stat.columnName().equals("nationality") && stat.uniqueCount() == 3);
    }

    /**
     * Tests unique count for an empty CSV (header only, no data rows).
     */
    @Test
    void shouldReturnZeroUniqueCountForEmptyCsv(
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

        assertThat(response.columnStatistics()).hasSize(3);
        assertThat(response.columnStatistics())
                .allMatch(stat -> stat.uniqueCount() == 0);
    }

    /**
     * Tests that unique counts are persisted correctly to the database.
     */
    @Test
    void shouldPersistUniqueCountsToDatabase(
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
                .anyMatch(stat -> stat.getColumnName().equals("driver") && stat.getUniqueCount() == 3)
                .anyMatch(stat -> stat.getColumnName().equals("number") && stat.getUniqueCount() == 3)
                .anyMatch(stat -> stat.getColumnName().equals("team") && stat.getUniqueCount() == 3);
    }

    /**
     * Tests retrieving a previously analyzed CSV by its ID.
     */
    @Test
    void shouldRetrievePreviousAnalysisById(
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
        Long analysisId = entities.get(0).getId();

        var result = mockMvc.perform(get("/api/analysis/{id}", analysisId))
                .andDo(print())
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
    }

    /**
     * Tests that GET endpoint returns 404 for non-existent analysis ID.
     */
    @Test
    void shouldReturn404ForNonExistentAnalysis() throws Exception {
        mockMvc.perform(get("/api/analysis/{id}", 999L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    /**
     * Tests retrieving multiple analyses to ensure each can be fetched independently.
     */
    @Test
    void shouldRetrieveMultipleAnalysesIndependently(
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
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData2))
                .andDo(print())
                .andExpect(status().isOk());

        var entities = dataAnalysisRepository.findAll();
        assertThat(entities).hasSize(2);

        Long id1 = entities.get(0).getId();
        var result1 = mockMvc.perform(get("/api/analysis/{id}", id1))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        Long id2 = entities.get(1).getId();
        var result2 = mockMvc.perform(get("/api/analysis/{id}", id2))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response1.numberOfRows()).isEqualTo(3);
        assertThat(response2.numberOfRows()).isEqualTo(4);
    }

    /**
     * Tests deleting an analysis by its ID.
     */
    @Test
    void shouldDeleteAnalysisById(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andDo(print())
                .andExpect(status().isOk());

        var entities = dataAnalysisRepository.findAll();
        assertThat(entities).hasSize(1);
        Long analysisId = entities.get(0).getId();

        mockMvc.perform(delete("/api/analysis/{id}", analysisId))
                .andExpect(status().isNoContent());

        assertThat(dataAnalysisRepository.count()).isEqualTo(0);

        mockMvc.perform(get("/api/analysis/{id}", analysisId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    /**
     * Tests that DELETE endpoint returns 404 for non-existent analysis ID.
     */
    @Test
    void shouldReturn404WhenDeletingNonExistentAnalysis(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        assertThat(dataAnalysisRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/api/analysis/{id}", 999L))
                .andExpect(status().isNotFound());

        assertThat(dataAnalysisRepository.count()).isEqualTo(1);
    }

    /**
     * Tests deleting one analysis doesn't affect others.
     */
    @Test
    void shouldDeleteOnlySpecifiedAnalysis(
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

        var entities = dataAnalysisRepository.findAll();
        assertThat(entities).hasSize(2);

        Long id1 = entities.get(0).getId();
        Long id2 = entities.get(1).getId();

        mockMvc.perform(delete("/api/analysis/{id}", id1))
                .andExpect(status().isNoContent());

        assertThat(dataAnalysisRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/analysis/{id}", id2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/analysis/{id}", id1))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests that deleting an analysis also deletes its associated column statistics.
     */
    @Test
    void shouldCascadeDeleteColumnStatistics(
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

        DataAnalysisEntity entity = entities.get(0);
        Long analysisId = entity.getId();

        assertThat(entity.getColumnStatistics()).hasSize(3);

        mockMvc.perform(delete("/api/analysis/{id}", analysisId))
                .andExpect(status().isNoContent());

        assertThat(dataAnalysisRepository.count()).isEqualTo(0);
    }
}
