package com.sujon.spring_data_analysis_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sujon.spring_data_analysis_api.controller.response.DataAnalysisResponse;
import com.sujon.spring_data_analysis_api.exception.BadRequestException;
import com.sujon.spring_data_analysis_api.exception.NotFoundException;
import com.sujon.spring_data_analysis_api.service.DataAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * REST endpoints for CSV data analysis.
 */
@RestController   
@RequestMapping("/api/analysis")
@CrossOrigin(
    origins = "*",
    allowedHeaders = "*",
    methods = {RequestMethod.POST, RequestMethod.GET, RequestMethod.DELETE, RequestMethod.OPTIONS}
) 
@RequiredArgsConstructor
public class DataAnalysisController {

    private final DataAnalysisService dataAnalysisService;

    @PostMapping(
            value = "/ingestCsv",
            consumes = {"text/plain", "text/csv"},
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

    // Allow download of json response
    @GetMapping("/{id}/download.json")
    public ResponseEntity<byte[]> downloadJson(@PathVariable Long id) {
        try {
            DataAnalysisResponse response = dataAnalysisService.getAnalysisById(id);
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            byte[] prettyJsonBytes = mapper.writeValueAsBytes(response);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"analysis.json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(prettyJsonBytes);

        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to generate JSON");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteAnalysisById(@PathVariable Long id) {
        dataAnalysisService.deleteAnalysisById(id);
    }
}
