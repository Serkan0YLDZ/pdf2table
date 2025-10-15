"""
PDF Analysis Service using FastAPI
Integrates deepdoctection and docling for PDF layout analysis
"""

import asyncio
import json
import logging
import os
import uuid
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional

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

# Global task storage (in production, use Redis or database)
tasks: Dict[str, Dict] = {}

# Configuration
ANALYSIS_RESULTS_DIR = os.getenv("ANALYSIS_RESULTS_DIR", "/tmp/analysis_results")
MAX_WORKERS = int(os.getenv("MAX_WORKERS", "2"))

# Ensure results directory exists
os.makedirs(ANALYSIS_RESULTS_DIR, exist_ok=True)


class AnalysisRequest(BaseModel):
    document_id: str
    analysis_type: str
    file_path: str
    file_name: str


class TaskStatus(BaseModel):
    task_id: str
    status: str
    progress: int
    message: str
    created_at: datetime
    completed_at: Optional[datetime] = None
    error: Optional[str] = None


class AnalysisResult(BaseModel):
    task_id: str
    document_id: str
    analysis_type: str
    results: List[Dict]


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
    
    # Generate task ID
    task_id = str(uuid.uuid4())
    
    # Create task entry
    task = {
        "task_id": task_id,
        "document_id": request.document_id,
        "analysis_type": request.analysis_type,
        "file_path": request.file_path,
        "file_name": request.file_name,
        "status": "pending",
        "progress": 0,
        "message": "Analysis queued",
        "created_at": datetime.now(),
        "completed_at": None,
        "error": None
    }
    
    tasks[task_id] = task
    
    # Start background task
    background_tasks.add_task(process_analysis, task_id)
    
    return {
        "task_id": task_id,
        "status": "started",
        "message": "Analysis started successfully"
    }


@app.get("/status/{task_id}")
async def get_task_status(task_id: str):
    """Get task status"""
    if task_id not in tasks:
        raise HTTPException(status_code=404, detail="Task not found")
    
    task = tasks[task_id]
    return TaskStatus(**task)


@app.get("/results/{task_id}")
async def get_task_results(task_id: str):
    """Get task results"""
    if task_id not in tasks:
        raise HTTPException(status_code=404, detail="Task not found")
    
    task = tasks[task_id]
    
    if task["status"] != "completed":
        raise HTTPException(status_code=400, detail="Task not completed yet")
    
    # Get results from file system
    results = await get_analysis_results(task["document_id"], task["analysis_type"])
    
    return AnalysisResult(
        task_id=task_id,
        document_id=task["document_id"],
        analysis_type=task["analysis_type"],
        results=results
    )


async def process_analysis(task_id: str):
    """Process PDF analysis in background"""
    task = tasks[task_id]
    
    try:
        logger.info(f"Processing analysis task: {task_id}")
        
        # Update status
        task["status"] = "running"
        task["progress"] = 10
        task["message"] = "Initializing analysis"
        
        # Import analysis services
        from analysis_services import AnalysisOrchestrator
        
        # Create orchestrator
        orchestrator = AnalysisOrchestrator()
        
        # Process analysis
        task["progress"] = 20
        task["message"] = f"Running {task['analysis_type']} analysis"
        
        # Convert relative path to absolute path
        file_path = task["file_path"]
        if not os.path.isabs(file_path):
            # If it's a relative path starting with ./uploads/, look in the java-app directory
            if file_path.startswith("./uploads/") or file_path.startswith("uploads/"):
                # Remove ./ prefix if present
                relative_path = file_path.replace("./", "")
                # Look in the java-app directory (one level up from python-analysis-service)
                java_app_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
                file_path = os.path.join(java_app_dir, "java-app", relative_path)
            else:
                # For other relative paths, make it absolute from the current working directory
                file_path = os.path.abspath(file_path)
        
        # Check if file exists
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"PDF file not found: {file_path}")
        
        results = await orchestrator.analyze_document(
            document_id=task["document_id"],
            analysis_type=task["analysis_type"],
            file_path=file_path,
            progress_callback=lambda p, msg: update_task_progress(task_id, p, msg)
        )
        
        # Update final status
        task["status"] = "completed"
        task["progress"] = 100
        task["message"] = "Analysis completed successfully"
        task["completed_at"] = datetime.now()
        
        logger.info(f"Analysis completed for task: {task_id}")
        
    except Exception as e:
        logger.error(f"Error processing analysis task {task_id}: {str(e)}")
        task["status"] = "failed"
        task["error"] = str(e)
        task["message"] = f"Analysis failed: {str(e)}"
        task["completed_at"] = datetime.now()


def update_task_progress(task_id: str, progress: int, message: str):
    """Update task progress"""
    if task_id in tasks:
        tasks[task_id]["progress"] = progress
        tasks[task_id]["message"] = message


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
