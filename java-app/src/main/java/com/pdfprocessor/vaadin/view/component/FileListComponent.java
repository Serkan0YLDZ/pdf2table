package com.pdfprocessor.vaadin.view.component;

import com.pdfprocessor.entity.Document;
import com.pdfprocessor.service.FileService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable file list component
 */
public class FileListComponent {
    
    private static final Logger logger = LoggerFactory.getLogger(FileListComponent.class);
    
    public static Component create(List<Document> files, FileService fileService, 
                                 Consumer<Document> onFileDeleted, Consumer<Document> onFileSelected) {
        VerticalLayout fileList = new VerticalLayout();
        fileList.setSpacing(false);
        fileList.setPadding(false);
        fileList.setWidthFull();
        
        for (Document document : files) {
            fileList.add(createFileItem(document, fileService, onFileDeleted, onFileSelected));
        }
        
        return fileList;
    }
    
    private static Component createFileItem(Document document, FileService fileService,
                                          Consumer<Document> onFileDeleted, Consumer<Document> onFileSelected) {
        HorizontalLayout fileItem = new HorizontalLayout();
        fileItem.addClassName("file-item");
        fileItem.setWidthFull();
        fileItem.setSpacing(false);
        fileItem.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        fileItem.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        
        // Add hover and selection styles
        fileItem.getStyle().set("cursor", "pointer");
        fileItem.getStyle().set("padding", "8px");
        fileItem.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        fileItem.getStyle().set("transition", "background-color 0.2s ease");
        
        // Hover effect
        fileItem.getElement().addEventListener("mouseenter", e -> {
            fileItem.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        });
        fileItem.getElement().addEventListener("mouseleave", e -> {
            fileItem.getStyle().set("background-color", "transparent");
        });
        
        HorizontalLayout leftSide = new HorizontalLayout();
        leftSide.setSpacing(false);
        leftSide.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        leftSide.setFlexGrow(1);
        leftSide.setWidthFull();
        
        Button pdfIcon = new Button(VaadinIcon.FILE_TEXT.create());
        pdfIcon.addClassName("pdf-icon");
        pdfIcon.getStyle().set("margin-right", "8px");
        
        Span fileNameSpan = new Span(document.getFileName());
        fileNameSpan.addClassName("file-name");
        fileNameSpan.getStyle().set("overflow", "hidden");
        fileNameSpan.getStyle().set("text-overflow", "ellipsis");
        fileNameSpan.getStyle().set("white-space", "nowrap");
        fileNameSpan.getStyle().set("max-width", "115px");
        fileNameSpan.getStyle().set("display", "block");
        
        leftSide.add(pdfIcon, fileNameSpan);
        
        Button deleteButton = new Button(VaadinIcon.CLOSE_SMALL.create());
        deleteButton.addClassName("delete-button");
        deleteButton.getStyle().set("margin-left", "8px");
        deleteButton.getStyle().set("flex-shrink", "0");
        deleteButton.addClickListener(event -> {
            boolean deleted = fileService.deleteDocument(document.getId());
            if (deleted) {
                onFileDeleted.accept(document);
                Notification notification = Notification.show("File deleted successfully: " + document.getFileName());
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                notification.setDuration(3000);
            } else {
                Notification notification = Notification.show("Failed to delete file: " + document.getFileName());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.setDuration(5000);
            }
        });
        
        fileItem.addClickListener(event -> onFileSelected.accept(document));
        fileItem.add(leftSide, deleteButton);
        
        return fileItem;
    }
}
