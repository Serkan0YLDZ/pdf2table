package com.pdfprocessor.vaadin.view.component;

import com.pdfprocessor.entity.Document;
import com.pdfprocessor.service.FileService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Reusable file upload component with dialog-based file selection
 */
public class FileUploadComponent {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadComponent.class);
    
    public static Component create(FileService fileService, Consumer<Document> onUploadSuccess) {
        Div uploadContainer = new Div();
        uploadContainer.addClassName("upload-container");
        uploadContainer.setWidthFull();
        
        // Create upload button
        Button uploadButton = new Button("Upload File...", VaadinIcon.UPLOAD.create());
        uploadButton.addClassName("upload-button");
        uploadButton.setWidthFull();
        uploadButton.getStyle().set("background-color", "var(--lumo-primary-color)");
        uploadButton.getStyle().set("color", "var(--lumo-primary-contrast-color)");
        uploadButton.getStyle().set("border", "none");
        uploadButton.getStyle().set("border-radius", "8px");
        uploadButton.getStyle().set("padding", "12px");
        uploadButton.getStyle().set("font-weight", "500");
        uploadButton.getStyle().set("cursor", "pointer");
        uploadButton.getStyle().set("transition", "background-color 0.2s ease");
        
        uploadButton.addClickListener(event -> {
            showFileUploadDialog(fileService, onUploadSuccess);
        });
        
        uploadContainer.add(uploadButton);
        return uploadContainer;
    }
    
    private static void showFileUploadDialog(FileService fileService, Consumer<Document> onUploadSuccess) {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");
        dialog.setHeight("300px");
        dialog.setModal(true);
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);
        
        VerticalLayout dialogContent = new VerticalLayout();
        dialogContent.setPadding(true);
        dialogContent.setSpacing(true);
        dialogContent.setSizeFull();
        
        // Header
        H3 header = new H3("Select PDF File");
        header.addClassName(LumoUtility.Margin.NONE);
        header.addClassName(LumoUtility.FontWeight.BOLD);
        
        // Upload area
        Div uploadArea = new Div();
        uploadArea.setWidthFull();
        uploadArea.setHeight("150px");
        uploadArea.getStyle().set("border", "2px dashed var(--lumo-contrast-30pct)");
        uploadArea.getStyle().set("border-radius", "8px");
        uploadArea.getStyle().set("display", "flex");
        uploadArea.getStyle().set("flex-direction", "column");
        uploadArea.getStyle().set("align-items", "center");
        uploadArea.getStyle().set("justify-content", "center");
        uploadArea.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        uploadArea.getStyle().set("cursor", "pointer");
        uploadArea.getStyle().set("transition", "border-color 0.2s ease");
        
        // Upload icon and text
        Span uploadIcon = new Span(VaadinIcon.UPLOAD.create());
        uploadIcon.getStyle().set("font-size", "24px");
        uploadIcon.getStyle().set("color", "var(--lumo-contrast-50pct)");
        uploadIcon.getStyle().set("margin-bottom", "8px");
        
        Span uploadText = new Span("Click to select PDF file");
        uploadText.getStyle().set("color", "var(--lumo-contrast-50pct)");
        uploadText.getStyle().set("font-size", "14px");
        
        uploadArea.add(uploadIcon, uploadText);
        
        // Hidden upload component
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".pdf");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(50 * 1024 * 1024); // 50MB
        upload.getStyle().set("display", "none");
        
        upload.addSucceededListener(event -> {
            try {
                logger.info("File upload started: {}", event.getFileName());
                
                InputStream inputStream = buffer.getInputStream();
                byte[] fileBytes = inputStream.readAllBytes();
                
                MultipartFile multipartFile = new SimpleMultipartFile(event.getFileName(), fileBytes);
                Document document = fileService.uploadFile(multipartFile);
                
                onUploadSuccess.accept(document);
                
                Notification notification = Notification.show("File uploaded successfully: " + document.getFileName());
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                notification.setDuration(3000);
                
                logger.info("File uploaded successfully: {}", document.getId());
                
                dialog.close();
                
            } catch (Exception e) {
                logger.error("Upload error: {}", e.getMessage());
                Notification notification = Notification.show("Upload failed: " + e.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.setDuration(5000);
            }
        });
        
        upload.addFailedListener(event -> {
            logger.error("File upload failed: {}", event.getReason().getMessage());
            Notification notification = Notification.show("Upload failed: " + event.getReason().getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.setDuration(5000);
        });
        
        // Make upload area clickable
        uploadArea.addClickListener(event -> {
            upload.getElement().executeJs("this.shadowRoot.querySelector('input[type=file]').click()");
        });
        
        // Buttons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> dialog.close());
        
        buttonLayout.add(cancelButton);
        
        dialogContent.add(header, uploadArea, upload, buttonLayout);
        dialog.add(dialogContent);
        
        dialog.open();
    }
    
    private static class SimpleMultipartFile implements MultipartFile {
        private final String fileName;
        private final byte[] fileBytes;
        
        public SimpleMultipartFile(String fileName, byte[] fileBytes) {
            this.fileName = fileName;
            this.fileBytes = fileBytes;
        }
        
        @Override
        public String getName() { return "file"; }
        
        @Override
        public String getOriginalFilename() { return fileName; }
        
        @Override
        public String getContentType() { return "application/pdf"; }
        
        @Override
        public boolean isEmpty() { return fileBytes.length == 0; }
        
        @Override
        public long getSize() { return fileBytes.length; }
        
        @Override
        public byte[] getBytes() { return fileBytes; }
        
        @Override
        public InputStream getInputStream() { return new ByteArrayInputStream(fileBytes); }
        
        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            // Not implemented for this use case
        }
    }
}
