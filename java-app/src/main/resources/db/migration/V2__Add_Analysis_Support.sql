-- Migration script for PDF Analysis System
-- Adds analysis support to existing documents table and creates analysis_files table

-- Add analysis fields to existing documents table
ALTER TABLE documents ADD COLUMN analysis_status VARCHAR(50) DEFAULT 'NOT_STARTED';
ALTER TABLE documents ADD COLUMN analysis_results JSONB;

-- Create index for analysis_status
CREATE INDEX idx_documents_analysis_status ON documents(analysis_status);

-- Create analysis_files table
CREATE TABLE analysis_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    analysis_type VARCHAR(50) NOT NULL,
    page_number INTEGER NOT NULL,
    result_file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for analysis_files
CREATE INDEX idx_analysis_files_document_type ON analysis_files(document_id, analysis_type);
CREATE INDEX idx_analysis_files_created_at ON analysis_files(created_at);

-- Add foreign key constraint
ALTER TABLE analysis_files ADD CONSTRAINT fk_analysis_files_document 
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

-- Add check constraint for analysis_type
ALTER TABLE analysis_files ADD CONSTRAINT chk_analysis_type 
    CHECK (analysis_type IN ('deepdoctection', 'docling'));

-- Add check constraint for page_number
ALTER TABLE analysis_files ADD CONSTRAINT chk_page_number 
    CHECK (page_number > 0);
