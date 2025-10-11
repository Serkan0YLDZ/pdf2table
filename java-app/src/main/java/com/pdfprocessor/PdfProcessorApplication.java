package com.pdfprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PDF Processor UI Application
 * Main Spring Boot application class for the Vaadin UI
 */
@SpringBootApplication
public class PdfProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdfProcessorApplication.class, args);
    }
}
