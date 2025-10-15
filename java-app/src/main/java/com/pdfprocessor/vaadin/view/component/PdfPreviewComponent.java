package com.pdfprocessor.vaadin.view.component;

import com.pdfprocessor.entity.Document;
import com.pdfprocessor.service.FileService;
import com.pdfprocessor.service.AnalysisService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Modern PDF preview component with three-column layout and horizontal scroll
 */
public class PdfPreviewComponent {
    
    public static Component create(Document selectedDocument, FileService fileService, AnalysisService analysisService) {
        // Main container with horizontal scroll
        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.setHeight("calc(100vh - 100px)");
        mainContainer.getStyle().set("overflow-x", "auto");
        mainContainer.getStyle().set("overflow-y", "hidden");
        mainContainer.getStyle().set("background", "var(--lumo-contrast-5pct)");
        mainContainer.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        mainContainer.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        
        // Inner horizontal layout with fixed minimum width
        HorizontalLayout container = new HorizontalLayout();
        container.setSpacing(true);
        container.setPadding(true);
        container.setWidth("max-content");
        container.setMinWidth("100%");
        container.setHeightFull();
        container.getStyle().set("min-width", "1000px"); // Ensure minimum width for proper display with 2 columns
        
        // First column: Original PDF Viewer (left side)
        Component pdfViewer = PdfViewer.create(selectedDocument, fileService);
        pdfViewer.getElement().getStyle().set("flex", "0 0 50%"); // Take half the width
        pdfViewer.getElement().getStyle().set("min-width", "500px");
        
        // Second column: Analysis PDF Viewer (right side) - Layout Analiz Çıktısı
        Component analysisPdfViewer = AnalysisPdfViewer.create(selectedDocument, "docling", analysisService);
        analysisPdfViewer.getElement().getStyle().set("flex", "0 0 50%"); // Take half the width
        analysisPdfViewer.getElement().getStyle().set("min-width", "500px");
        
        container.add(pdfViewer, analysisPdfViewer);
        mainContainer.add(container);
        
        return mainContainer;
    }
}
