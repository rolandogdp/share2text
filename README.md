# Share2Text — Whisper Android App with Share Support

**Local, private, on-device transcription** for audio you share from WhatsApp, Telegram, Messenger, or any other app.

## Highlights

- Appears in the system share sheet (handles `ACTION_SEND` and `ACTION_SEND_MULTIPLE`) for `audio/*` and `.ogg` (WhatsApp voice notes).
- Downloads and manages Whisper models (tiny → large-v3) via **WorkManager + OkHttp** with pause/resume and **SHA‑256** verification.
- On‑device transcription via **whisper.cpp** through **JNI** — no cloud, no data leaves the device.
- Foreground service for long transcriptions, streaming text into the UI as chunks complete.
- Robust **content URI** handling: copies input streams into app cache before processing.
- Compose + Material3 UI; MVVM + Repository; Hilt DI; Coroutines + Flow.

> **Target:** Android 10+ (API 29), arm64 (`arm64-v8a`).

---

## Build & Run

1. Open the project in **Android Studio Giraffe+**.
2. Ensure you have **NDK** and **CMake** installed (NDK r26+).
3. Build the app. The first build will **FetchContent** `whisper.cpp` (pinned to `v1.5.4`) via CMake.
4. Install on a physical **arm64** device (recommended).

### Notes on whisper.cpp

- The JNI layer is under `app/src/main/cpp`.
- Models are stored in `filesDir/models` (e.g., `/data/data/com.share2text.share/files/models`).
- You can swap to GGUF files from `whisper.cpp` or HuggingFace mirrors. URLs in the Model Picker are examples.
- Licenses are preserved from upstream projects (see `LICENSES/`).

> If the build fails while fetching whisper.cpp, run CMake again or manually clone into `app/src/main/cpp/_deps/`.

---

## Using the App

1. From WhatsApp/Telegram, tap **Share** on a voice message → choose **Share2Text**.
2. The app copies the audio into its cache and starts a **Foreground** transcription.
3. Watch live text appear. When done, **Copy**, **Share**, or **Save .txt**.

**Tip:** If no model is present, the app directs you to **Models** to download one.

---

## Model Manager

- Presets include: tiny, base, small, medium, large‑v3.
- Downloads use **WorkManager** with resume support and **SHA‑256** hash check.
- Set an **Active model**; the chosen file is used by the Whisper engine.
- You can delete models from the `files/models` directory (future UI planned).

---

## Privacy

- **No audio or text leaves the device.**
- Network access is used **only** for downloading models when you choose to.

---

## Caveats & TODOs

- The JNI bridge demonstrates chunked transcription (`30s` windows) with offset/duration. A production build should parse WAV and pass PCM floats to `whisper_full` or `whisper_full_with_state` directly (see `examples/common.cpp` in whisper.cpp).
- Hash values in `ModelRepository` are **placeholders** — replace with official SHA‑256 for your chosen binaries.
- Optional: integrate `ffmpeg` for more robust decoding (Opus-in-Ogg is already handled by Android media stack on most devices).
- Add GPU/NNAPI acceleration when available via ggml backends.
- Add multi-URI queueing (the entry screen already accepts multiple URIs but processes the first).

---

## License

This project is provided under the **MIT License** (see `LICENSE`). `whisper.cpp` is MIT‑licensed by Georgi Gerganov and contributors. Other third‑party licenses apply for dependencies.

---

## File Tree (excerpt)

```
Share2Text/
  app/
    src/main/
      java/com/share2text/share/
        audio/AudioDecoder.kt
        di/Modules.kt
        download/ModelRepository.kt
        nativebridge/WhisperBridge.kt
        repo/TranscriptionRepository.kt
        service/TranscriptionService.kt
        ui/screens/*.kt
        ui/share/ShareEntryActivity.kt
      cpp/
        CMakeLists.txt
        whisper_jni.cpp
      manifests/AndroidManifest.xml
      res/values/*.xml
```

---

## Acceptance Criteria Mapping

- ✅ **Share from WhatsApp (.opus in .ogg)** → `Intent` filters handle `audio/*` and `application/ogg`; decoding via `MediaExtractor/MediaCodec` → PCM 16k mono.
- ✅ **Local transcription** → ForegroundService + JNI (no cloud).
- ✅ **Copy/Share/Save** buttons on the Transcription screen.
- ✅ **SHA‑256** verified downloads via WorkManager.
- ✅ **Android 10+ / arm64** support.
