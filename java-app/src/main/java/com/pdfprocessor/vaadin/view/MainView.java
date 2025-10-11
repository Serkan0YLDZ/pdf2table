package com.pdfprocessor.vaadin.view;

import com.pdfprocessor.entity.Document;
import com.pdfprocessor.service.FileService;
import com.pdfprocessor.vaadin.view.component.FileListComponent;
import com.pdfprocessor.vaadin.view.component.FileUploadComponent;
import com.pdfprocessor.vaadin.view.component.PdfPreviewComponent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified PDF Viewer application
 */
@Route("")
@PageTitle("PDF Viewer")
public class MainView extends AppLayout {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    private final FileService fileService;
    private final VerticalLayout fileListContainer;
    private final VerticalLayout mainContent;
    private Button toggleButton;
    private boolean isSidebarOpen = true;
    private H2 fileListTitle;
    private Document selectedDocument;

    public MainView(FileService fileService) {
        this.fileService = fileService;
        this.fileListContainer = new VerticalLayout();
        this.mainContent = new VerticalLayout();
        
        createLayout();
    }


    private void createLayout() {
        setDrawerOpened(isSidebarOpen);
        createSidebar();
        createMainContent();
        createToggleButton();
    }

    private void createSidebar() {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setPadding(true);
        sidebar.setSpacing(true);
        sidebar.setWidth("250px");
        sidebar.addClassName(LumoUtility.Background.BASE);
        
        // Header
        HorizontalLayout header = createSidebarHeader();
        
        // Upload section
        Component uploadSection = FileUploadComponent.create(fileService, this::onFileUploaded);
        
        // File list section
        fileListTitle = new H2("Uploaded Files (" + getDocumentCount() + ")");
        fileListTitle.addClassName(LumoUtility.Margin.Top.MEDIUM);
        fileListTitle.addClassName(LumoUtility.Margin.Bottom.SMALL);
        
        // File list container
        fileListContainer.setSpacing(false);
        fileListContainer.setPadding(false);
        fileListContainer.setWidthFull();
        updateFileList();
        
        sidebar.add(header, uploadSection, fileListTitle, fileListContainer);
        addToDrawer(sidebar);
    }
    
    private HorizontalLayout createSidebarHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        
        VerticalLayout titleSection = new VerticalLayout();
        titleSection.setSpacing(false);
        titleSection.setPadding(false);
        
        H1 title = new H1("PDF Viewer");
        title.addClassName(LumoUtility.Margin.NONE);
        title.addClassName(LumoUtility.FontSize.LARGE);
        title.addClassName(LumoUtility.FontWeight.BOLD);
        
        Span subtitle = new Span("Manage and view your documents");
        subtitle.addClassName(LumoUtility.FontSize.SMALL);
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        
        titleSection.add(title, subtitle);
        
        Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create());
        closeButton.addClassName(LumoUtility.IconSize.SMALL);
        closeButton.getStyle().set("background", "none");
        closeButton.getStyle().set("border", "none");
        closeButton.getStyle().set("color", "var(--lumo-contrast-50pct)");
        closeButton.getStyle().set("padding", "4px");
        closeButton.addClickListener(event -> toggleSidebar());
        
        header.add(titleSection, closeButton);
        return header;
    }

    private void onFileUploaded(Document document) {
                updateFileList();
                updatePdfViewer();
                updateFileCount();
    }

    private void createMainContent() {
        mainContent.setPadding(false);
        mainContent.setSpacing(false);
        mainContent.setSizeFull();
        mainContent.addClassName(LumoUtility.Background.BASE);
        
        updatePdfViewer();
        setContent(mainContent);
    }

    private void createToggleButton() {
        toggleButton = new Button(VaadinIcon.MENU.create());
        toggleButton.addClassName(LumoUtility.IconSize.SMALL);
        toggleButton.getStyle().set("position", "absolute");
        toggleButton.getStyle().set("top", "16px");
        toggleButton.getStyle().set("left", "16px");
        toggleButton.getStyle().set("z-index", "1000");
        toggleButton.getStyle().set("background-color", "var(--lumo-primary-color)");
        toggleButton.getStyle().set("color", "var(--lumo-primary-contrast-color)");
        toggleButton.getStyle().set("border", "none");
        toggleButton.getStyle().set("border-radius", "6px");
        toggleButton.getStyle().set("width", "44px");
        toggleButton.getStyle().set("height", "44px");
        toggleButton.getStyle().set("box-shadow", "var(--lumo-box-shadow-l)");
        toggleButton.getStyle().set("transition", "all 0.2s ease");
        toggleButton.getStyle().set("display", "none");
        
        toggleButton.addClickListener(event -> toggleSidebar());
        mainContent.add(toggleButton);
    }

    private void toggleSidebar() {
        isSidebarOpen = !isSidebarOpen;
        setDrawerOpened(isSidebarOpen);
        
        if (isSidebarOpen) {
            toggleButton.getStyle().set("display", "none");
        } else {
            toggleButton.getStyle().set("display", "block");
        }
    }

    private void updatePdfViewer() {
        mainContent.removeAll();
        Component pdfPreview = PdfPreviewComponent.create(selectedDocument, fileService);
        mainContent.add(pdfPreview);
    }
    
    private void updateFileList() {
        fileListContainer.removeAll();
        Component fileList = FileListComponent.create(getAllDocuments(), fileService, 
            this::onFileDeleted, this::onFileSelected);
        fileListContainer.add(fileList);
    }
    
    private void onFileDeleted(Document document) {
                    updateFileList();
                    updatePdfViewer();
                    updateFileCount();
    }
    
    private void onFileSelected(Document document) {
        selectedDocument = document;
        updatePdfViewer();
        logger.info("File selected: {}", document.getFileName());
    }

    private List<Document> getAllDocuments() {
        return fileService.getAllDocuments();
    }
    
    private int getDocumentCount() {
        return getAllDocuments().size();
    }
    
    private void updateFileCount() {
        if (fileListTitle != null) {
            fileListTitle.setText("Uploaded Files (" + getDocumentCount() + ")");
        }
    }
}
