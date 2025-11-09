package com.example.wassertech.report;

import android.util.Log;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Measures PDF content dimensions and boundaries via JavaScript.
 */
public class PdfContentMeasurer {
    private static final String TAG = "PdfContentMeasurer";
    
    /**
     * Result of content measurement.
     */
    public static class MeasurementResult {
        public final int contentHeightCss;
        public final int contentWidthCss;
        public final List<PdfBoundaryModels.ComponentBoundary> componentBoundaries;
        public final List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries;
        public final boolean hasError;
        public final String errorMessage;
        
        private MeasurementResult(int contentHeightCss, int contentWidthCss,
                                  List<PdfBoundaryModels.ComponentBoundary> componentBoundaries,
                                  List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries,
                                  boolean hasError, String errorMessage) {
            this.contentHeightCss = contentHeightCss;
            this.contentWidthCss = contentWidthCss;
            this.componentBoundaries = componentBoundaries;
            this.sectionHeaderBoundaries = sectionHeaderBoundaries;
            this.hasError = hasError;
            this.errorMessage = errorMessage;
        }
        
        public static MeasurementResult success(int contentHeightCss, int contentWidthCss,
                                                List<PdfBoundaryModels.ComponentBoundary> componentBoundaries,
                                                List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries) {
            return new MeasurementResult(contentHeightCss, contentWidthCss, componentBoundaries, 
                                       sectionHeaderBoundaries, false, null);
        }
        
        public static MeasurementResult error(String errorMessage, int fallbackHeight) {
            return new MeasurementResult(fallbackHeight, 794, new ArrayList<>(), new ArrayList<>(), 
                                       true, errorMessage);
        }
    }
    
    /**
     * Measure content height and boundaries via JavaScript from #print-root.
     * Returns a MeasurementResult with all boundaries, or falls back to WebView.getContentHeight().
     */
    public static MeasurementResult measureContent(WebView webView) {
        // Wait for layout to stabilize and content to render
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simplified synchronous JavaScript - evaluateJavascript doesn't support Promise returns well
        final String measureJs = "(function() {" +
                "  try {" +
                "    const root = document.getElementById('print-root') || document.body;" +
                "    if (!root) { return JSON.stringify({error: 'print-root not found'}); }" +
                "    " +
                "    // Force layout recalculation" +
                "    root.offsetHeight;" +
                "    " +
                "    // Get root position - use getBoundingClientRect which gives position relative to viewport" +
                "    const rootRect = root.getBoundingClientRect();" +
                "    const rootTop = 0; // Root is at top of document, so its top relative to itself is 0" +
                "    " +
                "    // Measure root dimensions" +
                "    const cssWidth = Math.max(root.scrollWidth || 0, root.offsetWidth || 0, root.clientWidth || 0, rootRect.width || 0);" +
                "    const cssHeight = Math.max(root.scrollHeight || 0, root.offsetHeight || 0, root.clientHeight || 0, rootRect.height || 0);" +
                "    " +
                "    // Debug: log what we find" +
                "    console.log('DEBUG: Root found, width=' + cssWidth + ', height=' + cssHeight);" +
                "    " +
                "    // Find all section headers - they have class 'section-header-red' on h2 elements" +
                "    const sectionHeaders = Array.from(root.querySelectorAll('h2.section-header-red'));" +
                "    console.log('DEBUG: Found ' + sectionHeaders.length + ' section headers');" +
                "    " +
                "    // Helper function to get element position relative to root" +
                "    function getOffsetTop(el) {" +
                "      let offset = 0;" +
                "      while (el && el !== root && el !== document.body) {" +
                "        offset += el.offsetTop;" +
                "        el = el.offsetParent;" +
                "      }" +
                "      return offset;" +
                "    }" +
                "    " +
                "    const sectionHeaderBounds = [];" +
                "    for (let i = 0; i < sectionHeaders.length; i++) {" +
                "      const header = sectionHeaders[i];" +
                "      const top = getOffsetTop(header);" +
                "      const bottom = top + header.offsetHeight;" +
                "      console.log('DEBUG: SectionHeader[' + i + '] text=' + header.textContent.substring(0, 30) + ', top=' + top + ', bottom=' + bottom);" +
                "      sectionHeaderBounds.push({top: top, bottom: bottom});" +
                "    }" +
                "    " +
                "    // Find all component cards - they have class 'component-card'" +
                "    const components = Array.from(root.querySelectorAll('.component-card'));" +
                "    console.log('DEBUG: Found ' + components.length + ' component cards');" +
                "    " +
                "    const componentBounds = [];" +
                "    for (let i = 0; i < components.length; i++) {" +
                "      const comp = components[i];" +
                "      const top = getOffsetTop(comp);" +
                "      const bottom = top + comp.offsetHeight;" +
                "      " +
                "      // Get internal structure" +
                "      const header = comp.querySelector('.component-header');" +
                "      const fields = comp.querySelector('.component-fields');" +
                "      " +
                "      let headerTop = top;" +
                "      let headerBottom = top;" +
                "      let fieldsTop = top;" +
                "      let fieldsBottom = bottom;" +
                "      " +
                "      if (header) {" +
                "        headerTop = getOffsetTop(header);" +
                "        headerBottom = headerTop + header.offsetHeight;" +
                "      }" +
                "      " +
                "      if (fields) {" +
                "        fieldsTop = getOffsetTop(fields);" +
                "        fieldsBottom = fieldsTop + fields.offsetHeight;" +
                "      }" +
                "      " +
                "      const compName = header ? header.textContent : 'no name';" +
                "      console.log('DEBUG: Component[' + i + '] name=' + compName.substring(0, 30) + ', top=' + top + ', bottom=' + bottom);" +
                "      " +
                "      componentBounds.push({" +
                "        top: top," +
                "        bottom: bottom," +
                "        height: bottom - top," +
                "        headerTop: headerTop," +
                "        headerBottom: headerBottom," +
                "        fieldsTop: fieldsTop," +
                "        fieldsBottom: fieldsBottom" +
                "      });" +
                "    }" +
                "    " +
                "    return JSON.stringify({" +
                "      cssWidth: cssWidth," +
                "      cssHeight: cssHeight," +
                "      dpr: window.devicePixelRatio || 1," +
                "      innerH: window.innerHeight || 0," +
                "      componentBounds: componentBounds," +
                "      sectionHeaderBounds: sectionHeaderBounds," +
                "      debug: {" +
                "        sectionHeaderCount: sectionHeaders.length," +
                "        componentCount: components.length," +
                "        rootScrollTop: root.scrollTop," +
                "        rootOffsetTop: root.offsetTop" +
                "      }" +
                "    });" +
                "  } catch(e) {" +
                "    console.error('DEBUG: JavaScript error:', e);" +
                "    return JSON.stringify({error: e.toString(), stack: e.stack});" +
                "  }" +
                "})();";
        
        // Execute JavaScript and get result
        String jsResult = null;
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final String[] resultHolder = new String[1];
            
            Log.d(TAG, "Executing JavaScript measurement (synchronous)...");
            webView.evaluateJavascript(measureJs, value -> {
                resultHolder[0] = value;
                latch.countDown();
            });
            
            // Wait for result (max 3 seconds)
            boolean completed = latch.await(3, TimeUnit.SECONDS);
            if (completed && resultHolder[0] != null && !resultHolder[0].equals("null")) {
                jsResult = resultHolder[0];
                // Remove quotes if present
                if (jsResult.startsWith("\"") && jsResult.endsWith("\"")) {
                    jsResult = jsResult.substring(1, jsResult.length() - 1);
                    // Unescape JSON string
                    jsResult = jsResult.replace("\\\"", "\"").replace("\\\\", "\\");
                }
                Log.d(TAG, "JavaScript evaluation completed, result length: " + (jsResult != null ? jsResult.length() : 0));
            } else {
                Log.w(TAG, "JavaScript evaluation timed out or returned null. completed=" + completed + ", result=" + resultHolder[0]);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Interrupted while waiting for JavaScript result", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute JavaScript", e);
        }
        
        // Parse result
        if (jsResult != null && !jsResult.isEmpty() && !jsResult.equals("null")) {
            try {
                JSONObject json = new JSONObject(jsResult);
                
                // Check for errors
                if (json.has("error")) {
                    String errorMsg = json.getString("error");
                    Log.e(TAG, "JavaScript returned error: " + errorMsg);
                    if (json.has("stack")) {
                        Log.e(TAG, "Stack trace: " + json.getString("stack"));
                    }
                    return MeasurementResult.error(errorMsg, webView.getContentHeight());
                }
                
                int contentHeightCss = (int) Math.round(json.getDouble("cssHeight"));
                int contentWidthCss = (int) Math.round(json.getDouble("cssWidth"));
                
                Log.d(TAG, "JavaScript measurement result:");
                Log.d(TAG, "  cssHeight: " + contentHeightCss + " CSS px");
                Log.d(TAG, "  cssWidth: " + contentWidthCss + " CSS px");
                
                // Parse debug info
                if (json.has("debug")) {
                    JSONObject debug = json.getJSONObject("debug");
                    Log.d(TAG, "  Debug info:");
                    if (debug.has("sectionHeaderCount")) {
                        Log.d(TAG, "    Section headers found: " + debug.getInt("sectionHeaderCount"));
                    }
                    if (debug.has("componentCount")) {
                        Log.d(TAG, "    Components found: " + debug.getInt("componentCount"));
                    }
                }
                
                // Parse section header boundaries
                List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries = new ArrayList<>();
                if (json.has("sectionHeaderBounds")) {
                    JSONArray boundsArray = json.getJSONArray("sectionHeaderBounds");
                    Log.d(TAG, "  Parsing " + boundsArray.length() + " section header boundaries:");
                    for (int i = 0; i < boundsArray.length(); i++) {
                        JSONObject bound = boundsArray.getJSONObject(i);
                        int top = bound.getInt("top");
                        int bottom = bound.getInt("bottom");
                        sectionHeaderBoundaries.add(new PdfBoundaryModels.SectionHeaderBoundary(top, bottom));
                        Log.d(TAG, "    SectionHeader[" + i + "]: top=" + top + ", bottom=" + bottom + " CSS px");
                    }
                } else {
                    Log.w(TAG, "  No sectionHeaderBounds found in JavaScript result");
                }
                
                // Parse component boundaries
                List<PdfBoundaryModels.ComponentBoundary> componentBoundaries = new ArrayList<>();
                if (json.has("componentBounds")) {
                    JSONArray boundsArray = json.getJSONArray("componentBounds");
                    Log.d(TAG, "  Parsing " + boundsArray.length() + " component boundaries:");
                    for (int i = 0; i < boundsArray.length(); i++) {
                        JSONObject bound = boundsArray.getJSONObject(i);
                        int top = bound.getInt("top");
                        int bottom = bound.getInt("bottom");
                        int headerTop = bound.has("headerTop") ? bound.getInt("headerTop") : top;
                        int headerBottom = bound.has("headerBottom") ? bound.getInt("headerBottom") : top;
                        int fieldsTop = bound.has("fieldsTop") ? bound.getInt("fieldsTop") : top;
                        int fieldsBottom = bound.has("fieldsBottom") ? bound.getInt("fieldsBottom") : bottom;
                        componentBoundaries.add(new PdfBoundaryModels.ComponentBoundary(top, bottom, headerTop, headerBottom, 
                                                                                      fieldsTop, fieldsBottom));
                        Log.d(TAG, "    Component[" + i + "]: top=" + top + ", bottom=" + bottom + 
                              ", header=" + headerTop + "-" + headerBottom + 
                              ", fields=" + fieldsTop + "-" + fieldsBottom + " CSS px");
                    }
                } else {
                    Log.w(TAG, "  No componentBounds found in JavaScript result");
                }
                
                return MeasurementResult.success(contentHeightCss, contentWidthCss, 
                                                componentBoundaries, sectionHeaderBoundaries);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse JavaScript result: " + jsResult, e);
                Log.e(TAG, "Exception details: " + e.getMessage());
                e.printStackTrace();
                return MeasurementResult.error("Failed to parse JavaScript result: " + e.getMessage(), 
                                              webView.getContentHeight());
            }
        } else {
            // Fallback to WebView.getContentHeight()
            Log.w(TAG, "JavaScript returned null or empty, falling back to WebView.getContentHeight()");
            int fallbackHeight = webView.getContentHeight();
            Log.d(TAG, "WebView.getContentHeight() returned: " + fallbackHeight + " CSS px");
            Log.w(TAG, "WARNING: Using WebView.getContentHeight() may be inaccurate. Component boundaries not available.");
            return MeasurementResult.error("JavaScript returned null or empty", fallbackHeight);
        }
    }
}

