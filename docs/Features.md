# Implemented features: 
1. Null and empty value detection via blank checks and nullCounts
2. Row shape validation by enforcing equal column count per row
3. Header presence validation by rejecting blank first line
4. Basic CSV structure validation by rejecting malformed rows
5. Per-column uniqueness tracking using Set
6. Numeric type detection using parse attempt per value
7. Min value calculation for numeric columns
8. Max value calculation for numeric columns
9. Mean calculation for numeric columns
10. Median calculation for numeric columns
11. Standard deviation calculation for numeric columns
12. Percentile calculation for numeric columns
13. Row counting with blank-line skipping
14. Total character count calculation
15. Content hashing for duplicate dataset detection
16. Cached analysis reuse via hash lookup
17. Persistent storage of analysis metadata
18. Persistent storage of per-column statistics
19. Max file limit 5mb/1,000,000 imposed 
---

# Some important ideas
- Export functionality (JSON, Excel)
- Data filtering or transformation capabilities
- Batch processing of multiple CSVs
- Historical tracking and comparison
- Data visualization endpoints
- Column correlation analysis
- Missing data handling strategies

## Feature Scope
- Data ingestion and parsing
- Data validation and quality checks
- Schema inference and column type detection
- Schema stability and fingerprinting
- Column-level and dataset-level quality metrics
- Null, empty, and completeness analysis
- Uniqueness and cardinality analysis
- Statistical profiling (min, max, mean, median, std, percentiles)
- Categorical value profiling (top-K, dominance, long tail)
- Text column profiling (length, tokens, character patterns)
- Missing data pattern detection
- Missing data handling and imputation strategies
- Column correlation and dependency analysis
- Redundant and low-information column detection
- Anomaly and outlier detection
- Row-level and column-level validity labeling
- Dataset quality scoring and risk classification
- Rule-based issue detection and tagging
- Feature extraction and vectorization for ML
- Dataset fingerprinting and similarity detection
- Historical tracking and dataset versioning
- Dataset comparison and change analysis
- Data drift detection and monitoring
- Batch processing of multiple CSVs
- Consistent feature alignment across batches
- Model-ready data export (JSON, Parquet, etc.)
- Audit trail and lineage tracking
- Remediation recommendation generation
- Confidence scoring for predictions
