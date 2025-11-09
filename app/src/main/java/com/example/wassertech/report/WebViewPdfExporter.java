package com.example.wassertech.report;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.util.Log;
import android.webkit.WebView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Headless exporter: renders a WebView content into a PDF file without showing the system print dialog.
 *
 * NEW APPROACH (2025):
 * - Fixed layout width: 794px (CSS pixels) for A4 printing
 * - All calculations in CSS pixels
 * - Measure content height via JavaScript from #print-root
 * - Page height: 1123px (CSS) for A4 at 794px width
 * - Slice bitmap by CSS page boundaries converted to device pixels
 * - Prevent component splitting across pages
 */
public final class WebViewPdfExporter {

    private static final String TAG = "WebViewPdfExporter";
    
    // Fixed A4 dimensions in CSS pixels (at 96 DPI)
    private static final int A4_WIDTH_CSS = 794;   // 210mm = 794px at 96 DPI
    private static final int A4_HEIGHT_CSS = 1123; // 297mm = 1123px at 96 DPI
    

    public interface Callback {
        void onSuccess();
        void onError(Throwable t);
    }

    private WebViewPdfExporter() { /* no instances */ }

    /**
     * Export the provided WebView to PDF file synchronously from the calling thread (but will schedule UI work).
     * This method posts required drawing to the main thread; callback will be invoked on the main thread.
     *
     * IMPORTANT: webView must have finished loading and layout (onPageFinished). Best call shortly after that.
     */
    public static void export(final WebView webView,
                              final PrintAttributes attrs,
                              final File outFile,
                              final Callback cb) {
        export(webView, attrs, outFile, null, cb);
    }

    /**
     * Export with page footer information.
     */
    public static void export(final WebView webView,
                              final PrintAttributes attrs,
                              final File outFile,
                              final String reportNumber,
                              final Callback cb) {
        Log.d(TAG, "export() called, isMainThread: " + (Looper.myLooper() == Looper.getMainLooper()));
        
        // Ensure we run the WebView drawing on the UI thread.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread, execute directly
            Log.d(TAG, "Already on main thread, executing directly");
            try {
                performExport(webView, attrs, outFile, reportNumber);
                Log.d(TAG, "performExport() completed successfully");
                cb.onSuccess();
            } catch (Throwable t) {
                Log.e(TAG, "PDF export failed", t);
                cb.onError(t);
            }
        } else {
            // Post to main thread
            Log.d(TAG, "Posting to main thread");
            Handler main = new Handler(Looper.getMainLooper());
            main.post(() -> {
                try {
                    Log.d(TAG, "Executing performExport() on main thread");
                    performExport(webView, attrs, outFile, reportNumber);
                    Log.d(TAG, "performExport() completed successfully");
                    cb.onSuccess();
                } catch (Throwable t) {
                    Log.e(TAG, "PDF export failed", t);
                    cb.onError(t);
                }
            });
        }
    }

    private static void performExport(WebView webView, PrintAttributes attrs, File outFile, String reportNumber) throws IOException {
        // Save HTML file next to PDF
        try {
            File htmlFile = new File(outFile.getParent(), outFile.getName().replace(".pdf", ".html"));
            // We need to get HTML from WebView - but we don't have it here
            // HTML will be saved in PdfExporter.kt before calling this
            Log.d(TAG, "HTML file would be saved to: " + htmlFile.getAbsolutePath());
        } catch (Exception e) {
            Log.w(TAG, "Could not determine HTML file path", e);
        }
        Log.d(TAG, "performExport() started");
        if (webView == null) throw new IllegalArgumentException("webView == null");
        if (attrs == null) throw new IllegalArgumentException("attrs == null");
        if (outFile == null) throw new IllegalArgumentException("outFile == null");

        final int densityDpi = webView.getResources().getDisplayMetrics().densityDpi;
        final float density = webView.getResources().getDisplayMetrics().density;

        // Fixed CSS width for A4 printing: 794px
        final int cssWidth = A4_WIDTH_CSS;
        final int cssPageHeight = A4_HEIGHT_CSS;
        
        // Convert CSS width to device pixels for WebView layout
        final int webViewWidthDevicePx = Math.round(cssWidth * density);
        
        Log.d(TAG, "=== PDF EXPORT CALCULATIONS (NEW APPROACH) ===");
        Log.d(TAG, "Fixed layout:");
        Log.d(TAG, "  CSS width: " + cssWidth + " CSS px");
        Log.d(TAG, "  CSS page height: " + cssPageHeight + " CSS px");
        Log.d(TAG, "  WebView width (device): " + webViewWidthDevicePx + " device px (=" + cssWidth + " * " + density + ")");
        Log.d(TAG, "");
        Log.d(TAG, "Device metrics:");
        Log.d(TAG, "  density: " + density);
        Log.d(TAG, "  densityDpi: " + densityDpi);
        Log.d(TAG, "");

        // Ensure WebView is laid out with fixed width
        webView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(webViewWidthDevicePx, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        );
        webView.layout(0, 0, webViewWidthDevicePx, webView.getMeasuredHeight());
        
        // Measure content height and boundaries via JavaScript
        PdfContentMeasurer.MeasurementResult measurement = PdfContentMeasurer.measureContent(webView);
        int contentHeightCss = measurement.contentHeightCss;
        List<PdfBoundaryModels.ComponentBoundary> componentBoundaries = measurement.componentBoundaries;
        List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries = measurement.sectionHeaderBoundaries;
        
        if (measurement.hasError) {
            Log.w(TAG, "Measurement had errors: " + measurement.errorMessage);
        }
        
        if (contentHeightCss <= 0) {
            throw new IOException("Content height is 0 or negative: " + contentHeightCss);
        }
        
        // Log all boundaries for debugging
        Log.d(TAG, "");
        Log.d(TAG, "=== SECTION HEADER BOUNDARIES ===");
        for (int i = 0; i < sectionHeaderBoundaries.size(); i++) {
            PdfBoundaryModels.SectionHeaderBoundary header = sectionHeaderBoundaries.get(i);
            Log.d(TAG, "  SectionHeader[" + i + "]: " + header.toString());
        }
        Log.d(TAG, "");
        Log.d(TAG, "=== COMPONENT BOUNDARIES ===");
        for (int i = 0; i < componentBoundaries.size(); i++) {
            PdfBoundaryModels.ComponentBoundary comp = componentBoundaries.get(i);
            Log.d(TAG, "  Component[" + i + "]: " + comp.toString());
        }
        Log.d(TAG, "");
        
        // Calculate page boundaries respecting component and section header boundaries
        List<Float> pageBoundariesCss = PdfPageBoundaryCalculator.calculatePageBoundaries(
                contentHeightCss, cssPageHeight, componentBoundaries, sectionHeaderBoundaries);
        int pages = pageBoundariesCss.size() - 1;
        
        Log.d(TAG, "");
        Log.d(TAG, "=== PAGE BOUNDARIES CALCULATION ===");
        Log.d(TAG, "  contentHeightCss: " + contentHeightCss + " CSS px");
        Log.d(TAG, "  cssPageHeight: " + cssPageHeight + " CSS px");
        Log.d(TAG, "  componentBoundaries: " + componentBoundaries.size());
        Log.d(TAG, "  calculated pages: " + pages);
        for (int i = 0; i < pageBoundariesCss.size(); i++) {
            Log.d(TAG, "  Page boundary[" + i + "]: " + pageBoundariesCss.get(i) + " CSS px");
        }
        Log.d(TAG, "");
        
        // Calculate page dimensions in device pixels for PDF
        final PrintAttributes.MediaSize mediaSize = attrs.getMediaSize();
        int pageWidthMils = (mediaSize != null) ? mediaSize.getWidthMils() : 827; // A4 ~ 827 mils (210mm)
        int pageHeightMils = (mediaSize != null) ? mediaSize.getHeightMils() : 1169; // A4 ~ 1169 mils (297mm)
        int pageWidthDevicePx = Math.max(1, pageWidthMils * densityDpi / 1000);
        int pageHeightDevicePx = Math.max(1, pageHeightMils * densityDpi / 1000);
        
        Log.d(TAG, "PDF page dimensions (device px):");
        Log.d(TAG, "  width: " + pageWidthDevicePx + " device px");
        Log.d(TAG, "  height: " + pageHeightDevicePx + " device px");
        Log.d(TAG, "");
        
        // Render WebView to Bitmap
        Log.d(TAG, "Rendering WebView to Bitmap...");
        int contentHeightDevicePx = Math.round(contentHeightCss * density);
        
        Bitmap webViewBitmap = null;
        try {
            webViewBitmap = Bitmap.createBitmap(webViewWidthDevicePx, contentHeightDevicePx, Bitmap.Config.ARGB_8888);
            Canvas bitmapCanvas = new Canvas(webViewBitmap);
            webView.draw(bitmapCanvas);
            Log.d(TAG, "WebView rendered to Bitmap: " + webViewWidthDevicePx + "x" + contentHeightDevicePx + " device px");
            Log.d(TAG, "  (CSS equivalent: " + cssWidth + "x" + contentHeightCss + " CSS px)");
            
            // Draw debug lines: component boundaries (green), section headers (blue), and page breaks (purple)
            PdfDebugDrawer.drawDebugLines(bitmapCanvas, webViewWidthDevicePx, contentHeightDevicePx, density, 
                          componentBoundaries, sectionHeaderBoundaries, pageBoundariesCss);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory rendering WebView to Bitmap", e);
            throw new IOException("Out of memory rendering WebView", e);
        }
        
        PdfDocument document = new PdfDocument();

        try {
            // Track previous page end to ensure no overlap
            int prevPageEndDevicePx = 0;
            
            for (int i = 0; i < pages; i++) {
                // Check bounds
                if (i >= pageBoundariesCss.size() - 1) {
                    Log.w(TAG, "Page " + (i + 1) + " exceeds boundaries list, stopping");
                    break;
                }
                
                float pageStartCss = pageBoundariesCss.get(i);
                float pageEndCss = pageBoundariesCss.get(i + 1);
                
                // Skip empty or invalid pages
                if (pageStartCss >= contentHeightCss || pageEndCss <= pageStartCss) {
                    Log.d(TAG, "Skipping page " + (i + 1) + " - invalid boundaries: start=" + pageStartCss + ", end=" + pageEndCss);
                    break;
                }
                
                Log.d(TAG, "");
                Log.d(TAG, "=== PAGE " + (i + 1) + "/" + pages + " ===");
                Log.d(TAG, "CSS pixel calculations:");
                Log.d(TAG, "  pageStartCss: " + pageStartCss + " CSS px");
                Log.d(TAG, "  pageEndCss: " + pageEndCss + " CSS px");
                Log.d(TAG, "  pageHeightCss: " + (pageEndCss - pageStartCss) + " CSS px");
                Log.d(TAG, "  contentHeightCss: " + contentHeightCss + " CSS px");
                
                // Convert CSS page boundaries to device pixels for bitmap slicing
                // Use consistent rounding to avoid overlaps
                // For start: round down to ensure we don't skip content
                // For end: round up to ensure we don't miss content, but clamp to avoid overlap with next page
                int yStartDevicePx = (int) Math.floor(pageStartCss * density);
                int yEndDevicePx = (int) Math.ceil(pageEndCss * density);
                
                // Ensure we don't go below previous page end (prevent overlap)
                if (i > 0 && yStartDevicePx < prevPageEndDevicePx) {
                    Log.w(TAG, "Page " + (i + 1) + " start " + yStartDevicePx + " is before previous page end " + prevPageEndDevicePx + ", adjusting");
                    yStartDevicePx = prevPageEndDevicePx;
                }
                
                // Clamp to bitmap bounds
                yStartDevicePx = Math.max(0, Math.min(yStartDevicePx, contentHeightDevicePx - 1));
                yEndDevicePx = Math.max(yStartDevicePx + 1, Math.min(yEndDevicePx, contentHeightDevicePx));
                
                int actualSliceHeight = yEndDevicePx - yStartDevicePx;
                
                if (actualSliceHeight <= 0) {
                    Log.w(TAG, "Page " + (i + 1) + " has zero or negative height, stopping");
                    break;
                }
                
                // Store for next iteration
                prevPageEndDevicePx = yEndDevicePx;
                
                Log.d(TAG, "Device pixel calculations (for bitmap slicing):");
                Log.d(TAG, "  density: " + density);
                Log.d(TAG, "  yStartDevicePx: " + yStartDevicePx + " device px (from " + pageStartCss + " CSS px)");
                Log.d(TAG, "  yEndDevicePx: " + yEndDevicePx + " device px (from " + pageEndCss + " CSS px)");
                Log.d(TAG, "  actualSliceHeight: " + actualSliceHeight + " device px");
                Log.d(TAG, "  bitmap total height: " + contentHeightDevicePx + " device px");
                Log.d(TAG, "");
                
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidthDevicePx, pageHeightDevicePx, i + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Extract bitmap slice and draw it to the page
                // Source rectangle in bitmap (device pixels) - exact slice we need
                int srcLeft = 0;
                int srcTop = yStartDevicePx;
                int srcRight = webViewWidthDevicePx;
                int srcBottom = yEndDevicePx;
                int srcWidth = srcRight - srcLeft;
                int srcHeight = srcBottom - srcTop;
                
                if (srcHeight <= 0 || srcWidth <= 0) {
                    Log.e(TAG, "Invalid source rectangle: " + srcLeft + "," + srcTop + " to " + srcRight + "," + srcBottom);
                    document.finishPage(page);
                    continue;
                }
                
                // Calculate scale to fit bitmap width to page width
                float scale = (float) pageWidthDevicePx / (float) webViewWidthDevicePx;
                
                // Destination rectangle on PDF page (device pixels)
                // Width: fill entire page width (no margins)
                // Height: scale proportionally to maintain aspect ratio
                float dstLeft = 0;
                float dstTop = 0;
                float dstRight = pageWidthDevicePx;
                float dstBottom = srcHeight * scale; // Proportional height
                
                // Don't exceed page height
                if (dstBottom > pageHeightDevicePx) {
                    Log.w(TAG, "Scaled height " + dstBottom + " exceeds page height " + pageHeightDevicePx + ", clipping");
                    dstBottom = pageHeightDevicePx;
                }
                
                Log.d(TAG, "Drawing bitmap slice:");
                Log.d(TAG, "  Source: (" + srcLeft + ", " + srcTop + ") to (" + srcRight + ", " + srcBottom + ")");
                Log.d(TAG, "  Source size: " + srcWidth + "x" + srcHeight + " device px");
                Log.d(TAG, "  Scale: " + scale + " (uniform, maintains aspect ratio)");
                Log.d(TAG, "  Destination: (" + dstLeft + ", " + dstTop + ") to (" + dstRight + ", " + dstBottom + ")");
                Log.d(TAG, "  Destination size: " + (dstRight - dstLeft) + "x" + (dstBottom - dstTop) + " device px");
                Log.d(TAG, "");
                
                // Draw bitmap slice to page
                // drawBitmap will scale from srcRect to dstRect proportionally
                android.graphics.Rect srcRect = new android.graphics.Rect(srcLeft, srcTop, srcRight, srcBottom);
                android.graphics.RectF dstRect = new android.graphics.RectF(dstLeft, dstTop, dstRight, dstBottom);
                
                canvas.drawBitmap(webViewBitmap, srcRect, dstRect, null);
                
                // Draw page footer
                if (reportNumber != null && !reportNumber.isEmpty()) {
                    String footerText = reportNumber + " - Страница " + (i + 1) + " из " + pages;
                    
                    android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                    paint.setColor(0xFF6b7280);
                    paint.setTextSize(28);
                    paint.setTextAlign(android.graphics.Paint.Align.RIGHT);
                    paint.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
                    
                    // Footer position: bottom right, small margin (5mm)
                    float footerX = pageWidthDevicePx - (5f * densityDpi / 25.4f);
                    float footerY = pageHeightDevicePx - (2f * densityDpi / 25.4f);
                    
                    canvas.drawText(footerText, footerX, footerY, paint);
                }
                
                document.finishPage(page);
                Log.d(TAG, "Page " + (i + 1) + " completed");
            }

            // Write PDF to file
            outFile.getParentFile().mkdirs();
            Log.d(TAG, "Writing PDF to file: " + outFile.getAbsolutePath());
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                document.writeTo(fos);
                Log.d(TAG, "PDF written successfully, file size: " + outFile.length() + " bytes");
            }
        } finally {
            document.close();
            Log.d(TAG, "PDF document closed");
            // Recycle bitmap to free memory
            if (webViewBitmap != null && !webViewBitmap.isRecycled()) {
                webViewBitmap.recycle();
                Log.d(TAG, "WebView bitmap recycled");
            }
        }
    }
    
}
