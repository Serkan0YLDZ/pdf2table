#!/usr/bin/env python3
"""
Docling Analysis Images to PDF Converter
Converts all Docling analysis images to a single PDF file
"""

import os
import sys
import glob
from pathlib import Path
from PIL import Image
import argparse
import logging

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def convert_images_to_pdf(input_dir, output_path, document_id, analysis_type="docling"):
    """
    Convert all analysis images in a directory to a single PDF
    
    Args:
        input_dir (str): Directory containing analysis images
        output_path (str): Output PDF file path
        document_id (str): Document ID for logging
        analysis_type (str): Type of analysis (docling, deepdoctection, etc.)
    
    Returns:
        bool: True if successful, False otherwise
    """
    try:
        # Find all image files in the directory
        image_extensions = ['*.png', '*.jpg', '*.jpeg', '*.bmp', '*.tiff', '*.tif']
        image_files = []
        
        for ext in image_extensions:
            image_files.extend(glob.glob(os.path.join(input_dir, ext)))
            image_files.extend(glob.glob(os.path.join(input_dir, ext.upper())))
        
        if not image_files:
            logger.warning(f"No image files found in {input_dir}")
            return False
        
        # Sort files by page number (extract number from filename)
        def extract_page_number(filename):
            try:
                # Extract number from filename (e.g., "page_1.png" -> 1)
                basename = os.path.basename(filename)
                # Remove extension and extract number
                name_without_ext = os.path.splitext(basename)[0]
                # Find all numbers in the filename
                import re
                numbers = re.findall(r'\d+', name_without_ext)
                if numbers:
                    return int(numbers[-1])  # Use the last number found
                return 0
            except:
                return 0
        
        image_files.sort(key=extract_page_number)
        logger.info(f"Found {len(image_files)} image files for document {document_id}")
        
        # Convert images to RGB mode and prepare for PDF
        images = []
        for img_path in image_files:
            try:
                with Image.open(img_path) as img:
                    # Convert to RGB if necessary (PDF requires RGB)
                    if img.mode != 'RGB':
                        img = img.convert('RGB')
                    
                    # Resize if too large (optional - for performance)
                    max_width = 1200
                    if img.width > max_width:
                        ratio = max_width / img.width
                        new_height = int(img.height * ratio)
                        img = img.resize((max_width, new_height), Image.Resampling.LANCZOS)
                    
                    images.append(img.copy())
                    logger.debug(f"Processed image: {os.path.basename(img_path)}")
                    
            except Exception as e:
                logger.error(f"Error processing image {img_path}: {e}")
                continue
        
        if not images:
            logger.error("No valid images could be processed")
            return False
        
        # Create output directory if it doesn't exist
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        
        # Save as PDF
        if len(images) == 1:
            images[0].save(output_path, 'PDF', resolution=150.0, quality=95)
        else:
            images[0].save(
                output_path, 
                'PDF', 
                resolution=150.0, 
                quality=95,
                save_all=True, 
                append_images=images[1:]
            )
        
        logger.info(f"Successfully created PDF with {len(images)} pages: {output_path}")
        return True
        
    except Exception as e:
        logger.error(f"Error converting images to PDF: {e}")
        return False

def create_analysis_pdf(document_id, analysis_type="docling", base_dir="uploads/analysis"):
    """
    Create PDF from analysis images for a specific document
    
    Args:
        document_id (str): Document ID
        analysis_type (str): Type of analysis
        base_dir (str): Base directory for analysis files
    
    Returns:
        str: Path to created PDF file, or None if failed
    """
    try:
        # Construct paths
        analysis_dir = os.path.join(base_dir, document_id, analysis_type)
        output_filename = f"{document_id}_{analysis_type}_analysis.pdf"
        output_path = os.path.join(base_dir, document_id, analysis_type, output_filename)
        
        logger.info(f"Creating analysis PDF for document {document_id}, type {analysis_type}")
        logger.info(f"Input directory: {analysis_dir}")
        logger.info(f"Output path: {output_path}")
        
        # Check if input directory exists
        if not os.path.exists(analysis_dir):
            logger.error(f"Analysis directory does not exist: {analysis_dir}")
            return None
        
        # Convert images to PDF
        success = convert_images_to_pdf(analysis_dir, output_path, document_id, analysis_type)
        
        if success:
            logger.info(f"Analysis PDF created successfully: {output_path}")
            return output_path
        else:
            logger.error(f"Failed to create analysis PDF for document {document_id}")
            return None
            
    except Exception as e:
        logger.error(f"Error in create_analysis_pdf: {e}")
        return None

def main():
    """Main function for command line usage"""
    parser = argparse.ArgumentParser(description='Convert Docling analysis images to PDF')
    parser.add_argument('document_id', help='Document ID')
    parser.add_argument('--analysis-type', default='docling', help='Analysis type (default: docling)')
    parser.add_argument('--base-dir', default='uploads/analysis', help='Base directory for analysis files')
    parser.add_argument('--output', help='Output PDF path (optional)')
    
    args = parser.parse_args()
    
    # Create PDF
    pdf_path = create_analysis_pdf(args.document_id, args.analysis_type, args.base_dir)
    
    if pdf_path:
        print(f"SUCCESS: {pdf_path}")
        sys.exit(0)
    else:
        print("FAILED: Could not create PDF")
        sys.exit(1)

if __name__ == "__main__":
    main()
