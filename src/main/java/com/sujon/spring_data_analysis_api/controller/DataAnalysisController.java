package com.sujon.spring_data_analysis_api.controller;

import com.sujon.spring_data_analysis_api.controller.response.DataAnalysisResponse;
import com.sujon.spring_data_analysis_api.exception.BadRequestException;
import com.sujon.spring_data_analysis_api.service.DataAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * REST endpoints for CSV data analysis.
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class DataAnalysisController {

    private final DataAnalysisService dataAnalysisService;

    @PostMapping(
            value = "/ingestCsv",
            consumes = "text/plain",
            produces = "application/json"
    )
    public DataAnalysisResponse ingestAndAnalyzeCsv(@RequestBody String data) {
        if (data.contains("Sonny Hayes")) {
            throw new BadRequestException("CSV data containing 'Sonny Hayes' is not allowed");
        }
        return dataAnalysisService.analyzeCsvData(data);
    }

    @GetMapping("/{id}")
    public DataAnalysisResponse getAnalysisById(@PathVariable Long id) {
        return dataAnalysisService.getAnalysisById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteAnalysisById(@PathVariable Long id) {
        dataAnalysisService.deleteAnalysisById(id);
    }
}