# PDF Analysis Service

Bu servis, Java Vaadin uygulaması için PDF layout analizi yapmak üzere geliştirilmiş FastAPI tabanlı bir Python servisidir. Deepdoctection ve Docling kütüphanelerini kullanarak PDF dosyalarının düzen analizini gerçekleştirir.

## Özellikler

- **FastAPI** tabanlı modern web API
- **Deepdoctection** ile PDF layout analizi
- **Docling** ile gelişmiş PDF anlama
- **Asenkron** işleme ve background task'lar
- **Real-time** progress tracking
- **CORS** desteği
- **Health check** endpoint'i

## Kurulum

### 1. Python Sanal Ortamı Oluşturma

```bash
cd python-analysis-service
python -m venv venv
source venv/bin/activate  # Linux/Mac
# veya
venv\Scripts\activate  # Windows
```

### 2. Bağımlılıkları Yükleme

```bash
pip install -r requirements.txt
```

### 3. Sistem Bağımlılıkları

#### Ubuntu/Debian:
```bash
sudo apt-get update
sudo apt-get install -y tesseract-ocr libtesseract-dev
sudo apt-get install -y libgl1-mesa-glx libglib2.0-0
```

#### macOS:
```bash
brew install tesseract
```

#### Windows:
- Tesseract'ı [resmi siteden](https://github.com/UB-Mannheim/tesseract/wiki) indirin ve kurun

### 4. Model Dosyalarını İndirme

Deepdoctection ve Docling modelleri ilk çalıştırmada otomatik olarak indirilecektir. Bu işlem biraz zaman alabilir.

## Kullanım

### Geliştirme Ortamında Çalıştırma

```bash
python main.py
```

Veya uvicorn ile:

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### Production Ortamında Çalıştırma

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

## API Endpoints

### Health Check
```
GET /health
```

### Analiz Başlatma
```
POST /analyze
Content-Type: application/json

{
    "document_id": "uuid-string",
    "analysis_type": "deepdoctection" | "docling",
    "file_path": "/path/to/pdf/file",
    "file_name": "document.pdf"
}
```

### Task Durumu
```
GET /status/{task_id}
```

### Analiz Sonuçları
```
GET /results/{task_id}
```

## Konfigürasyon

### Environment Variables

- `ANALYSIS_RESULTS_DIR`: Analiz sonuçlarının kaydedileceği dizin (varsayılan: `/tmp/analysis_results`)
- `MAX_WORKERS`: Maksimum worker sayısı (varsayılan: `2`)

### Örnek Konfigürasyon

```bash
export ANALYSIS_RESULTS_DIR="/app/analysis_results"
export MAX_WORKERS=4
```

## Analiz Türleri

### Deepdoctection
- Detectron2 tabanlı layout detection
- Tablo segmentasyonu
- Görsel element tespiti
- Hızlı işleme

### Docling
- IBM Granite modelleri
- Gelişmiş PDF anlama
- Daha detaylı layout analizi
- Yapısal element tespiti

## Sonuç Formatı

Analiz sonuçları PNG formatında görsel dosyalar olarak kaydedilir:

```
{ANALYSIS_RESULTS_DIR}/
├── {document_id}/
│   ├── deepdoctection/
│   │   ├── page_1.png
│   │   ├── page_2.png
│   │   └── ...
│   └── docling/
│       ├── page_1.png
│       ├── page_2.png
│       └── ...
```

## Troubleshooting

### Yaygın Sorunlar

1. **Model İndirme Hatası**
   - İnternet bağlantısını kontrol edin
   - Proxy ayarlarını kontrol edin
   - Disk alanını kontrol edin

2. **Memory Hatası**
   - `MAX_WORKERS` değerini azaltın
   - Daha küçük PDF dosyaları ile test edin

3. **Tesseract Hatası**
   - Tesseract'ın doğru kurulduğunu kontrol edin
   - PATH değişkenini kontrol edin

### Log Seviyesi Ayarlama

```bash
export LOG_LEVEL=DEBUG
python main.py
```

## Geliştirme

### Yeni Analiz Türü Ekleme

1. `analysis_services.py` dosyasında yeni bir service sınıfı oluşturun
2. `AnalysisOrchestrator` sınıfında yeni servisi entegre edin
3. API endpoint'lerini güncelleyin

### Test Etme

```bash
# Health check
curl http://localhost:8000/health

# Analiz başlatma
curl -X POST http://localhost:8000/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "document_id": "test-123",
    "analysis_type": "deepdoctection",
    "file_path": "/path/to/test.pdf",
    "file_name": "test.pdf"
  }'
```

## Performans

- **Deepdoctection**: ~2-5 saniye/sayfa
- **Docling**: ~3-7 saniye/sayfa
- **Memory kullanımı**: ~2-4 GB
- **Disk kullanımı**: ~50-100 MB/model

## Güvenlik

- CORS ayarları production'da kısıtlanmalı
- File path validation eklenmeli
- Rate limiting uygulanmalı
- Authentication/authorization eklenmeli
