# 🪙 Gold Price India — Android App

A beautiful Android app showing live 24K, 22K & 18K gold prices in India sourced from **GoodReturns.in**.

## ✨ Features

- **Live gold prices** — scraped directly from GoodReturns.in (OkHttp + Jsoup)
- **24K, 22K, 18K** prices with purity info
- **GST toggle** — see prices with or without 3% GST, with full breakdown
- **Unit switcher** — Per Gram / Per 10g / Per Tola (11.664g)
- **14 cities** — Mumbai, Delhi, Bangalore, Pune, Chennai, Hyderabad, Kolkata, and more
- **Summary Table** — side-by-side view of all karats and units
- **Dark luxury gold theme** — Jetpack Compose with Material3

## 🏗 Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Networking | OkHttp 4.x |
| HTML Parsing | Jsoup 1.18 |
| Async | Kotlin Coroutines |
| Language | Kotlin |

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11+
- Android device/emulator API 26+

### Steps

1. **Clone / open the project**
   ```
   Open Android Studio → File → Open → select the `GoldPriceIndia` folder
   ```

2. **Sync Gradle**
   - Android Studio will auto-sync. If not: `File → Sync Project with Gradle Files`

3. **Run the app**
   - Connect a device or start an emulator
   - Click ▶ Run

> ⚠️ The app requires **internet permission** (already declared in AndroidManifest.xml) to fetch live prices.

## 📁 Project Structure

```
app/src/main/java/com/goldprice/india/
├── MainActivity.kt               ← Entry point
├── GoldViewModel.kt              ← UI state + business logic
├── data/
│   └── GoldRepository.kt         ← OkHttp scraping + Jsoup parsing
└── ui/
    ├── GoldPriceScreen.kt         ← All Compose UI composables
    └── theme/
        ├── Color.kt               ← Gold/dark palette
        └── Theme.kt               ← Material3 theme
```

## 🔧 Customization

- **Add more cities**: Edit `CITIES` list in `GoldRepository.kt`
- **Change GST rate**: Update `GST_RATE` in `GoldViewModel.kt`
- **Refresh interval**: Add a Timer in `GoldViewModel.init {}` to auto-refresh every N minutes

## 📝 Notes

- Prices are scraped from `goodreturns.in/gold-rates/` and city-specific pages
- The scraper uses two strategies: HTML table parsing + regex fallback
- Prices are indicative; contact your local jeweller for exact rates
- 1 Tola = 11.664 grams (standard)
