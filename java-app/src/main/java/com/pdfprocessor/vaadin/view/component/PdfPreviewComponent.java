package com.pdfprocessor.vaadin.view.component;

import com.pdfprocessor.entity.Document;
import com.pdfprocessor.service.FileService;
import com.pdfprocessor.service.AnalysisService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Simplified PDF preview component with three-column layout
 */
public class PdfPreviewComponent {
    
    public static Component create(Document selectedDocument, FileService fileService, AnalysisService analysisService) {
        HorizontalLayout container = new HorizontalLayout();
        container.setSpacing(true);
        container.setWidthFull();
        container.setHeight("calc(100vh - 100px)");
        
        // First column: PDF Viewer
        Component pdfViewer = PdfViewer.create(selectedDocument, fileService);
        pdfViewer.getElement().getStyle().set("flex", "1");
        
        // Second column: Docling Layout Analysis
        Component doclingColumn = AnalysisPanel.create("Docling Layout Analiz", "docling", selectedDocument, analysisService);
        doclingColumn.getElement().getStyle().set("flex", "0 0 400px");
        
        // Third column: Deepdoctection Layout Analysis
        Component deepdoctectionColumn = AnalysisPanel.create("Deepdoctection Layout Analiz", "deepdoctection", selectedDocument, analysisService);
        deepdoctectionColumn.getElement().getStyle().set("flex", "0 0 400px");
        
        container.add(pdfViewer, doclingColumn, deepdoctectionColumn);
        return container;
    }
}
