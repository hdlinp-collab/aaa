# 計算機（整人版）

外觀和一般計算機一樣的 Android App，但**部分計算結果是故意算錯的**。

- 約 35% 的算式會得到錯誤答案（`MainActivity.WRONG_RATE` 可調整）
- 錯得很「像真的」：最後幾位差一點、相鄰數字對調、改掉其中一位數
- 同一題每次都錯成一樣的答案，不會因為重按而露餡
- 答案小於 10 的簡單算式（如 1+1）永遠正確
- **隱藏開關**：長按「=」切換成誠實模式（螢幕右上角出現綠色小點），之後全部正確

## 建置

用 Android Studio 開啟此資料夾，或：

```bash
./gradlew assembleDebug   # 產出 app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest
```

推送到 GitHub 後，GitHub Actions（`.github/workflows/build.yml`）會自動建置，APK 可在該次 Actions 執行的 Artifacts 下載。
