# Plagiarism-Checker
PlagScan Pro is an advanced plagiarism detection system built with Spring Boot. It compares text and uploaded documents using a weighted pipeline of 7 similarity algorithms, performs internet source checks via web scraping, stores analysis history in H2/MySQL, and exposes REST APIs plus a dark-themed UI for report export and batch comparison.


# PlagScan Pro — Advanced Plagiarism Detection System

> Production-grade plagiarism detection powered by **7 algorithms**, with **internet search integration**, **database persistence**, and a **premium dark-themed UI**.

![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## 🚀 Quick Start

### Prerequisites
- **Java 17+** — [Download JDK](https://adoptium.net/)
- **Maven 3.8+** — [Download Maven](https://maven.apache.org/)
- **MySQL** (optional) — H2 in-memory database works out of the box

### Run the Application

```bash
cd plagiarism-checker
mvn clean package -DskipTests
mvn spring-boot:run
```

Then open: **http://localhost:8080**

---

## 🧠 Core Features

### 1. Document Compare (7 Algorithms)
Compare two documents using a weighted pipeline of 7 similarity algorithms:

| Algorithm | Weight | Description |
|-----------|--------|-------------|
| Rabin-Karp | 20% | Rolling hash k-gram overlap detection |
| KMP | 10% | Exact phrase matching (Knuth-Morris-Pratt) |
| Winnowing | 20% | Document fingerprinting (MOSS-style) |
| Levenshtein | 20% | Fuzzy sentence-level edit distance |
| Cosine TF | 15% | Term frequency vector space model |
| Jaccard | 5% | Set-based word overlap |
| Synonym Map | 10% | Paraphrase detection via synonym dictionary |

### 2. Internet Scan 🌐
Scan your text against the internet:
- Extracts key sentences as search queries
- Searches Google via Jsoup web scraping (no API key needed!)
- Fetches and cleans web page content
- Compares against each source using all 7 algorithms
- Returns matched sources with URLs, similarity scores, and matching snippets

### 3. File Upload Support
Supports `.txt`, `.pdf`, and `.docx` file uploads:
- PDF parsing via Apache PDFBox
- DOCX parsing via Apache POI
- Plain text direct reading

### 4. Batch Compare
Compare N documents against each other in an N×N matrix:
- Similarity heatmap visualization
- Flagged pairs with high similarity
- Great for classroom plagiarism detection

### 5. Database Persistence
All analysis results are stored in the database:
- **H2** (default) — zero configuration, file-based
- **MySQL** — production-ready, uncomment in `application.properties`
- Full history with summary statistics

### 6. Premium UI
Modern dark-themed interface:
- Plagiarism heatmap visualization
- Side-by-side diff with highlighted matches
- Sentence-level analysis with confidence scores
- Real-time progress feedback during internet scans
- Export reports as HTML / Print to PDF

---

## 📡 API Endpoints

### Document Analysis
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/analyze/text` | Compare two text inputs |
| POST | `/api/analyze/files` | Compare two uploaded files |
| POST | `/api/analyze/url` | Compare text vs URL |
| POST | `/api/v2/analyze` | Full analysis with history |

### Internet Scan
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/scan` | Scan text against the internet |

### Batch & Reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/batch` | N×N batch comparison |
| POST | `/api/v2/report/html` | Export HTML report |

### History
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v2/history` | Get all past analyses |
| GET | `/api/v2/history/summary` | Get summary statistics |
| DELETE | `/api/v2/history/{id}` | Delete a history entry |
| DELETE | `/api/v2/history` | Clear all history |

### Utilities
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/v2/synonyms?word=fast` | Lookup synonyms |
| GET | `/api/v2/synonyms/check?a=fast&b=quick` | Check if synonyms |

---

## 🗂️ Project Structure

```
plagiarism-checker/
├── pom.xml                          # Maven dependencies
├── schema.sql                       # Database schema reference
├── src/main/java/com/plagchecker/
│   ├── PlagCheckerApplication.java  # Spring Boot entry point
│   ├── api/
│   │   ├── PlagiarismController.java     # Basic REST endpoints
│   │   ├── ExtendedController.java       # V2 endpoints (scan, batch, history)
│   │   └── PlagiarismAnalysisService.java # Analysis pipeline
│   ├── core/
│   │   ├── RabinKarp.java                # Rolling hash algorithm
│   │   ├── KMPMatcher.java               # Knuth-Morris-Pratt
│   │   ├── Winnowing.java                # MOSS-style fingerprinting
│   │   ├── Levenshtein.java              # Edit distance
│   │   └── SimilarityScorer.java         # Simple scorer
│   ├── search/
│   │   ├── InternetSearchService.java    # Web search (Google scraping)
│   │   ├── WebContentExtractor.java      # HTML text extraction
│   │   └── InternetScanService.java      # Scan orchestration
│   ├── preprocessing/
│   │   └── TextPreprocessor.java         # Tokenization, stopwords, n-grams
│   ├── model/
│   │   ├── AnalysisResult.java           # Analysis result model
│   │   └── InternetScanResult.java       # Internet scan model
│   ├── entity/
│   │   ├── PlagiarismCheck.java          # JPA entity
│   │   └── PlagiarismCheckRepository.java # JPA repository
│   ├── io/
│   │   └── DocumentReader.java           # PDF/DOCX/TXT reader
│   ├── history/
│   │   └── AnalysisHistoryStore.java     # DB-backed history
│   ├── report/
│   │   └── ReportGenerator.java          # HTML report export
│   ├── synonyms/
│   │   └── SynonymMap.java               # Synonym dictionary
│   ├── batch/
│   │   └── BatchAnalysisService.java     # Batch comparison
│   └── config/
│       ├── RateLimitFilter.java          # Rate limiting (60 req/min)
│       └── WebConfig.java                # CORS config
└── src/main/resources/
    ├── application.properties            # Configuration
    └── static/
        └── index.html                    # Frontend UI
```

---

## 🔒 Security Features

- **Rate Limiting**: 60 requests/minute per IP address
- **Input Validation**: All inputs sanitized and validated
- **File Size Limits**: Max 10MB upload per file
- **CORS**: Configurable cross-origin policy
- **Timeout Protection**: Web fetch operations have 8-second timeouts
- **API Key Protection**: Google API keys never exposed to frontend

---

## 🗄️ Database Setup

### Option A: H2 (Default — Zero Config)
The app uses H2 file-based storage by default. Data persists in `./plagscan-data.mv.db`.
Access the H2 console at: http://localhost:8080/h2-console

### Option B: MySQL
1. Install MySQL and create the database:
   ```sql
   CREATE DATABASE plagcheck_db;
   ```
2. Update `application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/plagcheck_db
   spring.datasource.username=root
   spring.datasource.password=yourpassword
   ```
3. Run the app — tables are auto-created by Hibernate.

---

## 📊 Sample Test Cases

### Test Case 1: Exact Copy
```
Text A: "Machine learning is a subset of artificial intelligence that provides systems the ability to learn from data."
Text B: "Machine learning is a subset of artificial intelligence that provides systems the ability to learn from data."
Expected: ~100% similarity, HIGH_PLAGIARISM
```

### Test Case 2: Paraphrase
```
Text A: "The quick brown fox jumps over the lazy dog near the river bank."
Text B: "A fast dark fox leaps above a sleepy canine beside the stream bank."
Expected: Moderate similarity (synonym detection should catch quick→fast, jumps→leaps)
```

### Test Case 3: Original Content
```
Text A: "Neural networks have revolutionized computer vision and natural language processing."
Text B: "The stock market experienced significant volatility during the third quarter of 2024."
Expected: ~0% similarity, ORIGINAL
```

---

## ⚡ Performance

- **Parallel Processing**: Multithreaded search queries and web content fetching
- **Result Caching**: Search results and web content are cached to reduce API calls
- **Efficient Algorithms**: O(n) Rabin-Karp, O(min(m,n)) space-optimized Levenshtein
- **Connection Pooling**: HikariCP for database connections

---

## 📄 License

MIT License — free for academic and commercial use.
