package com.pdfprocessor.vaadin.view.component;

import com.pdfprocessor.entity.Document;
import com.pdfprocessor.service.FileService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simplified PDF preview component with three-column layout
 */
public class PdfPreviewComponent {
    
    public static Component create(Document selectedDocument, FileService fileService) {
        HorizontalLayout container = new HorizontalLayout();
        container.setSpacing(true);
        container.setWidthFull();
        container.setHeight("calc(100vh - 100px)");
        
        // First column: PDF Viewer
        Component pdfViewer = createPdfViewer(selectedDocument, fileService);
        pdfViewer.getElement().getStyle().set("flex", "1");
        
        // Second column: Docling Layout Analysis (placeholder)
        Component doclingColumn = createPlaceholderColumn("Docling Layout Analiz");
        doclingColumn.getElement().getStyle().set("flex", "0 0 300px");
        
        // Third column: Deepdoctection Layout Analysis (placeholder)
        Component deepdoctectionColumn = createPlaceholderColumn("Deepdoctection Layout Analiz");
        deepdoctectionColumn.getElement().getStyle().set("flex", "0 0 300px");
        
        container.add(pdfViewer, doclingColumn, deepdoctectionColumn);
        return container;
    }
    
    private static Component createPdfViewer(Document selectedDocument, FileService fileService) {
        VerticalLayout pdfViewer = new VerticalLayout();
        pdfViewer.setSpacing(false);
        pdfViewer.setPadding(true);
        pdfViewer.setWidthFull();
        pdfViewer.setHeightFull();
        
        if (selectedDocument == null) {
            // Empty state
        Div emptyState = new Div();
        emptyState.addClassName("empty-state");
            emptyState.getStyle().set("text-align", "center");
            emptyState.getStyle().set("padding", "2rem");
            emptyState.getStyle().set("color", "var(--lumo-contrast-50pct)");
            
            H3 emptyTitle = new H3("No PDF Selected");
            emptyTitle.getStyle().set("margin", "0 0 1rem 0");
            emptyTitle.getStyle().set("color", "var(--lumo-contrast-70pct)");
            
            Span emptyText = new Span("Select a PDF from the left panel to view it here");
            emptyText.getStyle().set("font-size", "var(--lumo-font-size-m)");
            
            emptyState.add(emptyTitle, emptyText);
            pdfViewer.add(emptyState);
        } else {
            // PDF header
            Div header = new Div();
            header.getStyle().set("margin-bottom", "1rem");
            header.getStyle().set("padding-bottom", "1rem");
            header.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
            
            H3 title = new H3(selectedDocument.getFileName());
            title.getStyle().set("margin", "0 0 0.5rem 0");
            title.getStyle().set("color", "var(--lumo-contrast-90pct)");
            
            Span fileInfo = new Span(fileService.getFileSizeInHumanReadable(selectedDocument.getFileSize()));
            fileInfo.getStyle().set("color", "var(--lumo-contrast-50pct)");
            fileInfo.getStyle().set("font-size", "var(--lumo-font-size-s)");
            
            header.add(title, fileInfo);
            
            // PDF content using Embedded component
            Div pdfContainer = new Div();
            pdfContainer.setWidthFull();
            pdfContainer.setHeight("calc(100% - 100px)");
            pdfContainer.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            pdfContainer.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
            
            try {
                // Create PDF viewer using iframe with StreamResource
                Div pdfViewerDiv = createPdfViewer(selectedDocument);
                pdfContainer.add(pdfViewerDiv);
            } catch (Exception e) {
                // Error state
                Div errorState = new Div();
                errorState.getStyle().set("text-align", "center");
                errorState.getStyle().set("padding", "2rem");
                errorState.getStyle().set("color", "var(--lumo-error-color)");
                
                H4 errorTitle = new H4("Error loading PDF");
                errorTitle.getStyle().set("margin", "0 0 1rem 0");
                
                Span errorText = new Span("Could not load the PDF file. Please try again.");
                errorText.getStyle().set("font-size", "var(--lumo-font-size-s)");
                
                errorState.add(errorTitle, errorText);
                pdfContainer.add(errorState);
            }
            
            pdfViewer.add(header, pdfContainer);
        }
        
        return pdfViewer;
    }
    
    private static Div createPdfViewer(Document document) throws IOException {
        Path filePath = Paths.get(document.getFilePath());
        byte[] fileBytes = Files.readAllBytes(filePath);
        
        StreamResource resource = new StreamResource(
            document.getFileName(),
            () -> new ByteArrayInputStream(fileBytes)
        );
        
        Div pdfContainer = new Div();
        pdfContainer.setWidth("100%");
        pdfContainer.setHeight("100%");
        
        // Create iframe for PDF viewing using browser's native PDF viewer
        // For now, we'll use a simple approach with the file path
        String pdfUrl = "/api/files/" + document.getFileName(); // This will be handled by a controller
        String iframeHtml = String.format(
            "<iframe src=\"%s#toolbar=1&navpanes=1&scrollbar=1\" width=\"100%%\" height=\"100%%\" style=\"border: none; border-radius: var(--lumo-border-radius-m);\"></iframe>",
            pdfUrl
        );
        
        pdfContainer.getElement().setProperty("innerHTML", iframeHtml);
        
        return pdfContainer;
    }
    
    private static Component createPlaceholderColumn(String title) {
        VerticalLayout column = new VerticalLayout();
        column.setSpacing(false);
        column.setPadding(true);
        column.setWidthFull();
        column.setHeightFull();
        column.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        column.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        column.getStyle().set("background", "var(--lumo-contrast-5pct)");
        
        // Header
        Div header = new Div();
        header.getStyle().set("margin-bottom", "1rem");
        header.getStyle().set("padding-bottom", "1rem");
        header.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        
        H4 columnTitle = new H4(title);
        columnTitle.getStyle().set("margin", "0");
        columnTitle.getStyle().set("color", "var(--lumo-contrast-70pct)");
        columnTitle.getStyle().set("font-size", "var(--lumo-font-size-m)");
        
        header.add(columnTitle);
        
        // Placeholder content
        Div placeholder = new Div();
        placeholder.getStyle().set("text-align", "center");
        placeholder.getStyle().set("padding", "2rem");
        placeholder.getStyle().set("color", "var(--lumo-contrast-40pct)");
        
        Span placeholderText = new Span("This feature will be available in a future update");
        placeholderText.getStyle().set("font-size", "var(--lumo-font-size-s)");
        placeholderText.getStyle().set("font-style", "italic");
        
        placeholder.add(placeholderText);
        
        column.add(header, placeholder);
        return column;
    }
}
