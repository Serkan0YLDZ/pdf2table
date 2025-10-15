package com.pdfprocessor.vaadin.view.component;

import com.pdfprocessor.entity.Document;
import com.pdfprocessor.service.AnalysisService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Analysis panel component for starting and checking analysis
 */
public class AnalysisPanel {
    
    public static Component create(String title, String analysisType, Document selectedDocument, AnalysisService analysisService) {
        VerticalLayout column = new VerticalLayout();
        column.setSpacing(false);
        column.setPadding(true);
        column.setWidthFull();
        column.setHeightFull();
        column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        column.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        column.getStyle().set("background", "var(--lumo-contrast-5pct)");
        
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
        header.getStyle().set("margin-bottom", "1rem");
        header.getStyle().set("padding-bottom", "1rem");
        header.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        
        H4 columnTitle = new H4(title);
        columnTitle.getStyle().set("margin", "0");
        columnTitle.getStyle().set("color", "var(--lumo-contrast-70pct)");
        columnTitle.getStyle().set("font-size", "var(--lumo-font-size-m)");
        
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
        
        controls.add(startButton, pollButton);
        return controls;
    }
    
    private static Button createStartButton(String analysisType, Document selectedDocument, AnalysisService analysisService) {
        Button startButton = new Button("Start " + analysisType + " Analysis");
        startButton.getStyle().set("width", "100%");
        startButton.getStyle().set("background", "var(--lumo-primary-color)");
        startButton.getStyle().set("color", "var(--lumo-primary-contrast-color)");
        startButton.getStyle().set("border", "none");
        startButton.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        startButton.getStyle().set("padding", "0.5rem");
        startButton.getStyle().set("cursor", "pointer");
        
        startButton.addClickListener(event -> {
            try {
                analysisService.startAnalysis(selectedDocument.getId(), analysisType);
                startButton.setText("Analysis Started...");
                startButton.setEnabled(false);
            } catch (Exception e) {
                startButton.setText("Error: " + e.getMessage());
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
        pollButton.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        pollButton.getStyle().set("padding", "0.5rem");
        pollButton.getStyle().set("cursor", "pointer");
        pollButton.getStyle().set("margin-top", "0.5rem");
        
        pollButton.addClickListener(event -> {
            try {
                analysisService.pollAndSaveAnalysisResults(selectedDocument.getId(), analysisType);
                pollButton.setText("Results Checked");
                
                // Refresh results display
                ResultsDisplay.refreshResultsDisplay(resultsArea, selectedDocument, analysisType, analysisService);
            } catch (Exception e) {
                pollButton.setText("Error: " + e.getMessage());
            }
        });
        
        return pollButton;
    }
    
    private static Div createResultsArea(String analysisType) {
        Div resultsArea = new Div();
        resultsArea.setId("results-" + analysisType);
        resultsArea.getStyle().set("min-height", "500px");
        resultsArea.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        resultsArea.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        resultsArea.getStyle().set("padding", "0.5rem");
        resultsArea.getStyle().set("background", "var(--lumo-base-color)");
        
        // Placeholder for results
        Span resultsPlaceholder = new Span("Analysis results will appear here...");
        resultsPlaceholder.getStyle().set("color", "var(--lumo-contrast-50pct)");
        resultsPlaceholder.getStyle().set("font-size", "var(--lumo-font-size-s)");
        resultsPlaceholder.getStyle().set("font-style", "italic");
        resultsArea.add(resultsPlaceholder);
        
        return resultsArea;
    }
}
