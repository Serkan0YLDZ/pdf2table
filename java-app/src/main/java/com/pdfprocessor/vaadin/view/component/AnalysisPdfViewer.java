package com.pdfprocessor.vaadin.view.component;

import com.pdfprocessor.entity.AnalysisFile;
import com.pdfprocessor.entity.Document;
import com.pdfprocessor.service.AnalysisService;
import com.pdfprocessor.service.PdfMergeService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PDF viewer component for displaying merged analysis results
 */
public class AnalysisPdfViewer {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalysisPdfViewer.class);
    
    public static Component create(Document selectedDocument, String analysisType, AnalysisService analysisService) {
        VerticalLayout container = new VerticalLayout();
        container.setSpacing(true);
        container.setPadding(true);
        container.setWidthFull();
        container.setHeightFull();
        
        // Modern styling
        container.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        container.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        container.getStyle().set("background", "var(--lumo-base-color)");
        container.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        container.getStyle().set("transition", "all 0.2s ease");
        
        if (selectedDocument == null) {
            container.add(createEmptyState());
        } else {
            // Header with title and controls
            container.add(createHeader(selectedDocument, analysisType, analysisService));
            
            // Check if analysis results exist
            if (hasAnalysisResults(selectedDocument, analysisType, analysisService)) {
                // PDF viewer area
                Div pdfViewerArea = createPdfViewerArea(selectedDocument, analysisType, analysisService);
                container.add(pdfViewerArea);
            } else {
                // Show analysis button to start Docling analysis
                container.add(createAnalysisStartArea(selectedDocument, analysisType, analysisService));
            }
        }
        
        return container;
    }
    
    private static boolean hasAnalysisResults(Document selectedDocument, String analysisType, AnalysisService analysisService) {
        try {
            logger.info("Checking analysis results for document: {}, type: {}", selectedDocument.getId(), analysisType);
            List<AnalysisFile> results = analysisService.getAnalysisResults(selectedDocument.getId(), analysisType);
            
            // More strict check - results must exist, not be null, and not be empty
            boolean hasResults = results != null && !results.isEmpty() && results.size() > 0;
            logger.info("Analysis results found in DB: {}, count: {}", hasResults, results != null ? results.size() : 0);
            
            // Additional check: verify that the physical files actually exist
            if (hasResults) {
                boolean physicalFilesExist = checkPhysicalFilesExist(selectedDocument.getId().toString(), analysisType);
                logger.info("Physical files exist: {}", physicalFilesExist);
                
                if (!physicalFilesExist) {
                    logger.warn("Analysis results exist in DB but physical files are missing for document: {}", selectedDocument.getId());
                    return false; // Return false if files don't exist
                }
                
                for (AnalysisFile result : results) {
                    logger.info("Analysis result: page={}", result.getPageNumber());
                }
            }
            
            return hasResults;
        } catch (Exception e) {
            logger.warn("Error checking analysis results for document {}: {}", selectedDocument.getId(), e.getMessage());
            return false;
        }
    }
    
    private static boolean checkPhysicalFilesExist(String documentId, String analysisType) {
        try {
            String analysisDir = "uploads/analysis/" + documentId + "/" + analysisType;
            java.io.File dir = new java.io.File(analysisDir);
            
            if (!dir.exists() || !dir.isDirectory()) {
                logger.info("Analysis directory does not exist: {}", analysisDir);
                return false;
            }
            
            // Check if there are any image files in the directory
            String[] imageExtensions = {".png", ".jpg", ".jpeg", ".bmp", ".tiff", ".tif"};
            java.io.File[] files = dir.listFiles();
            
            if (files == null || files.length == 0) {
                logger.info("No files found in analysis directory: {}", analysisDir);
                return false;
            }
            
            // Check if any of the files are images
            for (java.io.File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();
                    for (String ext : imageExtensions) {
                        if (fileName.endsWith(ext)) {
                            logger.info("Found image file: {}", file.getName());
                            return true; // At least one image file exists
                        }
                    }
                }
            }
            
            logger.info("No image files found in analysis directory: {}", analysisDir);
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking physical files for document {}: {}", documentId, e.getMessage());
            return false;
        }
    }
    
    private static Component createEmptyState() {
        Div emptyState = new Div();
        emptyState.getStyle().set("text-align", "center");
        emptyState.getStyle().set("padding", "3rem");
        emptyState.getStyle().set("color", "var(--lumo-contrast-50pct)");
        
        H3 emptyTitle = new H3("No Analysis PDF Available");
        emptyTitle.getStyle().set("margin", "0 0 1rem 0");
        emptyTitle.getStyle().set("color", "var(--lumo-contrast-70pct)");
        
        Span emptyText = new Span("Generate analysis results first, then view them as PDF here");
        emptyText.getStyle().set("font-size", "var(--lumo-font-size-m)");
        
        emptyState.add(emptyTitle, emptyText);
        return emptyState;
    }
    
    private static Component createAnalysisStartArea(Document selectedDocument, String analysisType, AnalysisService analysisService) {
        Div startArea = new Div();
        startArea.setWidth("calc(100% - 4rem)");
        startArea.setHeight("calc(100% - 100px)");
        startArea.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        startArea.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        startArea.getStyle().set("background", "var(--lumo-contrast-5pct)");
        startArea.getStyle().set("box-shadow", "inset 0 1px 3px rgba(0,0,0,0.1)");
        startArea.getStyle().set("display", "flex");
        startArea.getStyle().set("flex-direction", "column");
        startArea.getStyle().set("justify-content", "center");
        startArea.getStyle().set("align-items", "center");
        startArea.getStyle().set("padding", "2rem");
        
        // Icon
        Div iconDiv = new Div();
        iconDiv.getStyle().set("font-size", "4rem");
        iconDiv.getStyle().set("color", "var(--lumo-primary-color)");
        iconDiv.getStyle().set("margin-bottom", "1rem");
        iconDiv.getElement().setProperty("innerHTML", "📄");
        
        // Title
        H3 title = new H3("Start " + analysisType + " Analysis");
        title.getStyle().set("margin", "0 0 1rem 0");
        title.getStyle().set("color", "var(--lumo-contrast-90pct)");
        title.getStyle().set("font-size", "var(--lumo-font-size-xl)");
        title.getStyle().set("font-weight", "600");
        
        // Description
        Span description = new Span("Click the button below to start the " + analysisType + " analysis. This will process your PDF and generate layout analysis results.");
        description.getStyle().set("color", "var(--lumo-contrast-60pct)");
        description.getStyle().set("font-size", "var(--lumo-font-size-m)");
        description.getStyle().set("text-align", "center");
        description.getStyle().set("margin-bottom", "2rem");
        description.getStyle().set("max-width", "400px");
        description.getStyle().set("line-height", "1.5");
        
        // Start Analysis Button
        Button startAnalysisButton = createStartAnalysisButton(selectedDocument, analysisType, analysisService);
        
        startArea.add(iconDiv, title, description, startAnalysisButton);
        return startArea;
    }
    
    private static Button createStartAnalysisButton(Document selectedDocument, String analysisType, AnalysisService analysisService) {
        Button startButton = new Button("Start " + analysisType + " Analysis");
        startButton.getStyle().set("background", "var(--lumo-primary-color)");
        startButton.getStyle().set("color", "var(--lumo-primary-contrast-color)");
        startButton.getStyle().set("border", "none");
        startButton.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        startButton.getStyle().set("padding", "1rem 2rem");
        startButton.getStyle().set("font-size", "var(--lumo-font-size-l)");
        startButton.getStyle().set("font-weight", "600");
        startButton.getStyle().set("cursor", "pointer");
        startButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        startButton.getStyle().set("transition", "all 0.2s ease");
        startButton.getStyle().set("min-width", "180px");
        
        // Hover effects
        startButton.getElement().addEventListener("mouseenter", e -> {
            startButton.getStyle().set("background", "var(--lumo-primary-color-10pct)");
            startButton.getStyle().set("transform", "translateY(-2px)");
            startButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-l)");
        });
        
        startButton.getElement().addEventListener("mouseleave", e -> {
            startButton.getStyle().set("background", "var(--lumo-primary-color)");
            startButton.getStyle().set("transform", "translateY(0)");
            startButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        });
        
        startButton.addClickListener(event -> {
            try {
                // Start the analysis
                analysisService.startAnalysis(selectedDocument.getId(), analysisType);
                
                // Update button state
                startButton.setText("Analysis Started...");
                startButton.setEnabled(false);
                startButton.getStyle().set("background", "var(--lumo-success-color)");
                
                // Show progress message
                startButton.getUI().ifPresent(ui -> {
                    ui.access(() -> {
                        // You could add a progress indicator here
                        logger.info("Analysis started for document: {}", selectedDocument.getId());
                    });
                });
                
            } catch (Exception e) {
                logger.error("Error starting analysis: {}", e.getMessage(), e);
                startButton.setText("Error: " + e.getMessage());
                startButton.getStyle().set("background", "var(--lumo-error-color)");
            }
        });
        
        return startButton;
    }
    
    private static Component createHeader(Document selectedDocument, String analysisType, AnalysisService analysisService) {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        // Title section
        VerticalLayout titleSection = new VerticalLayout();
        titleSection.setSpacing(false);
        titleSection.setPadding(false);
        
        H3 title = new H3(analysisType + " Analysis PDF");
        title.getStyle().set("margin", "0 0 0.25rem 0");
        title.getStyle().set("color", "var(--lumo-contrast-90pct)");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)");
        title.getStyle().set("font-weight", "600");
        
        Span subtitle = new Span("Document: " + selectedDocument.getFileName());
        subtitle.getStyle().set("color", "var(--lumo-contrast-50pct)");
        subtitle.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        titleSection.add(title, subtitle);
        
        // Action buttons
        HorizontalLayout actionButtons = new HorizontalLayout();
        actionButtons.setSpacing(true);
        
        Button refreshButton = createRefreshButton(selectedDocument, analysisType, analysisService);
        Button downloadButton = createDownloadButton(selectedDocument, analysisType, analysisService);
        
        actionButtons.add(refreshButton, downloadButton);
        
        header.add(titleSection, actionButtons);
        return header;
    }
    
    private static Button createRefreshButton(Document selectedDocument, String analysisType, AnalysisService analysisService) {
        Button refreshButton = new Button(VaadinIcon.REFRESH.create());
        refreshButton.getStyle().set("width", "40px");
        refreshButton.getStyle().set("height", "40px");
        refreshButton.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        refreshButton.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        refreshButton.getStyle().set("background", "var(--lumo-base-color)");
        refreshButton.getStyle().set("cursor", "pointer");
        refreshButton.getStyle().set("transition", "all 0.2s ease");
        
        // Hover effect
        refreshButton.getElement().addEventListener("mouseenter", e -> {
            refreshButton.getStyle().set("background", "var(--lumo-primary-color-10pct)");
            refreshButton.getStyle().set("border-color", "var(--lumo-primary-color)");
            refreshButton.getStyle().set("transform", "scale(1.05)");
        });
        
        refreshButton.getElement().addEventListener("mouseleave", e -> {
            refreshButton.getStyle().set("background", "var(--lumo-base-color)");
            refreshButton.getStyle().set("border-color", "var(--lumo-contrast-20pct)");
            refreshButton.getStyle().set("transform", "scale(1)");
        });
        
        // Click handler to refresh only this PDF viewer area
        refreshButton.addClickListener(event -> {
            try {
                // Show loading state
                refreshButton.setEnabled(false);
                refreshButton.getElement().setProperty("innerHTML", 
                    "<div style='animation: spin 1s linear infinite; transform-origin: center;'>⟳</div>");
                refreshButton.getStyle().set("background", "var(--lumo-contrast-10pct)");
                
                // Add CSS animation if not already present
                refreshButton.getUI().ifPresent(ui -> {
                    ui.getPage().executeJs(
                        "if (!document.getElementById('refresh-spinner-style')) {" +
                        "  var style = document.createElement('style');" +
                        "  style.id = 'refresh-spinner-style';" +
                        "  style.textContent = '@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }';" +
                        "  document.head.appendChild(style);" +
                        "}"
                    );
                });
                
                // Poll for new results
                analysisService.pollAndSaveAnalysisResults(selectedDocument.getId(), analysisType);
                
                // Refresh only this PDF viewer area using UI.access for thread safety
                refreshButton.getUI().ifPresent(ui -> {
                    ui.access(() -> {
                        try {
                            // Find the main container (this AnalysisPdfViewer's container)
                            Component mainContainer = findThisPdfViewerContainer(refreshButton);
                            if (mainContainer instanceof VerticalLayout) {
                                refreshThisPdfViewerArea((VerticalLayout) mainContainer, selectedDocument, analysisType, analysisService);
                            }
                            
                            // Reset button state
                            refreshButton.setEnabled(true);
                            refreshButton.getElement().setProperty("innerHTML", "");
                            // Create a completely new button with icon to avoid icon conflicts
                            refreshButton.getElement().setProperty("innerHTML", 
                                "<vaadin-icon icon=\"vaadin:refresh\" style=\"width: 16px; height: 16px;\"></vaadin-icon>");
                            
                            // Check if results exist and update button color accordingly
                            var analysisResults = analysisService.getAnalysisResults(selectedDocument.getId(), analysisType);
                            if (analysisResults != null && !analysisResults.isEmpty()) {
                                refreshButton.getStyle().set("background", "var(--lumo-success-color)");
                                refreshButton.getStyle().set("color", "var(--lumo-success-contrast-color)");
                                logger.info("PDF viewer area refreshed successfully for {} analysis, document: {}", analysisType, selectedDocument.getId());
                            } else {
                                refreshButton.getStyle().set("background", "var(--lumo-base-color)");
                                refreshButton.getStyle().set("color", "var(--lumo-contrast-90pct)");
                                logger.info("No analysis results found yet for {} analysis, document: {}", analysisType, selectedDocument.getId());
                            }
                            
                        } catch (Exception e) {
                            logger.error("Error refreshing PDF viewer area: {}", e.getMessage(), e);
                            // Reset button on error
                            refreshButton.setEnabled(true);
                            refreshButton.getElement().setProperty("innerHTML", "");
                            // Create a completely new button with icon to avoid icon conflicts
                            refreshButton.getElement().setProperty("innerHTML", 
                                "<vaadin-icon icon=\"vaadin:refresh\" style=\"width: 16px; height: 16px;\"></vaadin-icon>");
                            refreshButton.getStyle().set("background", "var(--lumo-error-color)");
                            refreshButton.getStyle().set("color", "var(--lumo-error-contrast-color)");
                        }
                    });
                });
                
            } catch (Exception e) {
                logger.error("Error checking analysis results: {}", e.getMessage(), e);
                // Reset button on error
                refreshButton.setEnabled(true);
                refreshButton.getElement().setProperty("innerHTML", "");
                // Create a completely new button with icon to avoid icon conflicts
                refreshButton.getElement().setProperty("innerHTML", 
                    "<vaadin-icon icon=\"vaadin:refresh\" style=\"width: 16px; height: 16px;\"></vaadin-icon>");
                refreshButton.getStyle().set("background", "var(--lumo-error-color)");
                refreshButton.getStyle().set("color", "var(--lumo-error-contrast-color)");
            }
        });
        
        return refreshButton;
    }
    
    private static Button createDownloadButton(Document selectedDocument, String analysisType, AnalysisService analysisService) {
        Button downloadButton = new Button(VaadinIcon.DOWNLOAD.create());
        downloadButton.getStyle().set("width", "40px");
        downloadButton.getStyle().set("height", "40px");
        downloadButton.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        downloadButton.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        downloadButton.getStyle().set("background", "var(--lumo-base-color)");
        downloadButton.getStyle().set("cursor", "pointer");
        downloadButton.getStyle().set("transition", "all 0.2s ease");
        
        // Hover effect
        downloadButton.getElement().addEventListener("mouseenter", e -> {
            downloadButton.getStyle().set("background", "var(--lumo-success-color-10pct)");
            downloadButton.getStyle().set("border-color", "var(--lumo-success-color)");
            downloadButton.getStyle().set("transform", "scale(1.05)");
        });
        
        downloadButton.getElement().addEventListener("mouseleave", e -> {
            downloadButton.getStyle().set("background", "var(--lumo-base-color)");
            downloadButton.getStyle().set("border-color", "var(--lumo-contrast-20pct)");
            downloadButton.getStyle().set("transform", "scale(1)");
        });
        
        return downloadButton;
    }
    
    private static Div createPdfViewerArea(Document selectedDocument, String analysisType, AnalysisService analysisService) {
        Div pdfViewerArea = new Div();
        pdfViewerArea.setWidthFull();
        pdfViewerArea.setHeight("calc(100% - 100px)");
        pdfViewerArea.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        pdfViewerArea.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        pdfViewerArea.getStyle().set("background", "var(--lumo-contrast-5pct)");
        pdfViewerArea.getStyle().set("box-shadow", "inset 0 1px 3px rgba(0,0,0,0.1)");
        pdfViewerArea.getStyle().set("overflow", "hidden");
        
        try {
            // Get analysis results
            List<AnalysisFile> analysisResults = analysisService.getAnalysisResults(selectedDocument.getId(), analysisType);
            logger.info("PDF Viewer Area - Analysis results: {}", analysisResults != null ? analysisResults.size() : "null");
            
            if (analysisResults == null || analysisResults.isEmpty()) {
                logger.info("No analysis results found, showing no results state");
                pdfViewerArea.add(createNoResultsState());
            } else {
                logger.info("Analysis results found, attempting to create PDF viewer");
                try {
                    // Create merged PDF viewer
                    Component pdfViewer = createMergedPdfViewer(selectedDocument, analysisType, analysisResults);
                    pdfViewerArea.add(pdfViewer);
                } catch (Exception pdfError) {
                    logger.error("Error creating merged PDF viewer: {}", pdfError.getMessage(), pdfError);
                    // If PDF creation fails, show analysis start area instead
                    pdfViewerArea.add(createAnalysisStartArea(selectedDocument, analysisType, analysisService));
                }
            }
            
        } catch (Exception e) {
            logger.error("Error creating PDF viewer: {}", e.getMessage(), e);
            pdfViewerArea.add(createErrorState("No analysis results available yet. Please start the analysis first."));
        }
        
        return pdfViewerArea;
    }
    
    private static Component createNoResultsState() {
        Div noResults = new Div();
        noResults.getStyle().set("text-align", "center");
        noResults.getStyle().set("padding", "3rem");
        noResults.getStyle().set("color", "var(--lumo-contrast-50pct)");
        noResults.getStyle().set("display", "flex");
        noResults.getStyle().set("flex-direction", "column");
        noResults.getStyle().set("justify-content", "center");
        noResults.getStyle().set("align-items", "center");
        noResults.getStyle().set("height", "100%");
        
        // Icon
        Div iconDiv = new Div();
        iconDiv.getStyle().set("font-size", "3rem");
        iconDiv.getStyle().set("color", "var(--lumo-contrast-30pct)");
        iconDiv.getStyle().set("margin-bottom", "1rem");
        iconDiv.getElement().setProperty("innerHTML", "⏳");
        
        H4 noResultsTitle = new H4("Analysis in Progress");
        noResultsTitle.getStyle().set("margin", "0 0 1rem 0");
        noResultsTitle.getStyle().set("color", "var(--lumo-contrast-70pct)");
        noResultsTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");
        
        Span noResultsText = new Span("The analysis is running. Please wait for the results to be generated. You can check back later by refreshing this area.");
        noResultsText.getStyle().set("font-size", "var(--lumo-font-size-m)");
        noResultsText.getStyle().set("max-width", "300px");
        noResultsText.getStyle().set("line-height", "1.5");
        
        noResults.add(iconDiv, noResultsTitle, noResultsText);
        return noResults;
    }
    
    private static Component createMergedPdfViewer(Document selectedDocument, String analysisType, List<AnalysisFile> analysisResults) {
        try {
            // Create PDF merge service
            PdfMergeService pdfMergeService = new PdfMergeService();
            byte[] pdfBytes = pdfMergeService.mergeAnalysisImagesToPdf(selectedDocument, analysisType, analysisResults);
            
            // Create iframe for PDF viewing using file path
            Div pdfContainer = new Div();
            pdfContainer.setWidth("100%");
            pdfContainer.setHeight("100%");
            
            // Get the PDF file name
            String fileName = selectedDocument.getId() + "_" + analysisType + "_analysis.pdf";
            
            // Create iframe HTML for PDF viewing using API endpoint
            String iframeHtml = String.format(
                "<iframe src=\"%s#toolbar=1&navpanes=1&scrollbar=1\" width=\"100%%\" height=\"100%%\" style=\"border: none; border-radius: var(--lumo-border-radius-m);\"></iframe>",
                "/api/files/analysis/" + selectedDocument.getId() + "/" + analysisType + "/pdf/" + fileName
            );
            
            pdfContainer.getElement().setProperty("innerHTML", iframeHtml);
            return pdfContainer;
            
        } catch (Exception e) {
            logger.error("Error creating merged PDF viewer: {}", e.getMessage(), e);
            return createErrorState("Failed to create PDF viewer: " + e.getMessage());
        }
    }
    
    private static Component createErrorState(String errorMessage) {
        Div errorState = new Div();
        errorState.getStyle().set("text-align", "center");
        errorState.getStyle().set("padding", "2rem");
        errorState.getStyle().set("color", "var(--lumo-error-color)");
        
        H4 errorTitle = new H4("Error loading PDF");
        errorTitle.getStyle().set("margin", "0 0 1rem 0");
        
        Span errorText = new Span(errorMessage);
        errorText.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        errorState.add(errorTitle, errorText);
        return errorState;
    }
    
    /**
     * Find this specific PDF viewer's container by navigating up the component hierarchy
     */
    private static Component findThisPdfViewerContainer(Component component) {
        Component current = component;
        while (current != null) {
            if (current instanceof VerticalLayout) {
                VerticalLayout layout = (VerticalLayout) current;
                // Check if this is the AnalysisPdfViewer container by looking for specific styling
                if (layout.getStyle().get("border-radius") != null && 
                    layout.getStyle().get("border-radius").equals("var(--lumo-border-radius-l)")) {
                    return current;
                }
            }
            current = current.getParent().orElse(null);
        }
        return component; // Fallback to original component
    }
    
    /**
     * Refresh only this specific PDF viewer area without affecting other areas
     */
    private static void refreshThisPdfViewerArea(VerticalLayout mainContainer, Document selectedDocument, 
                                               String analysisType, AnalysisService analysisService) {
        try {
            // Remove ALL existing content areas (both start analysis and PDF viewer areas)
            // Find and remove any existing content divs
            List<Component> componentsToRemove = mainContainer.getChildren()
                .filter(child -> child instanceof Div)
                .collect(Collectors.toList());
            
            // Remove all found divs
            componentsToRemove.forEach(component -> {
                mainContainer.remove(component);
                logger.info("Removed existing content area for {} analysis", analysisType);
            });
            
            // Always create and add the new PDF viewer area
            // This will show either the PDF (if results exist) or "Analysis in Progress" (if no results yet)
            Div newPdfViewerArea = createPdfViewerArea(selectedDocument, analysisType, analysisService);
            mainContainer.add(newPdfViewerArea);
            
            logger.info("Refreshed PDF viewer area for {} analysis, document: {} - Results exist: {}", 
                       analysisType, selectedDocument.getId(), hasAnalysisResults(selectedDocument, analysisType, analysisService));
                
        } catch (Exception e) {
            logger.error("Error refreshing this PDF viewer area: {}", e.getMessage(), e);
        }
    }
}
