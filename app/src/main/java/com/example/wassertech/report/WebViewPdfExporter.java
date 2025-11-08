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

/**
 * Headless exporter: renders a WebView content into a PDF file without showing the system print dialog.
 *
 * Notes / caveats:
 * - This implementation renders the WebView by drawing it to a PdfDocument Canvas.
 * - We compute page size in device pixels from PrintAttributes (media size in mils -> inches -> px using densityDpi).
 * - WebView contentHeight is in CSS pixels; we convert roughly to device pixels using density.
 * - Long pages are split into multiple PDF pages.
 * - For best results ensure WebView content is fully loaded and laid out before calling export().
 *   Callers should wait for onPageFinished or otherwise ensure content is ready.
 *
 * Usage:
 *   // ensure call happens on main thread because WebView drawing must be on UI thread
 *   WebViewPdfExporter.export(webView, attrs, outFile, new WebViewPdfExporter.Callback() { ... });
 */
public final class WebViewPdfExporter {

    private static final String TAG = "WebViewPdfExporter";

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
        Log.d(TAG, "performExport() started");
        if (webView == null) throw new IllegalArgumentException("webView == null");
        if (attrs == null) throw new IllegalArgumentException("attrs == null");
        if (outFile == null) throw new IllegalArgumentException("outFile == null");
        
        Log.d(TAG, "WebView width: " + webView.getWidth() + ", height: " + webView.getHeight() + ", contentHeight: " + webView.getContentHeight());

        // Compute page size in pixels using densityDpi and mediaSize (mils)
        final PrintAttributes.MediaSize mediaSize = attrs.getMediaSize();
        final int densityDpi = webView.getResources().getDisplayMetrics().densityDpi;
        final float density = webView.getResources().getDisplayMetrics().density;

        // Fallback to A4 if mediaSize is null
        int pageWidthMils = (mediaSize != null) ? mediaSize.getWidthMils() : 827; // A4 ~ 827 mils (210mm)
        int pageHeightMils = (mediaSize != null) ? mediaSize.getHeightMils() : 1169; // A4 ~ 1169 mils (297mm)

        // Convert mils -> inches (1000 mils = 1 inch), then inches -> pixels (dpi)
        int pageWidthPx = Math.max(1, pageWidthMils * densityDpi / 1000);
        int pageHeightPx = Math.max(1, pageHeightMils * densityDpi / 1000);

        // Ensure WebView has a valid width; otherwise force a measure/layout to width = pageWidthPx
        int webViewWidth = webView.getWidth();
        if (webViewWidth <= 0) {
            // If WebView hasn't been measured, layout it with desired width
            webView.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(pageWidthPx, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            );
            webView.layout(0, 0, webView.getMeasuredWidth(), webView.getMeasuredHeight());
            webViewWidth = webView.getMeasuredWidth();
        }

        // contentHeight (in "CSS pixels")
        int contentHeightCss = webView.getContentHeight();
        Log.d(TAG, "contentHeight (CSS): " + contentHeightCss + ", density: " + density);
        
        if (contentHeightCss <= 0) {
            // if contentHeight not ready, wait a bit and try again
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            contentHeightCss = webView.getContentHeight();
            if (contentHeightCss <= 0) {
                // Last resort: use measured height
                int measuredHeight = webView.getMeasuredHeight();
                contentHeightCss = (int) (measuredHeight / density);
                Log.d(TAG, "Using measuredHeight as fallback, converted to CSS: " + contentHeightCss);
            }
        }
        
        // Calculate scale to fit WebView width into page width
        float scale = (float) pageWidthPx / (float) webViewWidth;
        Log.d(TAG, "Scale factor: " + scale + " (pageWidth: " + pageWidthPx + ", webViewWidth: " + webViewWidth + ")");
        
        // CORRECT CALCULATION:
        // WebView.contentHeight is in CSS pixels
        // Page height is in device pixels
        // 
        // To calculate how many CSS pixels fit on one page:
        //   pageHeightPx (device pixels) = pageHeightPx / density (CSS pixels)
        // 
        // But we also need to account for page margins in CSS pixels
        // The @page margin in the HTML template is 18mm top, 16mm bottom = 34mm total
        // Converting mm to CSS pixels: 1mm ≈ 3.78px at 96 DPI (CSS standard)
        // But we're using device pixels, so: 1mm = (densityDpi / 25.4) pixels
        // Margin in device pixels: 34mm * (densityDpi / 25.4)
        // Margin in CSS pixels: (34mm * densityDpi / 25.4) / density = 34mm * densityDpi / (25.4 * density)
        // Since density = densityDpi / 160: margin = 34 * 160 / 25.4 ≈ 214 CSS px
        //
        // Actually, simpler: the @page margin is already accounted for in the HTML layout
        // The contentHeight from WebView already excludes margins
        // So we can directly calculate: pageHeightInContentCss = pageHeightPx / density
        
        float pageHeightInContentCss = pageHeightPx / density;
        int pages = (int) Math.ceil((double) contentHeightCss / (double) pageHeightInContentCss);
        
        // Ensure at least 1 page
        if (pages <= 0) {
            pages = 1;
        }
        
        // DETAILED LOGGING
        Log.d(TAG, "=== PDF EXPORT CALCULATIONS ===");
        Log.d(TAG, "WebView dimensions:");
        Log.d(TAG, "  width: " + webViewWidth + " device px");
        Log.d(TAG, "  height: " + webView.getHeight() + " device px");
        Log.d(TAG, "  measuredHeight: " + webView.getMeasuredHeight() + " device px");
        Log.d(TAG, "  contentHeight (CSS): " + contentHeightCss + " CSS px");
        Log.d(TAG, "  contentHeight (device): " + (contentHeightCss * density) + " device px");
        Log.d(TAG, "");
        Log.d(TAG, "Page dimensions:");
        Log.d(TAG, "  width: " + pageWidthPx + " device px");
        Log.d(TAG, "  height: " + pageHeightPx + " device px");
        Log.d(TAG, "  width (mils): " + pageWidthMils + " mils");
        Log.d(TAG, "  height (mils): " + pageHeightMils + " mils");
        Log.d(TAG, "");
        Log.d(TAG, "Device metrics:");
        Log.d(TAG, "  density: " + density);
        Log.d(TAG, "  densityDpi: " + densityDpi);
        Log.d(TAG, "");
        Log.d(TAG, "Scale calculation:");
        Log.d(TAG, "  scale: " + scale + " (pageWidth: " + pageWidthPx + " / webViewWidth: " + webViewWidth + ")");
        Log.d(TAG, "");
        Log.d(TAG, "Page height calculation:");
        Log.d(TAG, "  pageHeightPx: " + pageHeightPx + " device px");
        Log.d(TAG, "  density: " + density);
        Log.d(TAG, "  pageHeightInContentCss: " + pageHeightInContentCss + " CSS px (=" + pageHeightPx + " / " + density + ")");
        Log.d(TAG, "");
        Log.d(TAG, "Content height:");
        Log.d(TAG, "  contentHeightCss: " + contentHeightCss + " CSS px");
        Log.d(TAG, "  contentHeightDevicePx: " + (contentHeightCss * density) + " device px");
        Log.d(TAG, "");
        Log.d(TAG, "Page count calculation:");
        Log.d(TAG, "  pages = ceil(" + contentHeightCss + " / " + pageHeightInContentCss + ")");
        Log.d(TAG, "  pages = ceil(" + (contentHeightCss / pageHeightInContentCss) + ")");
        Log.d(TAG, "  calculated pages: " + pages);
        Log.d(TAG, "=== END CALCULATIONS ===");
        Log.d(TAG, "");

        // NEW APPROACH: Render WebView to a Bitmap once, then extract page portions
        // This avoids the negative coordinate clipping issue
        Log.d(TAG, "Rendering WebView to Bitmap...");
        int contentHeightDevicePx = (int) Math.ceil(contentHeightCss * density);
        
        // Create a bitmap large enough to hold the entire WebView content
        Bitmap webViewBitmap = null;
        try {
            webViewBitmap = Bitmap.createBitmap(webViewWidth, contentHeightDevicePx, Bitmap.Config.ARGB_8888);
            Canvas bitmapCanvas = new Canvas(webViewBitmap);
            webView.draw(bitmapCanvas);
            Log.d(TAG, "WebView rendered to Bitmap: " + webViewWidth + "x" + contentHeightDevicePx);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory rendering WebView to Bitmap", e);
            throw new IOException("Out of memory rendering WebView", e);
        }
        
        PdfDocument document = new PdfDocument();

        try {
            for (int i = 0; i < pages; i++) {
                // Calculate start and end positions in CSS pixels
                float pageStartCss = i * pageHeightInContentCss;
                float pageEndCss = Math.min((i + 1) * pageHeightInContentCss, contentHeightCss);
                
                // Skip empty pages
                if (pageStartCss >= contentHeightCss) {
                    Log.d(TAG, "Skipping page " + (i + 1) + " - start " + pageStartCss + " >= content " + contentHeightCss);
                    break;
                }
                
                Log.d(TAG, "");
                Log.d(TAG, "=== PAGE " + (i + 1) + "/" + pages + " ===");
                Log.d(TAG, "CSS pixel calculations:");
                Log.d(TAG, "  pageStartCss: " + pageStartCss + " CSS px");
                Log.d(TAG, "  pageEndCss: " + pageEndCss + " CSS px");
                Log.d(TAG, "  pageHeightInContentCss: " + pageHeightInContentCss + " CSS px");
                Log.d(TAG, "");
                
                // Convert to device pixels
                int pageStartDevicePx = (int) (pageStartCss * density);
                int pageEndDevicePx = (int) Math.min(pageEndCss * density, contentHeightDevicePx);
                int pageHeightDevicePx = pageEndDevicePx - pageStartDevicePx;
                
                Log.d(TAG, "Device pixel calculations:");
                Log.d(TAG, "  pageStartDevicePx: " + pageStartDevicePx + " device px");
                Log.d(TAG, "  pageEndDevicePx: " + pageEndDevicePx + " device px");
                Log.d(TAG, "  pageHeightDevicePx: " + pageHeightDevicePx + " device px");
                Log.d(TAG, "");
                
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, i + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Extract the portion of the bitmap we need for this page
                // Source: bitmap from (0, pageStartDevicePx) with size (webViewWidth, pageHeightDevicePx)
                // Destination: canvas at (0, 0) with size (pageWidthPx, pageHeightPx)
                
                // Calculate source rectangle in bitmap
                int srcLeft = 0;
                int srcTop = pageStartDevicePx;
                int srcRight = webViewWidth;
                int srcBottom = Math.min(pageEndDevicePx, contentHeightDevicePx);
                
                // Calculate destination rectangle on canvas
                float dstLeft = 0;
                float dstTop = 0;
                float dstRight = pageWidthPx;
                float dstBottom = pageHeightPx;
                
                // Scale the bitmap to fit page width if needed
                float bitmapScale = (float) pageWidthPx / (float) webViewWidth;
                
                Log.d(TAG, "Drawing bitmap portion:");
                Log.d(TAG, "  Source: (" + srcLeft + ", " + srcTop + ") to (" + srcRight + ", " + srcBottom + ")");
                Log.d(TAG, "  Destination: (" + dstLeft + ", " + dstTop + ") to (" + dstRight + ", " + dstBottom + ")");
                Log.d(TAG, "  Scale: " + bitmapScale);
                Log.d(TAG, "");
                
                // Draw the portion of the bitmap
                android.graphics.Rect srcRect = new android.graphics.Rect(srcLeft, srcTop, srcRight, srcBottom);
                android.graphics.RectF dstRect = new android.graphics.RectF(dstLeft, dstTop, dstRight, dstBottom);
                
                canvas.drawBitmap(webViewBitmap, srcRect, dstRect, null);
                
                Log.d(TAG, "Bitmap portion drawn");
                Log.d(TAG, "=== END PAGE " + (i + 1) + " ===");
                Log.d(TAG, "");
                
                // Draw page footer with page number (draw on original canvas, not scaled)
                if (reportNumber != null && !reportNumber.isEmpty()) {
                    // Footer text: "АXXXXX/mmyy - Страница N из T"
                    String footerText = reportNumber + " - Страница " + (i + 1) + " из " + pages;
                    
                    // Set up text paint
                    android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                    paint.setColor(0xFF6b7280); // muted color
                    paint.setTextSize(28); // ~10pt at 300 DPI
                    paint.setTextAlign(android.graphics.Paint.Align.RIGHT);
                    paint.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
                    
                    // Footer position: bottom right, with margins (in device pixels)
                    // Convert mm to pixels: 1mm = (densityDpi / 25.4) pixels
                    // Fields are now 10mm on all sides
                    float footerX = pageWidthPx - (10f * densityDpi / 25.4f); // 10mm margin from right
                    float footerY = pageHeightPx - (2f * densityDpi / 25.4f); // 2mm from bottom
                    
                    // Get fresh canvas (after restore, transformations are reset)
                    Canvas pageCanvas = page.getCanvas();
                    pageCanvas.drawText(footerText, footerX, footerY, paint);
                }
                
                document.finishPage(page);
            }

            // Write the document content to the output file
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