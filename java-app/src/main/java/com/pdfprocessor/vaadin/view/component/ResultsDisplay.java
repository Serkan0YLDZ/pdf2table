package com.pdfprocessor.vaadin.view.component;

import com.pdfprocessor.entity.AnalysisFile;
import com.pdfprocessor.entity.Document;
import com.pdfprocessor.service.AnalysisService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

/**
 * Results display component for showing analysis results
 */
public class ResultsDisplay {
    
    /**
     * Refresh the results display with current analysis results
     */
    public static void refreshResultsDisplay(Div resultsArea, Document selectedDocument, 
                                           String analysisType, AnalysisService analysisService) {
        try {
            // Clear existing content
            resultsArea.removeAll();
            
            // Get analysis results
            List<AnalysisFile> results = 
                analysisService.getAnalysisResults(selectedDocument.getId(), analysisType);
            
            if (results.isEmpty()) {
                resultsArea.add(createNoResultsState());
            } else {
                resultsArea.add(createResultsHeader(results.size()));
                resultsArea.add(createImageContainer(results, selectedDocument, analysisType));
            }
            
        } catch (Exception e) {
            resultsArea.add(createErrorState(e.getMessage()));
        }
    }
    
    private static Component createNoResultsState() {
        Span noResults = new Span("No analysis results found. Click 'Check Results' after analysis completes.");
        noResults.getStyle().set("color", "var(--lumo-contrast-50pct)");
        noResults.getStyle().set("font-size", "var(--lumo-font-size-s)");
        noResults.getStyle().set("font-style", "italic");
        return noResults;
    }
    
    private static Component createResultsHeader(int resultCount) {
        Span resultsHeader = new Span("Analysis Results (" + resultCount + " pages):");
        resultsHeader.getStyle().set("font-weight", "bold");
        resultsHeader.getStyle().set("margin-bottom", "0.5rem");
        resultsHeader.getStyle().set("display", "block");
        return resultsHeader;
    }
    
    private static Component createImageContainer(List<AnalysisFile> results, Document selectedDocument, String analysisType) {
        Div imageContainer = new Div();
        imageContainer.getStyle().set("max-height", "450px");
        imageContainer.getStyle().set("overflow-y", "auto");
        imageContainer.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        imageContainer.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        imageContainer.getStyle().set("padding", "0.5rem");
        imageContainer.getStyle().set("background", "var(--lumo-base-color)");
        
        // Sort results by page number
        results.sort((a, b) -> Integer.compare(a.getPageNumber(), b.getPageNumber()));
        
        for (AnalysisFile result : results) {
            imageContainer.add(createPageResult(result, selectedDocument, analysisType));
        }
        
        return imageContainer;
    }
    
    private static Component createPageResult(AnalysisFile result, Document selectedDocument, String analysisType) {
        Div pageContainer = new Div();
        pageContainer.getStyle().set("margin-bottom", "1rem");
        pageContainer.getStyle().set("text-align", "center");
        
        // Page header
        Div pageHeader = new Div();
        pageHeader.getStyle().set("margin-bottom", "0.5rem");
        pageHeader.getStyle().set("font-weight", "bold");
        pageHeader.getStyle().set("color", "var(--lumo-contrast-70pct)");
        pageHeader.setText("Page " + result.getPageNumber());
        
        // Create image element
        Image analysisImage = new Image();
        String imageUrl = "/api/files/analysis/" + selectedDocument.getId() + "/" + analysisType + "/" + result.getPageNumber();
        analysisImage.setSrc(imageUrl);
        analysisImage.setAlt("Analysis result for page " + result.getPageNumber());
        
        // Set image styles
        analysisImage.getStyle().set("max-width", "100%");
        analysisImage.getStyle().set("height", "auto");
        analysisImage.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        analysisImage.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        analysisImage.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");
        
        // Add error handling for image loading
        analysisImage.getElement().addEventListener("error", e -> {
            Div errorDiv = new Div();
            errorDiv.getStyle().set("padding", "2rem");
            errorDiv.getStyle().set("text-align", "center");
            errorDiv.getStyle().set("color", "var(--lumo-contrast-50pct)");
            errorDiv.getStyle().set("background", "var(--lumo-contrast-5pct)");
            errorDiv.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
            errorDiv.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
            errorDiv.setText("Image not available");
            pageContainer.removeAll();
            pageContainer.add(pageHeader, errorDiv);
        });
        
        pageContainer.add(pageHeader, analysisImage);
        return pageContainer;
    }
    
    private static Component createErrorState(String errorMessage) {
        Span errorSpan = new Span("Error loading results: " + errorMessage);
        errorSpan.getStyle().set("color", "var(--lumo-error-color)");
        return errorSpan;
    }
}
