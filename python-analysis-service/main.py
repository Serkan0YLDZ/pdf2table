"""
PDF Analysis Service using FastAPI
Integrates deepdoctection and docling for PDF layout analysis
"""

import logging
import os
from datetime import datetime
from pathlib import Path
from typing import List, Dict

import aiofiles
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="PDF Analysis Service",
    description="Service for PDF layout analysis using deepdoctection and docling",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration
ANALYSIS_RESULTS_DIR = os.getenv("ANALYSIS_RESULTS_DIR", "../java-app/uploads/analysis")

# Ensure results directory exists
os.makedirs(ANALYSIS_RESULTS_DIR, exist_ok=True)


class AnalysisRequest(BaseModel):
    document_id: str
    analysis_type: str
    file_path: str
    file_name: str


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "PDF Analysis Service",
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }


@app.get("/results/{document_id}/{analysis_type}")
async def get_analysis_results_endpoint(document_id: str, analysis_type: str):
    """Get analysis results for a document and analysis type"""
    try:
        results = await get_analysis_results(document_id, analysis_type)
        return {"results": results, "count": len(results)}
    except Exception as e:
        logger.error(f"Error getting analysis results: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/analyze")
async def start_analysis(request: AnalysisRequest, background_tasks: BackgroundTasks):
    """Start PDF analysis"""
    logger.info(f"Starting analysis for document: {request.document_id}, type: {request.analysis_type}")
    
    # Start background task - basit queue
    background_tasks.add_task(process_analysis_simple, request)
    
    return {
        "status": "started",
        "message": "Analysis started successfully"
    }


async def process_analysis_simple(request: AnalysisRequest):
    """Process PDF analysis in background - basit versiyon"""
    try:
        logger.info(f"Processing analysis for document: {request.document_id}, type: {request.analysis_type}")
        
        # Import analysis services
        from analysis_services import AnalysisOrchestrator
        
        # Create orchestrator
        orchestrator = AnalysisOrchestrator()
        
        # Convert relative path to absolute path
        file_path = request.file_path
        if not os.path.isabs(file_path):
            if file_path.startswith("./uploads/") or file_path.startswith("uploads/"):
                relative_path = file_path.replace("./", "")
                java_app_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
                file_path = os.path.join(java_app_dir, "java-app", relative_path)
            else:
                file_path = os.path.abspath(file_path)
        
        # Check if file exists
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"PDF file not found: {file_path}")
        
        # Process analysis
        await orchestrator.analyze_document(
            document_id=request.document_id,
            analysis_type=request.analysis_type,
            file_path=file_path
        )
        
        logger.info(f"Analysis completed for document: {request.document_id}")
        
    except Exception as e:
        logger.error(f"Error processing analysis for document {request.document_id}: {str(e)}")


async def get_analysis_results(document_id: str, analysis_type: str) -> List[Dict]:
    """Get analysis results from file system"""
    results = []
    
    # Look for result files
    result_dir = Path(ANALYSIS_RESULTS_DIR) / document_id / analysis_type
    
    if result_dir.exists():
        for file_path in sorted(result_dir.glob("page_*.png")):
            # Extract page number from filename
            page_number = int(file_path.stem.split("_")[1])
            
            results.append({
                "page_number": page_number,
                "file_path": str(file_path),
                "file_size": file_path.stat().st_size,
                "created_at": datetime.fromtimestamp(file_path.stat().st_ctime).isoformat()
            })
    
    return results


@app.get("/results/{document_id}/{analysis_type}")
async def get_analysis_results_endpoint(document_id: str, analysis_type: str):
    """Get analysis results for a document and analysis type"""
    try:
        results = await get_analysis_results(document_id, analysis_type)
        return {"results": results, "count": len(results)}
    except Exception as e:
        logger.error(f"Error getting analysis results: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
