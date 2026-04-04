# Export Instructions for ManoMitra Architecture Diagram

To ensure **IEEE Conference Paper Quality (300+ DPI)**, follow these steps to export the provided HTML/SVG architecture diagrams.

## Method 1: Browser-to-PDF (Highest Vector Quality)
This method preserves text as vectors, ensuring no pixelation at any zoom level.

1. Open `system_architecture_diagram.html` (Full version) or `system_architecture_ieee.html` (2-column version) in Google Chrome or Edge.
2. Press `Ctrl + P` (Print).
3. Set **Destination** to `Save as PDF`.
4. Under **More Settings**:
   - **Paper size**: A4 or Letter.
   - **Margins**: None.
   - **Background graphics**: **CHECKED** (Critical for colors).
5. Click **Save**.
6. Use Adobe Acrobat or an online converter to convert the PDF to a high-res TIFF/PNG if required by the publisher.

## Method 2: High-Resolution Screenshot (Fixed DPI)
If you require a specific PNG/JPEG format at 300 DPI:

1. Open the file in the browser.
2. Open Developer Tools (`F12`).
3. Press `Ctrl + Shift + P`.
4. Type `Capture full size screenshot` and press Enter.
5. Before capturing, you can zoom in (e.g., 200% or 300%) to increase the effective resolution of the output.

## Metadata for IEEE Caption
**Title:** Fig. 1. Overall System Architecture of the ManoMitra Multi-Role Mental Health Platform.
**Description:** The architecture is modularized into five layers: (1) Presentation Layer for role-specific interfaces, (2) Application Logic for state management, (3) AI Intelligence Layer for safety filtering and LLM generation, (4) Backend Cloud Layer using Firebase for secure persistence, and (5) Data Analytics for aggregated trend monitoring.

---
*Generated for the ManoMitra Project Evaluators.*
