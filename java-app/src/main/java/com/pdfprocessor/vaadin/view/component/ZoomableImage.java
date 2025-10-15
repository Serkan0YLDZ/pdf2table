package com.pdfprocessor.vaadin.view.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Zoomable image component with zoom controls
 */
public class ZoomableImage {
    
    private static final double ZOOM_FACTOR = 1.2;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 5.0;
    
    public static Component create(String imageUrl, String altText) {
        Div container = new Div();
        container.setWidthFull();
        container.getStyle().set("position", "relative");
        container.getStyle().set("overflow", "hidden");
        container.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        container.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        container.getStyle().set("background", "var(--lumo-contrast-5pct)");
        
        // Image container with zoom functionality
        Div imageContainer = new Div();
        imageContainer.setWidthFull();
        imageContainer.getStyle().set("position", "relative");
        imageContainer.getStyle().set("overflow", "auto");
        imageContainer.getStyle().set("max-height", "400px");
        
        // Create image
        Image image = new Image();
        image.setSrc(imageUrl);
        image.setAlt(altText);
        image.getStyle().set("display", "block");
        image.getStyle().set("max-width", "100%");
        image.getStyle().set("height", "auto");
        image.getStyle().set("transition", "transform 0.2s ease");
        image.getStyle().set("cursor", "grab");
        
        // Zoom level indicator
        Span zoomLevel = new Span("100%");
        zoomLevel.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        zoomLevel.getStyle().set("color", "var(--lumo-contrast-70pct)");
        zoomLevel.getStyle().set("margin", "0 8px");
        zoomLevel.getStyle().set("min-width", "40px");
        zoomLevel.getStyle().set("text-align", "center");
        zoomLevel.getStyle().set("display", "inline-block");
        
        // Zoom controls
        Div zoomControls = createZoomControls(image, zoomLevel);
        zoomControls.getStyle().set("position", "absolute");
        zoomControls.getStyle().set("top", "8px");
        zoomControls.getStyle().set("right", "8px");
        zoomControls.getStyle().set("z-index", "10");
        zoomControls.getStyle().set("background", "rgba(255, 255, 255, 0.9)");
        zoomControls.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        zoomControls.getStyle().set("padding", "4px");
        zoomControls.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        
        imageContainer.add(image);
        container.add(imageContainer, zoomControls);
        
        return container;
    }
    
    private static Div createZoomControls(Image image, Span zoomLevel) {
        HorizontalLayout controls = new HorizontalLayout();
        controls.setSpacing(false);
        controls.setPadding(false);
        controls.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        // Zoom out button
        Button zoomOutBtn = new Button(VaadinIcon.MINUS.create());
        zoomOutBtn.getStyle().set("width", "32px");
        zoomOutBtn.getStyle().set("height", "32px");
        zoomOutBtn.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        zoomOutBtn.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        zoomOutBtn.getStyle().set("background", "var(--lumo-base-color)");
        zoomOutBtn.getStyle().set("cursor", "pointer");
        zoomOutBtn.getStyle().set("transition", "all 0.2s ease");
        
        // Zoom in button
        Button zoomInBtn = new Button(VaadinIcon.PLUS.create());
        zoomInBtn.getStyle().set("width", "32px");
        zoomInBtn.getStyle().set("height", "32px");
        zoomInBtn.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        zoomInBtn.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        zoomInBtn.getStyle().set("background", "var(--lumo-base-color)");
        zoomInBtn.getStyle().set("cursor", "pointer");
        zoomInBtn.getStyle().set("transition", "all 0.2s ease");
        
        // Reset button
        Button resetBtn = new Button(VaadinIcon.REFRESH.create());
        resetBtn.getStyle().set("width", "32px");
        resetBtn.getStyle().set("height", "32px");
        resetBtn.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
        resetBtn.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        resetBtn.getStyle().set("background", "var(--lumo-base-color)");
        resetBtn.getStyle().set("cursor", "pointer");
        resetBtn.getStyle().set("transition", "all 0.2s ease");
        resetBtn.getStyle().set("margin-left", "4px");
        
        // Add button click handlers
        zoomOutBtn.addClickListener(e -> {
            double currentZoom = getCurrentZoom(image);
            zoomImage(image, zoomLevel, currentZoom / ZOOM_FACTOR);
        });
        
        zoomInBtn.addClickListener(e -> {
            double currentZoom = getCurrentZoom(image);
            zoomImage(image, zoomLevel, currentZoom * ZOOM_FACTOR);
        });
        
        resetBtn.addClickListener(e -> {
            zoomImage(image, zoomLevel, 1.0);
        });
        
        // Add hover effects
        addHoverEffect(zoomOutBtn);
        addHoverEffect(zoomInBtn);
        addHoverEffect(resetBtn);
        
        controls.add(zoomOutBtn, zoomLevel, zoomInBtn, resetBtn);
        
        Div controlsContainer = new Div();
        controlsContainer.add(controls);
        
        return controlsContainer;
    }
    
    private static double getCurrentZoom(Image image) {
        String currentZoomStr = image.getElement().getProperty("currentZoom", "1.0");
        try {
            return Double.parseDouble(currentZoomStr);
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }
    
    private static void zoomImage(Image image, Span zoomLevel, double newZoom) {
        // Clamp zoom level
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        
        // Apply zoom
        image.getElement().setProperty("currentZoom", String.valueOf(newZoom));
        image.getStyle().set("transform", "scale(" + newZoom + ")");
        image.getStyle().set("transform-origin", "center center");
        
        // Update zoom level display
        int percentage = (int) (newZoom * 100);
        zoomLevel.setText(percentage + "%");
    }
    
    private static void addHoverEffect(Button button) {
        button.getElement().addEventListener("mouseenter", e -> {
            button.getStyle().set("background", "var(--lumo-primary-color-10pct)");
            button.getStyle().set("border-color", "var(--lumo-primary-color)");
            button.getStyle().set("transform", "scale(1.05)");
        });
        
        button.getElement().addEventListener("mouseleave", e -> {
            button.getStyle().set("background", "var(--lumo-base-color)");
            button.getStyle().set("border-color", "var(--lumo-contrast-20pct)");
            button.getStyle().set("transform", "scale(1)");
        });
    }
}
