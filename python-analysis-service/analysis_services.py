"""
Analysis services for PDF layout analysis using deepdoctection and docling
"""

import asyncio
import logging
import os
from pathlib import Path
from typing import Callable, Dict, List, Optional

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

# Configure logging
logger = logging.getLogger(__name__)

# Analysis results directory
ANALYSIS_RESULTS_DIR = os.getenv("ANALYSIS_RESULTS_DIR", "/tmp/analysis_results")


class AnalysisOrchestrator:
    """Simplified analysis orchestrator"""
    
    def __init__(self):
        self.deepdoctection_analyzer = None
        self.docling_converter = None
    
    async def analyze_document(
        self, 
        document_id: str, 
        analysis_type: str, 
        file_path: str,
        progress_callback: Optional[Callable[[int, str], None]] = None
    ) -> List[Dict]:
        """Analyze document using specified analysis type"""
        
        logger.info(f"Starting {analysis_type} analysis for document: {document_id}")
        
        if progress_callback:
            progress_callback(30, f"Loading {analysis_type} models")
        
        # Process document based on type
        if analysis_type == "deepdoctection":
            results = await self._analyze_with_deepdoctection(document_id, file_path, progress_callback)
        elif analysis_type == "docling":
            results = await self._analyze_with_docling(document_id, file_path, progress_callback)
        else:
            raise ValueError(f"Unsupported analysis type: {analysis_type}")
        
        if progress_callback:
            progress_callback(90, "Saving results")
        
        logger.info(f"Analysis completed for document: {document_id}")
        return results

    async def _analyze_with_deepdoctection(self, document_id: str, file_path: str, progress_callback) -> List[Dict]:
        """Analyze with deepdoctection"""
        if self.deepdoctection_analyzer is None:
            self.deepdoctection_analyzer = self._init_deepdoctection()
        
        if progress_callback:
            progress_callback(50, "Running deepdoctection analysis")
        
        # Run analysis in thread pool
        loop = asyncio.get_event_loop()
        results = await loop.run_in_executor(
            None, 
            self._process_deepdoctection_sync, 
            document_id, 
            file_path, 
            progress_callback
        )
        return results
    
    async def _analyze_with_docling(self, document_id: str, file_path: str, progress_callback) -> List[Dict]:
        """Analyze with docling"""
        if self.docling_converter is None:
            self.docling_converter = self._init_docling()
        
        if progress_callback:
            progress_callback(50, "Running docling analysis")
        
        # Run analysis in thread pool
        loop = asyncio.get_event_loop()
        results = await loop.run_in_executor(
            None, 
            self._process_docling_sync, 
            document_id, 
            file_path, 
            progress_callback
        )
        return results

    
    def _init_deepdoctection(self):
        """Initialize deepdoctection analyzer"""
        try:
            from deepdoctection.analyzer import get_dd_analyzer
            
            config_overwrite = [
                "USE_LAYOUT=True",
                "USE_TABLE_SEGMENTATION=True", 
                "USE_OCR=False",
                "USE_LAYOUT_NMS=True"
            ]
            
            analyzer = get_dd_analyzer(
                reset_config_file=True,
                config_overwrite=config_overwrite
            )
            
            logger.info("Deepdoctection analyzer initialized successfully")
            return analyzer
            
        except Exception as e:
            logger.error(f"Failed to initialize deepdoctection analyzer: {e}")
            raise
    
    def _init_docling(self):
        """Initialize docling converter"""
        try:
            from docling.document_converter import DocumentConverter, FormatOption
            from docling.datamodel.base_models import InputFormat
            from docling.datamodel.pipeline_options import PdfPipelineOptions
            from docling.pipeline.standard_pdf_pipeline import StandardPdfPipeline
            from docling.backend.docling_parse_v4_backend import DoclingParseV4DocumentBackend
            from docling.datamodel.layout_model_specs import DOCLING_LAYOUT_V2
            from docling.datamodel.pipeline_options import LayoutOptions
            
            layout_options = LayoutOptions(
                model_spec=DOCLING_LAYOUT_V2,
                create_orphan_clusters=True,
                keep_empty_clusters=False
            )
            
            pdf_pipeline_options = PdfPipelineOptions(
                do_table_structure=True,
                do_ocr=False,
                generate_page_images=True,
                layout_options=layout_options,
                images_scale=0.8
            )
            
            pdf_format_option = FormatOption(
                pipeline_cls=StandardPdfPipeline,
                backend=DoclingParseV4DocumentBackend,
                pipeline_options=pdf_pipeline_options
            )
            
            converter = DocumentConverter(
                allowed_formats=[InputFormat.PDF],
                format_options={InputFormat.PDF: pdf_format_option}
            )
            
            logger.info("Docling converter initialized successfully")
            return converter
            
        except Exception as e:
            logger.error(f"Failed to initialize docling converter: {e}")
            raise
    
    def _process_deepdoctection_sync(self, document_id: str, file_path: str, progress_callback) -> List[Dict]:
        """Synchronous deepdoctection processing"""
        results = []
        
        try:
            df = self.deepdoctection_analyzer.analyze(path=file_path)
            
            if progress_callback:
                progress_callback(70, "Processing analysis results")
            
            for dp in df:
                page_number = dp.page_number
                
                if progress_callback:
                    progress_callback(60 + (page_number * 5), f"Processing page {page_number}")
                
                result_dir = Path(ANALYSIS_RESULTS_DIR) / document_id / "deepdoctection"
                result_dir.mkdir(parents=True, exist_ok=True)
                
                result_file = result_dir / f"page_{page_number}.png"
                self._create_deepdoctection_visualization(dp, str(result_file))
                
                results.append({
                    "page_number": page_number,
                    "file_path": str(result_file),
                    "analysis_type": "deepdoctection"
                })
            
            logger.info(f"Deepdoctection analysis completed: {len(results)} pages processed")
            return results
            
        except Exception as e:
            logger.error(f"Error in deepdoctection processing: {e}")
            raise
    
    def _process_docling_sync(self, document_id: str, file_path: str, progress_callback) -> List[Dict]:
        """Synchronous docling processing"""
        results = []
        
        try:
            result = self.docling_converter.convert(file_path)
            
            if progress_callback:
                progress_callback(70, "Processing analysis results")
            
            for page_no, page in enumerate(result.pages, 1):
                if progress_callback:
                    progress_callback(60 + (page_no * 5), f"Processing page {page_no}")
                
                result_dir = Path(ANALYSIS_RESULTS_DIR) / document_id / "docling"
                result_dir.mkdir(parents=True, exist_ok=True)
                
                result_file = result_dir / f"page_{page_no}.png"
                self._create_docling_visualization(page, str(result_file))
                
                results.append({
                    "page_number": page_no,
                    "file_path": str(result_file),
                    "analysis_type": "docling"
                })
            
            logger.info(f"Docling analysis completed: {len(results)} pages processed")
            return results
            
        except Exception as e:
            logger.error(f"Error in docling processing: {e}")
            raise
    
    def _create_deepdoctection_visualization(self, datapoint, output_path: str):
        """Create layout visualization for deepdoctection"""
        try:
            # Get image
            image = datapoint.image
            
            # Convert to PIL Image if needed
            if hasattr(image, 'numpy'):
                image_np = image.numpy()
            else:
                image_np = np.array(image)
            
            # Convert to RGB if needed
            if len(image_np.shape) == 3 and image_np.shape[2] == 3:
                pil_image = Image.fromarray(image_np)
            else:
                pil_image = Image.fromarray(image_np).convert('RGB')
            
            # Create drawing context
            draw = ImageDraw.Draw(pil_image)
            
            # Try to load a font
            try:
                font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
            except:
                font = ImageFont.load_default()
            
            # Draw layout annotations
            for annotation in datapoint.layouts:
                bbox = annotation.bounding_box
                category = annotation.category_name
                
                # Draw bounding box
                x1, y1, x2, y2 = bbox.ulx, bbox.uly, bbox.lrx, bbox.lry
                draw.rectangle([x1, y1, x2, y2], outline='red', width=2)
                
                # Draw category label
                draw.text((x1, y1-20), category, fill='red', font=font)
            
            # Save image
            pil_image.save(output_path, 'PNG')
            logger.info(f"Deepdoctection layout visualization saved: {output_path}")
            
        except Exception as e:
            logger.error(f"Error creating deepdoctection visualization: {e}")
            self._create_placeholder_image(output_path, "Deepdoctection")
    
    def _create_docling_visualization(self, page, output_path: str):
        """Create layout visualization for docling"""
        try:
            # Get page image using the get_image method
            try:
                page_image = page.get_image()
                if page_image is not None:
                    # Convert to PIL Image
                    if hasattr(page_image, 'numpy'):
                        image_np = page_image.numpy()
                    else:
                        image_np = np.array(page_image)
                    
                    pil_image = Image.fromarray(image_np).convert('RGB')
                else:
                    # Create a placeholder image
                    pil_image = Image.new('RGB', (800, 1000), 'white')
            except Exception as e:
                logger.warning(f"Could not get page image: {e}")
                # Create a placeholder image
                pil_image = Image.new('RGB', (800, 1000), 'white')
            
            # Create drawing context
            draw = ImageDraw.Draw(pil_image)
            
            # Try to load a font
            try:
                font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
            except:
                font = ImageFont.load_default()
            
            # Try different ways to access layout elements
            layout_elements = []
            
            # Method 1: Direct predictions.layout
            if hasattr(page, 'predictions') and page.predictions:
                if hasattr(page.predictions, 'layout') and page.predictions.layout:
                    # Docling uses 'clusters' not 'elements'
                    if hasattr(page.predictions.layout, 'clusters'):
                        layout_elements.extend(page.predictions.layout.clusters)
                    elif hasattr(page.predictions.layout, 'elements'):
                        layout_elements.extend(page.predictions.layout.elements)
            
            # Method 2: Check if page has direct layout attribute
            if hasattr(page, 'layout') and page.layout:
                if hasattr(page.layout, 'elements'):
                    layout_elements.extend(page.layout.elements)
            
            # Draw layout elements
            if layout_elements:
                logger.info(f"Drawing {len(layout_elements)} layout elements")
                for element in layout_elements:
                    # Try to get bounding box
                    bbox = None
                    if hasattr(element, 'bbox') and element.bbox:
                        bbox = element.bbox
                    elif hasattr(element, 'bounding_box') and element.bounding_box:
                        bbox = element.bounding_box
                    elif hasattr(element, 'box') and element.box:
                        bbox = element.box
                    
                    # Try to get category/label
                    category = 'unknown'
                    for label_attr in ['label', 'category', 'type', 'class', 'element_type']:
                        if hasattr(element, label_attr):
                            category = getattr(element, label_attr)
                            # Convert enum to string if needed
                            if hasattr(category, 'value'):
                                category = category.value
                            elif hasattr(category, 'name'):
                                category = category.name
                            break
                    
                    if bbox:
                        # Extract coordinates - Docling uses l, t, r, b
                        if hasattr(bbox, 'l') and hasattr(bbox, 't') and hasattr(bbox, 'r') and hasattr(bbox, 'b'):
                            x1, y1, x2, y2 = bbox.l, bbox.t, bbox.r, bbox.b
                        elif hasattr(bbox, 'x0') and hasattr(bbox, 'y0') and hasattr(bbox, 'x1') and hasattr(bbox, 'y1'):
                            x1, y1, x2, y2 = bbox.x0, bbox.y0, bbox.x1, bbox.y1
                        elif hasattr(bbox, 'ulx') and hasattr(bbox, 'uly') and hasattr(bbox, 'lrx') and hasattr(bbox, 'lry'):
                            x1, y1, x2, y2 = bbox.ulx, bbox.uly, bbox.lrx, bbox.lry
                        elif hasattr(bbox, 'left') and hasattr(bbox, 'top') and hasattr(bbox, 'right') and hasattr(bbox, 'bottom'):
                            x1, y1, x2, y2 = bbox.left, bbox.top, bbox.right, bbox.bottom
                        else:
                            continue
                        
                        # Choose color based on category
                        color = 'blue'  # default
                        if 'table' in str(category).lower():
                            color = 'red'
                        elif 'picture' in str(category).lower() or 'image' in str(category).lower():
                            color = 'green'
                        elif 'formula' in str(category).lower():
                            color = 'purple'
                        elif 'text' in str(category).lower():
                            color = 'blue'
                        elif 'key_value' in str(category).lower():
                            color = 'orange'
                        
                        # Draw bounding box
                        draw.rectangle([x1, y1, x2, y2], outline=color, width=2)
                        
                        # Draw category label
                        draw.text((x1, y1-20), str(category), fill=color, font=font)
            
            # Save image
            pil_image.save(output_path, 'PNG')
            logger.info(f"Docling layout visualization saved: {output_path}")
            
        except Exception as e:
            logger.error(f"Error creating docling visualization: {e}")
            self._create_placeholder_image(output_path, "Docling")
    
    def _create_placeholder_image(self, output_path: str, analysis_type: str):
        """Create a placeholder image when visualization fails"""
        try:
            # Create a simple placeholder image
            img = Image.new('RGB', (400, 600), 'lightgray')
            draw = ImageDraw.Draw(img)
            
            # Add text
            try:
                font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 24)
            except:
                font = ImageFont.load_default()
            
            draw.text((50, 250), f"{analysis_type} Analysis", fill='black', font=font)
            draw.text((50, 300), "Visualization not available", fill='gray', font=font)
            
            img.save(output_path, 'PNG')
            logger.info(f"Placeholder image created: {output_path}")
            
        except Exception as e:
            logger.error(f"Error creating placeholder image: {e}")
