package com.example.wassertech.report;

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
        Log.d(TAG, "export() called, isMainThread: " + (Looper.myLooper() == Looper.getMainLooper()));
        
        // Ensure we run the WebView drawing on the UI thread.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread, execute directly
            Log.d(TAG, "Already on main thread, executing directly");
            try {
                performExport(webView, attrs, outFile);
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
                    performExport(webView, attrs, outFile);
                    Log.d(TAG, "performExport() completed successfully");
                    cb.onSuccess();
                } catch (Throwable t) {
                    Log.e(TAG, "PDF export failed", t);
                    cb.onError(t);
                }
            });
        }
    }

    private static void performExport(WebView webView, PrintAttributes attrs, File outFile) throws IOException {
        Log.d(TAG, "performExport() started");
        if (webView == null) throw new IllegalArgumentException("webView == null");
        if (attrs == null) throw new IllegalArgumentException("attrs == null");
        if (outFile == null) throw new IllegalArgumentException("outFile == null");
        
        Log.d(TAG, "WebView width: " + webView.getWidth() + ", height: " + webView.getHeight() + ", contentHeight: " + webView.getContentHeight());

        // Compute page size in pixels using densityDpi and mediaSize (mils)
        final PrintAttributes.MediaSize mediaSize = attrs.getMediaSize();
        final int densityDpi = webView.getResources().getDisplayMetrics().densityDpi;

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

        // contentHeight (in "CSS pixels"). Convert to device pixels using density (approximation).
        float density = webView.getResources().getDisplayMetrics().density;
        int contentHeightCss = webView.getContentHeight();
        Log.d(TAG, "contentHeight (CSS): " + contentHeightCss + ", density: " + density);
        
        if (contentHeightCss <= 0) {
            // if contentHeight not ready, try to use measured height
            contentHeightCss = webView.getMeasuredHeight();
            Log.d(TAG, "Using measuredHeight as fallback: " + contentHeightCss);
        }
        
        // contentHeight возвращается в CSS пикселях (независимо от density)
        // Нужно конвертировать в device pixels для правильного расчета страниц
        int measuredHeight = webView.getMeasuredHeight();
        int contentHeightPx;
        
        // contentHeight всегда в CSS пикселях, конвертируем в device pixels
        if (contentHeightCss > 0) {
            contentHeightPx = (int) (contentHeightCss * density);
            Log.d(TAG, "Converted contentHeight from CSS to device pixels: " + contentHeightCss + " -> " + contentHeightPx);
        } else {
            // Fallback: используем measuredHeight
            contentHeightPx = Math.max(1, measuredHeight);
            Log.d(TAG, "Using measuredHeight as fallback: " + contentHeightPx);
        }
        
        Log.d(TAG, "Final contentHeightPx: " + contentHeightPx + ", measuredHeight: " + measuredHeight + ", density: " + density);

        // Calculate scale to fit WebView width into page width
        float scale = (float) pageWidthPx / (float) webViewWidth;

        // Compute total scaled content height
        int totalScaledHeightPx = (int) Math.ceil(contentHeightPx * scale);

        // Determine number of pages
        int pages = (totalScaledHeightPx + pageHeightPx - 1) / pageHeightPx;
        Log.d(TAG, "Calculated pages: " + pages + ", totalScaledHeight: " + totalScaledHeightPx + ", pageHeight: " + pageHeightPx);

        PdfDocument document = new PdfDocument();

        try {
            for (int i = 0; i < pages; i++) {
                Log.d(TAG, "Rendering page " + (i + 1) + " of " + pages);
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, i + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Save canvas state
                canvas.save();

                // Apply scaling so WebView content fits page width
                canvas.scale(scale, scale);

                // Translate canvas so we render the correct slice of the WebView
                int dy = (int) ((i * (float) pageHeightPx) / scale);
                canvas.translate(0, -dy);

                // Draw the webView onto the canvas. This calls WebView.draw(Canvas) which renders the view.
                webView.draw(canvas);

                // Restore canvas and finish page
                canvas.restore();
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
        }
    }
}