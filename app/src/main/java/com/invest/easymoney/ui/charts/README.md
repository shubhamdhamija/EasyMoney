MPAndroidChart integration for EasyMoney

This folder contains the MPAndroidChart wrapper used in the stock detail screen.

Files
- `MPLineChart.kt` — Compose `AndroidView` wrapper around `LineChart`.
- `ChartMapper.kt` — small helper for converting epoch times to x-axis labels.
- `ChartMapperTest.kt` — unit tests for the mapper.

How it works
- `MPLineChart` accepts `prices: List<Float>` and optional `times: List<Long>`.
- The wrapper creates a `LineChart`, maps price values to `Entry(index, price)`, and uses `IndexAxisValueFormatter` to show time labels when provided.

Testing
Run unit tests with:

```bash
./gradlew test
```

Notes
- MPAndroidChart is included as a dependency in `app/build.gradle.kts` (version 3.1.0).
- This wrapper is intentionally minimal — for production you may want to:
  - Provide better X-axis formatting, dynamic granularity, and zoom/pan controls.
  - Provide accessibility labels and color theming.
  - Cache chart data to avoid excessive re-binding on recomposition.

