# Architecture

## Overview

The application is a JavaFX desktop app with a layered architecture:

```
┌─────────────────────────────────────────┐
│              UI Layer (JavaFX)          │  ← User interactions
├─────────────────────────────────────────┤
│  Template   │   Scanner   │  Processor  │  ← Business logic
├─────────────────────────────────────────┤
│             Storage Layer               │  ← Persistence
└─────────────────────────────────────────┘
```

## Module Responsibilities

### `com.bise.omr.ui`

**Purpose:** JavaFX views and user interaction handling.

- `MainApplication` — JavaFX `Application` subclass, entry point
- `MainWindow` — Navigation hub between the two main screens
- `TemplateDesignerView` — Canvas-based drag-to-mark template editor
- `ScanView` — Batch scan controls and results list

### `com.bise.omr.template`

**Purpose:** Template model and persistence.

The template is a JSON document describing:

- A unique `templateCode` (identifier used in output folders)
- Page dimensions (matched against scanned image size)
- A list of `TemplateField` subclasses (QR / Bubble / Section)

Each field has x/y/width/height coordinates defining its region on the page.

### `com.bise.omr.scanner`

**Purpose:** Input sources for images.

- `PdfImporter` — Renders PDF pages to `BufferedImage` for the designer canvas
- `ScannerController` — TWAIN scanner interface (currently test-mode with folder-based input)

### `com.bise.omr.processor`

**Purpose:** OMR extraction pipeline.

Processing flow for each scanned sheet:

1. `ImagePreprocessor.deskewAndNormalize()` — straighten, threshold, align
2. Iterate through each `TemplateField` in the template
3. Call `field.extract(img)` — polymorphic dispatch based on field type:
   - `QRCodeField` → `QRCodeReader.read()` → returns student code string
   - `BubbleGroupField` → `BubbleDetector.detectFilled()` → returns answers list
   - `SectionMarkerField` → returns cropped sub-image
4. Collect everything into an `OmrResult`

### `com.bise.omr.storage`

**Purpose:** Output organization.

- `FileOrganizer` — creates `output/{templateCode}/{studentCode}/pageN.jpg`
- `CsvExporter` — writes final results to a CSV spreadsheet

## Output Folder Structure

After processing a batch of sheets, you get:

```
output/
└── OMR-EXAM-2026/                     ← templateCode
    ├── 1291252/                       ← studentCode (from QR)
    │   ├── page1.jpg
    │   ├── page2.jpg
    │   └── ...
    ├── 1291253/
    │   └── ...
    └── UNKNOWN_1734543201/            ← if QR unreadable
        └── page1.jpg

templates/
└── OMR-EXAM-2026.json                  ← saved template
```

## Design Decisions

### Why JavaFX over Swing?

- Better canvas drawing performance (important for the designer)
- Modern CSS-based styling
- Built-in FXML for potential future UI updates
- Works on Windows, macOS, Linux with same code

### Why JavaCV over pure OpenCV?

- No separate native library installation
- Published to Maven Central, easy dependency management
- Comes with Java-friendly wrappers

### Why store templates as JSON?

- Human-readable — can be hand-edited if needed
- Version control friendly
- Easy to debug with any text editor
- Jackson handles serialization cleanly

### Why the abstract `TemplateField` hierarchy?

- Polymorphism: each field type knows how to extract its own data
- Easy to add new field types later (e.g., `SignatureField`, `TextField` for OCR)
- Jackson `@JsonSubTypes` handles serialization automatically

## Future Extensions

### Scanner Integration Options

**Option A: Morena (recommended)**

- Commercial library with free trial
- Most mature Java TWAIN binding
- Download: http://www.gnome.sk/Twain/jtp.html
- Place `morena7.jar` in `lib/` folder

**Option B: SaneScan (Linux)**

- Open source
- Works on Linux systems
- Less tested on Windows

**Option C: Shell out to OS scan command**

- Windows: use PowerShell `WIA` API
- macOS: use `sips` or `image capture`
- Linux: use `scanimage` from SANE project
- Simplest but least flexible

### Performance Optimization

For large batches (1000+ sheets):

- Process sheets in parallel using `ExecutorService`
- Load templates once, reuse for whole batch
- Batch CSV writes at the end rather than per-sheet
- Consider progress indicator with cancellation

### Multi-page Sheets

Exam sheets can have multiple pages. Extend the design by:

- Adding `pageCount` to `Template`
- Grouping fields by `pageNumber`
- Scanning N pages per student, saving all to `output/{template}/{student}/pageN.jpg`
- Matching pages to student via QR code on page 1
