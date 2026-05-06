#OMR Software

A custom OMR (Optical Mark Recognition) template designer and scanner for exam sheets.

## Features

- **Template Designer** — Upload a PDF of your OMR sheet, visually mark QR code zones, MCQ bubble groups, and written-answer section regions
- **Scanner Integration** — Connect to TWAIN-compatible scanners (currently in test mode, real TWAIN integration ready to plug in)
- **Batch Processing** — Process hundreds of sheets automatically using your saved template
- **QR Code Detection** — Auto-read student codes from QR/barcodes using ZXing
- **Bubble Detection** — Detect filled MCQ answers using OpenCV-backed image processing
- **Organized Output** — Images saved per template code and student code, ready for a teacher portal
- **CSV Export** — Export all MCQ results to CSV for import into your grading system

## Project Structure

```
omr-software/
├── pom.xml                                    Maven build configuration
├── README.md                                  This file
├── ARCHITECTURE.md                            Detailed architecture notes
├── docs/                                      Additional documentation
├── lib/                                       External JARs (morena7.jar for TWAIN)
├── src/main/java/com/bise/omr/
│   ├── MainApplication.java                   Entry point
│   ├── ui/
│   │   ├── MainWindow.java                    Home screen
│   │   ├── TemplateDesignerView.java          Screen 1: Create template
│   │   └── ScanView.java                      Screen 2: Scan & process
│   ├── template/
│   │   ├── Template.java                      Main template model
│   │   ├── TemplateField.java                 Abstract field base class
│   │   ├── QRCodeField.java                   QR / barcode field
│   │   ├── BubbleGroupField.java              MCQ bubble grid field
│   │   ├── SectionMarkerField.java            Written answer region
│   │   └── TemplateRepository.java            JSON save/load
│   ├── scanner/
│   │   ├── PdfImporter.java                   PDF → image for designer
│   │   └── ScannerController.java             TWAIN scanner interface
│   ├── processor/
│   │   ├── OmrProcessor.java                  Main processing engine
│   │   ├── ImagePreprocessor.java             Deskew, threshold
│   │   ├── BubbleDetector.java                Detect filled bubbles
│   │   ├── QRCodeReader.java                  Read QR codes (ZXing)
│   │   └── OmrResult.java                     Result model
│   └── storage/
│       ├── FileOrganizer.java                 Organize output images
│       └── CsvExporter.java                   Export results to CSV
├── src/main/resources/
│   ├── logback.xml                            Logging configuration
│   └── styles/app.css                         JavaFX styling
└── src/test/java/                             Unit tests
```

## Prerequisites

- **Java 17 or higher** — [Download from Adoptium](https://adoptium.net/)
- **Maven 3.8+** — [Download from Apache](https://maven.apache.org/download.cgi)
- **VS Code** with the Java Extension Pack (or IntelliJ IDEA)

## Getting Started

### 1. Clone or extract the project

```bash
cd omr-software
```

### 2. Install dependencies

```bash
mvn clean install
```

This downloads JavaFX, OpenCV (via JavaCV), PDFBox, ZXing, Jackson, OpenCSV, and all other dependencies.

### 3. Run the application

```bash
mvn javafx:run
```

Or from your IDE, run `com.bise.omr.MainApplication`.

### 4. Test mode (no scanner needed)

If you don't have a scanner connected yet, create a folder `test-images/` in the project root and place some JPG/PNG scans of your sheet inside it. The `ScannerController` will read them sequentially, simulating real scanner behavior.

## Development Roadmap

This skeleton has working code for the UI, template creation, saving/loading, and PDF import. The following areas have `TODO` markers where you need to complete implementation:

### Priority 1 — Core functionality

- `ImagePreprocessor.java` — integrate OpenCV deskew, threshold, and corner detection
- `BubbleDetector.java` — improve accuracy with OpenCV's adaptive thresholding and contour detection
- `ScannerController.java` — integrate real TWAIN library (Morena, JTwain, or similar)

### Priority 2 — UX improvements

- Visual field editing on the designer canvas (move, resize, delete)
- Field configuration dialog when drawing (e.g., set number of questions for bubble groups)
- Live preview of detected fields during scan

### Priority 3 — Portal integration

- REST client to push results to your teacher portal
- Cropped section region uploads for teacher manual review
- Answer key upload and auto-scoring

## Using Claude Code in VS Code

Open this folder in VS Code with Claude Code. Use prompts like:

- _"Implement the deskew method in ImagePreprocessor.java using JavaCV OpenCV bindings"_
- _"Add a dialog to TemplateDesignerView that asks for question count when user draws a bubble group"_
- _"Improve BubbleDetector.calculateFillRatio to use OpenCV thresholding for better accuracy"_
- _"Add ability to delete fields by right-clicking on the canvas in TemplateDesignerView"_

Each class has clear JavaDoc describing its purpose, so Claude can easily understand context.

## Technology Stack

| Layer            | Technology                   |
| ---------------- | ---------------------------- |
| UI               | JavaFX 21                    |
| Image processing | OpenCV via JavaCV (bytedeco) |
| PDF handling     | Apache PDFBox 3              |
| QR codes         | ZXing 3.5                    |
| JSON             | Jackson                      |
| CSV              | OpenCSV                      |
| Logging          | SLF4J + Logback              |
| Testing          | JUnit 5                      |
| Build            | Maven                        |

## License

Open source OMR software. Adapt as needed.

## Support

For questions or issues, refer to `ARCHITECTURE.md` or the JavaDoc comments inside each file.
