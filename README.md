# Plagiarism-Checker

Plagiarism-Checker is an advanced Spring Boot application for document comparison and internet-based source scanning. It uses a multi-algorithm similarity engine, supports file uploads, stores analysis history, and exposes REST APIs for automated plagiarism detection.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- MySQL (optional, H2 is configured by default)

### Run locally

```bash
cd plagiarism-checker
mvn clean package -DskipTests
mvn spring-boot:run
```

Open: **http://localhost:8080**

---

## 🧠 What it does

- Compares documents with a weighted pipeline of 7 similarity algorithms
- Scans web sources via HTML scraping and compares matched content
- Supports `.txt`, `.pdf`, and `.docx` uploads
- Stores results in a database for history and reporting
- Exposes REST endpoints for text, file, batch, and internet analysis
- Includes a modern dark-themed UI for reports and analysis review

---

## 🔍 Core Features

### Multi-algorithm comparison
- Rabin-Karp: rolling hash k-gram comparison
- KMP: exact phrase detection
- Winnowing: fingerprint-based plagiarism detection
- Levenshtein: edit distance for fuzzy matching
- Cosine TF: vector similarity for term distributions
- Jaccard: set overlap scoring
- Synonym matching: paraphrase detection using a synonym dictionary

### Internet scan
- Extracts key sentences and builds search queries
- Scrapes pages using Jsoup
- Compares fetched web content with the submitted text
- Returns source URLs, similarity scores, and matched snippets

### File upload support
- `.txt`: plain text
- `.pdf`: Apache PDFBox
- `.docx`: Apache POI

### Batch comparison
- Runs N×N document comparisons
- Detects similar pairs across multiple files
- Useful for classroom or library plagiarism checks

### History and persistence
- Default H2 database for zero-config use
- Optional MySQL support via `application.properties`
- Stores past results, summaries, and report metadata

---

## 📡 API Endpoints

### Document analysis
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/analyze/text` | Compare two text inputs |
| POST | `/api/analyze/files` | Compare two uploaded files |
| POST | `/api/analyze/url` | Compare text against a URL |
| POST | `/api/v2/analyze` | Full analysis with history |

### Internet scan
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/scan` | Scan text against internet sources |

### Batch & reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/batch` | N×N batch document comparison |
| POST | `/api/v2/report/html` | Export HTML report |

### History
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v2/history` | Retrieve all analysis history |
| GET | `/api/v2/history/summary` | Get summary statistics |
| DELETE | `/api/v2/history/{id}` | Delete a history entry |
| DELETE | `/api/v2/history` | Clear analysis history |

### Utilities
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/v2/synonyms?word=fast` | Lookup synonyms |
| GET | `/api/v2/synonyms/check?a=fast&b=quick` | Compare synonyms |

---

## 🗂️ Project structure

```
plagiarism-checker/
├── pom.xml
├── README.md
├── src/main/java/com/plagchecker/
│   ├── PlagCheckerApplication.java
│   ├── api/
│   ├── batch/
│   ├── config/
│   ├── core/
│   ├── entity/
│   ├── history/
│   ├── io/
│   ├── model/
│   ├── preprocessing/
│   ├── report/
│   ├── search/
│   ├── synonyms/
│   └── ...
└── src/main/resources/
    ├── application.properties
    └── static/index.html
```

---

## 🔒 Security & reliability

- Rate limiting for API protection
- Input validation for text and file uploads
- File size limits for uploads
- CORS configuration
- Timeout controls for remote web fetches

---

## 🗄️ Database setup

### H2 (default)
Zero configuration; runs out of the box.

### MySQL
Update `src/main/resources/application.properties` with your MySQL URL, username, and password.

---

## 📌 Notes

- The app targets Java 21 and Spring Boot 3.2
- Build with Maven using `mvn clean package`
- Run with `mvn spring-boot:run`

---

## 📄 License

MIT License — free for academic and commercial use.
