# Spring ETL Validator

<p>Welcome to the Spring ETL Validator. 
This project is a Spring Boot–based data analysis
REST API that ingests CSV files and automatically profiles their structure and content. 
The service extracts metadata such as column types, null counts, and statistical summaries,
then persists analysis results for later retrieval.
The system is designed to support real-world data engineering workflows, 
including dataset exploration, data quality assessment, and ETL validation before 
data is loaded into analytics platforms. It exposes clean REST endpoints for uploading data, 
retrieving analysis results, and managing stored analyses.
The focus is on production-style backend design using modern Java, clear API 
contracts, test-driven development, and extensibility for future features such 
as additional metrics, storage backends, or file formats.</p>

## Overview

This is a data analysis service built with:
- **Java 17**
- **Spring Boot 3** (Web, JPA, Actuator)
- **Gradle** for dependency management
- **H2 Database** for lightweight in-memory persistence
- **Lombok** for reducing boilerplate code
- **JUnit 5** for testing

The service provides REST API endpoints for ingesting and analyzing data, with results persisted to an H2 database.

### Real-World Use Cases

This type of service is commonly used in data engineering and analytics platforms where users need to:
- **Explore unknown datasets** - Quickly understand the structure, data types, and basic statistics of CSV files without manual inspection
- **Data quality assessment** - Automatically detect data types, identify null values, and calculate statistical summaries to assess data completeness
- **ETL pipeline validation** - Verify data format and content before loading into data warehouses or lakes
- **Self-service analytics** - Enable business users to upload and analyze their own datasets through a simple API
- **Data profiling** - Generate metadata and summary statistics for data catalogs and governance tools

In production environments, similar services often integrate with cloud storage (S3, Azure Blob), handle larger file formats (Parquet, Avro), and scale horizontally to process multiple files concurrently.

## Prerequisites

- Java 17 (JDK 17)
- A Java-compatible IDE, such as IntelliJ IDEA

## Getting Started

### Build the Project
```bash
./gradlew build
```

### Run the Application
```bash
./gradlew bootRun
```
The service will start on `http://localhost:8080`

## Rerun spring when need to
```bash
./gradlew clean bootRun
```
## Stop local host before rerun 

### Run Tests
```bash
./gradlew test
```
The test result will be printed on the terminal.
Alternatively, the result can be viewed on the browser: file:///C:/Projects/spring-etl-validator/build/reports/tests/test/index.html
Note: file:///C:/Projects/  (will depend on the path you decide to download the project)


### Test the API Manually

Once the application is running, you can interact with the API using Swagger UI:

**Open in your browser:** `http://localhost:8080/swagger-ui/index.html`

This provides an interactive interface to test API endpoints without needing additional tools like Postman or curl.


## API Endpoints

### Data Analysis
- `POST /api/analysis/ingestCsv` - Ingest and analyze CSV data
- `GET /api/analysis/{id}` - Retrieve a previously analyzed CSV by ID 
- `DELETE /api/analysis/{id}` - Delete an analysis by ID

## Example test case from Windows terminal 
Invoke-WebRequest `
  -Uri http://localhost:8080/api/analysis/ingestCsv `
-Method POST `
  -ContentType "text/plain" `
-Body "driver,number,team"

This should in response return this:
Content:
{
"id": 1,
"numberOfRows": 0,
"numberOfColumns": 3,
"totalCharacters": 18,
"columnStatistics": [
{
"columnName": "driver",
"nullCount": 0,
"uniqueCount": 0
},
{
"columnName": "number",
"nullCount": 0,
"uniqueCount": 0
},
{
"columnName": "team",
"nullCount": 0,
"uniqueCount": 0
}
],
"createdAt": "2025-12-26T18:23:14.6431307Z"
}