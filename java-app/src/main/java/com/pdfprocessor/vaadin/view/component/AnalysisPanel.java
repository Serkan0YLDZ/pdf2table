package com.pdfprocessor.vaadin.view.component;

import com.pdfprocessor.entity.Document;
import com.pdfprocessor.service.AnalysisService;
import com.pdfprocessor.service.PdfMergeService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

/**
 * Analysis panel component for starting and checking analysis
 */
public class AnalysisPanel {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalysisPanel.class);
    
    public static Component create(String title, String analysisType, Document selectedDocument, AnalysisService analysisService) {
        VerticalLayout column = new VerticalLayout();
        column.setSpacing(true);
        column.setPadding(true);
        column.setWidthFull();
        column.setHeightFull();
        
        // Modern styling with improved spacing and colors
        column.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        column.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        column.getStyle().set("background", "var(--lumo-base-color)");
        column.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        column.getStyle().set("transition", "all 0.2s ease");
        
        // Header
        column.add(createHeader(title));
        
        if (selectedDocument == null) {
            column.add(createEmptyState());
        } else {
            Div resultsArea = createResultsArea(analysisType);
            Component controls = createControls(analysisType, selectedDocument, analysisService, resultsArea);
            column.add(controls, (Component) resultsArea);
        }
        
        return column;
    }
    
    private static Component createHeader(String title) {
        Div header = new Div();
        header.getStyle().set("margin-bottom", "1.5rem");
        header.getStyle().set("padding-bottom", "1rem");
        header.getStyle().set("border-bottom", "2px solid var(--lumo-primary-color-10pct)");
        
        H4 columnTitle = new H4(title);
        columnTitle.getStyle().set("margin", "0");
        columnTitle.getStyle().set("color", "var(--lumo-contrast-90pct)");
        columnTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");
        columnTitle.getStyle().set("font-weight", "600");
        columnTitle.getStyle().set("letter-spacing", "0.025em");
        
        header.add(columnTitle);
        return header;
    }
    
    private static Component createEmptyState() {
        Div emptyState = new Div();
        emptyState.getStyle().set("text-align", "center");
        emptyState.getStyle().set("padding", "2rem");
        emptyState.getStyle().set("color", "var(--lumo-contrast-40pct)");
        
        Span emptyText = new Span("Select a PDF to start analysis");
        emptyText.getStyle().set("font-size", "var(--lumo-font-size-s)");
        emptyText.getStyle().set("font-style", "italic");
        
        emptyState.add(emptyText);
        return emptyState;
    }
    
    private static Component createControls(String analysisType, Document selectedDocument, AnalysisService analysisService, Div resultsArea) {
        Div controls = new Div();
        controls.getStyle().set("margin-bottom", "1rem");
        
        // Start Analysis Button
        Button startButton = createStartButton(analysisType, selectedDocument, analysisService);
        
        // Poll Results Button
        Button pollButton = createPollButton(analysisType, selectedDocument, analysisService, resultsArea);
        
        // PDF Export Button
        Button pdfExportButton = createPdfExportButton(analysisType, selectedDocument, analysisService);
        
        controls.add(startButton, pollButton, pdfExportButton);
        return controls;
    }
    
    private static Button createStartButton(String analysisType, Document selectedDocument, AnalysisService analysisService) {
        Button startButton = new Button("Start " + analysisType + " Analysis");
        startButton.getStyle().set("width", "100%");
        startButton.getStyle().set("background", "var(--lumo-primary-color)");
        startButton.getStyle().set("color", "var(--lumo-primary-contrast-color)");
        startButton.getStyle().set("border", "none");
        startButton.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        startButton.getStyle().set("padding", "0.75rem 1rem");
        startButton.getStyle().set("cursor", "pointer");
        startButton.getStyle().set("font-weight", "500");
        startButton.getStyle().set("font-size", "var(--lumo-font-size-s)");
        startButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        startButton.getStyle().set("transition", "all 0.2s ease");
        
        // Hover effects
        startButton.getElement().addEventListener("mouseenter", e -> {
            startButton.getStyle().set("background", "var(--lumo-primary-color-10pct)");
            startButton.getStyle().set("transform", "translateY(-1px)");
            startButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        });
        
        startButton.getElement().addEventListener("mouseleave", e -> {
            startButton.getStyle().set("background", "var(--lumo-primary-color)");
            startButton.getStyle().set("transform", "translateY(0)");
            startButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        });
        
        startButton.addClickListener(event -> {
            try {
                analysisService.startAnalysis(selectedDocument.getId(), analysisType);
                startButton.setText("Analysis Started...");
                startButton.setEnabled(false);
                startButton.getStyle().set("background", "var(--lumo-success-color)");
            } catch (Exception e) {
                startButton.setText("Error: " + e.getMessage());
                startButton.getStyle().set("background", "var(--lumo-error-color)");
            }
        });
        
        return startButton;
    }
    
    private static Button createPollButton(String analysisType, Document selectedDocument, AnalysisService analysisService, Div resultsArea) {
        Button pollButton = new Button("Check Results");
        pollButton.getStyle().set("width", "100%");
        pollButton.getStyle().set("background", "var(--lumo-success-color)");
        pollButton.getStyle().set("color", "var(--lumo-primary-contrast-color)");
        pollButton.getStyle().set("border", "none");
        pollButton.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        pollButton.getStyle().set("padding", "0.75rem 1rem");
        pollButton.getStyle().set("cursor", "pointer");
        pollButton.getStyle().set("margin-top", "0.75rem");
        pollButton.getStyle().set("font-weight", "500");
        pollButton.getStyle().set("font-size", "var(--lumo-font-size-s)");
        pollButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        pollButton.getStyle().set("transition", "all 0.2s ease");
        
        // Hover effects
        pollButton.getElement().addEventListener("mouseenter", e -> {
            pollButton.getStyle().set("background", "var(--lumo-success-color-10pct)");
            pollButton.getStyle().set("transform", "translateY(-1px)");
            pollButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        });
        
        pollButton.getElement().addEventListener("mouseleave", e -> {
            pollButton.getStyle().set("background", "var(--lumo-success-color)");
            pollButton.getStyle().set("transform", "translateY(0)");
            pollButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        });
        
        pollButton.addClickListener(event -> {
            try {
                analysisService.pollAndSaveAnalysisResults(selectedDocument.getId(), analysisType);
                pollButton.setText("Results Checked");
                pollButton.getStyle().set("background", "var(--lumo-primary-color)");
                
                // Refresh results display
                ResultsDisplay.refreshResultsDisplay(resultsArea, selectedDocument, analysisType, analysisService);
            } catch (Exception e) {
                pollButton.setText("Error: " + e.getMessage());
                pollButton.getStyle().set("background", "var(--lumo-error-color)");
            }
        });
        
        return pollButton;
    }
    
    private static Button createPdfExportButton(String analysisType, Document selectedDocument, AnalysisService analysisService) {
        Button pdfExportButton = new Button("Export as PDF");
        pdfExportButton.getStyle().set("width", "100%");
        pdfExportButton.getStyle().set("background", "var(--lumo-contrast-20pct)");
        pdfExportButton.getStyle().set("color", "var(--lumo-contrast-90pct)");
        pdfExportButton.getStyle().set("border", "1px solid var(--lumo-contrast-30pct)");
        pdfExportButton.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        pdfExportButton.getStyle().set("padding", "0.75rem 1rem");
        pdfExportButton.getStyle().set("cursor", "pointer");
        pdfExportButton.getStyle().set("margin-top", "0.75rem");
        pdfExportButton.getStyle().set("font-weight", "500");
        pdfExportButton.getStyle().set("font-size", "var(--lumo-font-size-s)");
        pdfExportButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        pdfExportButton.getStyle().set("transition", "all 0.2s ease");
        
        // Hover effects
        pdfExportButton.getElement().addEventListener("mouseenter", e -> {
            pdfExportButton.getStyle().set("background", "var(--lumo-contrast-30pct)");
            pdfExportButton.getStyle().set("transform", "translateY(-1px)");
            pdfExportButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        });
        
        pdfExportButton.getElement().addEventListener("mouseleave", e -> {
            pdfExportButton.getStyle().set("background", "var(--lumo-contrast-20pct)");
            pdfExportButton.getStyle().set("transform", "translateY(0)");
            pdfExportButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        });
        
        pdfExportButton.addClickListener(event -> {
            try {
                // Get analysis results
                var analysisResults = analysisService.getAnalysisResults(selectedDocument.getId(), analysisType);
                
                if (analysisResults.isEmpty()) {
                    pdfExportButton.setText("No results to export");
                    return;
                }
                
                // Create PDF merge service (you'll need to inject this properly)
                PdfMergeService pdfMergeService = new PdfMergeService();
                byte[] pdfBytes = pdfMergeService.mergeAnalysisImagesToPdf(selectedDocument, analysisType, analysisResults);
                
                // Create download resource
                String fileName = selectedDocument.getFileName().replace(".pdf", "") + "_" + analysisType + "_analysis.pdf";
                StreamResource resource = new StreamResource(fileName, () -> new ByteArrayInputStream(pdfBytes));
                
                // Trigger download
                pdfExportButton.getUI().ifPresent(ui -> {
                    ui.getPage().executeJs("window.open($0, '_blank')", resource);
                });
                
                pdfExportButton.setText("PDF Downloaded!");
                
            } catch (Exception e) {
                logger.error("Error exporting PDF: {}", e.getMessage(), e);
                pdfExportButton.setText("Export Failed");
            }
        });
        
        return pdfExportButton;
    }
    
    private static Div createResultsArea(String analysisType) {
        Div resultsArea = new Div();
        resultsArea.setId("results-" + analysisType);
        resultsArea.getStyle().set("min-height", "500px");
        resultsArea.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        resultsArea.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        resultsArea.getStyle().set("padding", "1rem");
        resultsArea.getStyle().set("background", "var(--lumo-contrast-5pct)");
        resultsArea.getStyle().set("box-shadow", "inset 0 1px 3px rgba(0,0,0,0.1)");
        
        // Placeholder for results
        Span resultsPlaceholder = new Span("Analysis results will appear here...");
        resultsPlaceholder.getStyle().set("color", "var(--lumo-contrast-50pct)");
        resultsPlaceholder.getStyle().set("font-size", "var(--lumo-font-size-s)");
        resultsPlaceholder.getStyle().set("font-style", "italic");
        resultsPlaceholder.getStyle().set("text-align", "center");
        resultsPlaceholder.getStyle().set("display", "block");
        resultsPlaceholder.getStyle().set("padding", "2rem");
        resultsArea.add(resultsPlaceholder);
        
        return resultsArea;
    }
}
