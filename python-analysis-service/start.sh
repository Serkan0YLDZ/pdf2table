#!/bin/bash
cd /Users/serkan/Github/pdf2table/python-analysis-service
source myenv/bin/activate
export ANALYSIS_RESULTS_DIR="../java-app/uploads/analysis"
python main.py
