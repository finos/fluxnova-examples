package co.summit58.feewaiver.sb.controllers;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import co.summit58.feewaiver.sb.models.*;
import co.summit58.feewaiver.sb.services.*;

@RestController
@RequestMapping("/analyze")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisPersistenceService analysisPersistenceService;

    public AnalysisController(AnalysisService analysisService,
                                AnalysisPersistenceService analysisPersistenceService) {
        this.analysisService = analysisService;
        this.analysisPersistenceService = analysisPersistenceService;
    }

    @PostMapping
    public AnalyzeResponse analyze(@Valid @RequestBody AnalyzeRequest request) throws Exception {
        return analysisService.analyze(request);
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @GetMapping
    public List<AnalysisRecordResponse> findAll() throws Exception {
        return analysisPersistenceService.findAll();
    }

    @GetMapping("/{id}")
    public AnalysisRecordResponse findById(@PathVariable("id") Long id) throws Exception {
        return analysisPersistenceService.findById(id);
    }

    @PostMapping("/{id}/review")
    public ReviewResponse review(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReviewRequest request) {
        return analysisPersistenceService.review(id, request);
    }

    @GetMapping("/pending-review")
    public List<AnalysisRecordResponse> findPendingReview() throws Exception {
        return analysisPersistenceService.findPendingReview();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception e) {
        return new ErrorResponse("ANALYSIS_ERROR", e.getMessage());
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(Exception e) {
        return new ErrorResponse("VALIDATION_ERROR", "Invalid request payload");
    }

    public record ErrorResponse(String code, String message) {}
}