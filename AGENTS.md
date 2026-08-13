# CustomOpenWith

攔截 http/https 連結與分享文字，讓使用者手動挑瀏覽器開啟，並記錄 URL 歷史。

- Release 簽章：CI 從 repo secrets（`RELEASE_KEYSTORE_BASE64` / `RELEASE_KEYSTORE_PASSWORD`）還原固定 keystore，經 `RELEASE_KEYSTORE_PATH` / `RELEASE_KEYSTORE_PASSWORD` 環境變數餵給 Gradle——簽章不一致的 APK 無法覆蓋更新。本機建置沒設環境變數時退回 debug key。
- 版本號由 CI 注入（`-PappVersionCode`/`-PappVersionName`，tag 為 `v<versionName>`），app 內的更新檢查（`UpdateManager`）靠這個 tag 格式與 GitHub Releases API 比對——改版本方案時 workflow、build script、`UpdateManager` 要一起改。
- 更新檢查只由使用者按按鈕觸發，不要加自動檢查。
- repo 內不准放任何 keystore、API key 或個人資訊。
- Commit 訊息用 conventional commits（`feat:`、`fix:`、`chore:`…）。
- `README.md` 與 `CLAUDE.md` 都是指向本檔的 symlink（GitHub 預覽用、Claude Code 只讀 `CLAUDE.md`）。
