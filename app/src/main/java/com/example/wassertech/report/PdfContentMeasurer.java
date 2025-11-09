package com.example.wassertech.report;

import android.util.Log;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PdfContentMeasurer {
    private static final String TAG = "PdfContentMeasurer";

    public static class MeasurementResult {
        public final int contentHeightCss;
        public final int contentWidthCss;
        public final List<PdfBoundaryModels.ComponentBoundary> componentBoundaries;
        public final List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries;
        public final boolean hasError;
        public final String errorMessage;
        public final JSONObject debugJson;

        private MeasurementResult(int contentHeightCss, int contentWidthCss,
                                  List<PdfBoundaryModels.ComponentBoundary> componentBoundaries,
                                  List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries,
                                  boolean hasError, String errorMessage, JSONObject debugJson) {
            this.contentHeightCss = contentHeightCss;
            this.contentWidthCss = contentWidthCss;
            this.componentBoundaries = componentBoundaries;
            this.sectionHeaderBoundaries = sectionHeaderBoundaries;
            this.hasError = hasError;
            this.errorMessage = errorMessage;
            this.debugJson = debugJson;
        }

        public static MeasurementResult success(int contentHeightCss, int contentWidthCss,
                                                List<PdfBoundaryModels.ComponentBoundary> componentBoundaries,
                                                List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries,
                                                JSONObject debugJson) {
            return new MeasurementResult(contentHeightCss, contentWidthCss, componentBoundaries,
                    sectionHeaderBoundaries, false, null, debugJson);
        }

        public static MeasurementResult error(String errorMessage, int fallbackHeight, JSONObject debugJson) {
            return new MeasurementResult(fallbackHeight, 794, new ArrayList<>(), new ArrayList<>(),
                    true, errorMessage, debugJson);
        }
    }

    public static MeasurementResult measureContent(WebView webView) {
        try {
            // Не убирайте, это может влиять на рендер!
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        final String measureJs = "(function() { " +
                "  try { " +
                "    var root = document.getElementById('print-root') || document.body;" +
                "    var debugMsg = [];" +
                "    if (!root) { debugMsg.push('print-root not found'); return JSON.stringify({error: 'print-root not found', debugMsgs: debugMsg}); } " +

                "    debugMsg.push('Root found: ' + (root ? 'present' : 'missing'));" +
                "    var rootRect = root.getBoundingClientRect();" +
                "    var cssWidth = Math.max(root.scrollWidth || 0, root.offsetWidth || 0, root.clientWidth || 0, rootRect.width || 0);" +
                "    var cssHeight = Math.max(root.scrollHeight || 0, root.offsetHeight || 0, root.clientHeight || 0, rootRect.height || 0);" +
                "    debugMsg.push('Root size: width=' + cssWidth + ', height=' + cssHeight);" +

                "    debugMsg.push('HTML length: ' + (root.innerHTML ? root.innerHTML.length : 'n/a'));" +

                "    var sectionHeaders = Array.from(root.querySelectorAll('h2.section-header-red'));" +
                "    debugMsg.push('Found ' + sectionHeaders.length + ' section headers');" +
                "    var sectionHeaderBounds = [];" +
                "    for (var i = 0; i < sectionHeaders.length; i++) { " +
                "      var header = sectionHeaders[i];" +
                "      var top = header.offsetTop;" +
                "      var bottom = top + header.offsetHeight;" +
                "      debugMsg.push('SectionHeader['+i+']: text=' + (header.textContent||'').substring(0,30) + ', top=' + top + ', bottom=' + bottom);" +
                "      sectionHeaderBounds.push({top: top, bottom: bottom});" +
                "    }" +

                "    var components = Array.from(root.querySelectorAll('.component-card'));" +
                "    debugMsg.push('Found ' + components.length + ' component cards');" +
                "    var componentBounds = [];" +
                "    for (var i = 0; i < components.length; i++) { " +
                "      var comp = components[i];" +
                "      var top = comp.offsetTop;" +
                "      var bottom = top + comp.offsetHeight;" +
                "      var header = comp.querySelector('.component-header');" +
                "      var fields = comp.querySelector('.component-fields');" +

                "      var headerTop = top, headerBottom = top, fieldsTop = top, fieldsBottom = bottom;" +
                "      if (header) { headerTop = header.offsetTop; headerBottom = headerTop + header.offsetHeight; }" +
                "      if (fields) { fieldsTop = fields.offsetTop; fieldsBottom = fieldsTop + fields.offsetHeight; }" +
                "      var compName = header ? header.textContent : 'no name';" +
                "      debugMsg.push('Component[' + i + ']: name=' + (compName||'').substring(0,30) + ', top=' + top + ', bottom=' + bottom);" +
                "      componentBounds.push({top: top, bottom: bottom, height: bottom-top, headerTop: headerTop, headerBottom: headerBottom, fieldsTop: fieldsTop, fieldsBottom: fieldsBottom});" +
                "    }" +

                "    return JSON.stringify({ " +
                "      cssWidth: cssWidth," +
                "      cssHeight: cssHeight," +
                "      dpr: window.devicePixelRatio || 1," +
                "      innerH: window.innerHeight || 0," +
                "      componentBounds: componentBounds," +
                "      sectionHeaderBounds: sectionHeaderBounds," +
                "      debug: { debugMsgs: debugMsg, sectionHeaderCount: sectionHeaders.length, componentCount: components.length, rootScrollTop: root.scrollTop, rootOffsetTop: root.offsetTop } " +
                "    });" +
                "  } catch(e) { " +
                "    var debugError = 'JavaScript error: ' + e.toString();" +
                "    return JSON.stringify({error: debugError, stack: e.stack, debugMsgs: [debugError]});" +
                "  } " +
                "})();";

        String jsResult = null;
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final String[] resultHolder = new String[1];

            Log.d(TAG, "Executing JS measurement...");

            webView.evaluateJavascript(measureJs, value -> {
                resultHolder[0] = value;
                latch.countDown();
            });

            boolean completed = latch.await(3, TimeUnit.SECONDS);
            if (completed && resultHolder[0] != null && !resultHolder[0].equals("null")) {
                jsResult = resultHolder[0];
                if (jsResult.startsWith("\"") && jsResult.endsWith("\"")) {
                    jsResult = jsResult.substring(1, jsResult.length() - 1);
                    jsResult = jsResult.replace("\\\"", "\"").replace("\\\\", "\\");
                }
                Log.d(TAG, "JS finished, result length: " + jsResult.length());
                Log.d(TAG, "JS raw: " + jsResult);
            } else {
                Log.e(TAG, "JS evaluation timed out or null. completed=" + completed + ", result=" + resultHolder[0]);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Interrupted while waiting for JS result", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute JS", e);
        }

        if (jsResult != null && !jsResult.isEmpty() && !jsResult.equals("null")) {
            try {
                JSONObject json = new JSONObject(jsResult);

                if (json.has("error")) {
                    String errorMsg = json.getString("error");
                    Log.e(TAG, "JS returned error: " + errorMsg);
                    if (json.has("stack")) {
                        Log.e(TAG, "JS stack: " + json.getString("stack"));
                    }
                    if (json.has("debugMsgs")) {
                        Log.e(TAG, "JS debug: " + json.getJSONArray("debugMsgs").toString());
                    }
                    return MeasurementResult.error(errorMsg, webView.getContentHeight(),
                            json.has("debug") ? json.getJSONObject("debug") : null);
                }

                int contentHeightCss = (int) Math.round(json.getDouble("cssHeight"));
                int contentWidthCss = (int) Math.round(json.getDouble("cssWidth"));
                Log.d(TAG, "JS measurement result: cssHeight=" + contentHeightCss + " cssWidth=" + contentWidthCss);

                // Additional debug info
                if (json.has("debug")) {
                    JSONObject debug = json.getJSONObject("debug");
                    Log.d(TAG, "Debug info from JS: " + debug.toString());
                    if (debug.has("debugMsgs")) {
                        Log.d(TAG, "DebugMsgs: " + debug.getJSONArray("debugMsgs").toString());
                    }
                }

                List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries = new ArrayList<>();
                if (json.has("sectionHeaderBounds")) {
                    JSONArray boundsArray = json.getJSONArray("sectionHeaderBounds");
                    Log.d(TAG, "Parsing " + boundsArray.length() + " section header boundaries...");
                    for (int i = 0; i < boundsArray.length(); i++) {
                        JSONObject bound = boundsArray.getJSONObject(i);
                        int top = bound.getInt("top");
                        int bottom = bound.getInt("bottom");
                        sectionHeaderBoundaries.add(new PdfBoundaryModels.SectionHeaderBoundary(top, bottom));
                        Log.d(TAG, "  SectionHeader[" + i + "]: top=" + top + ", bottom=" + bottom + " px");
                    }
                } else {
                    Log.w(TAG, "No sectionHeaderBounds in JS result");
                }

                List<PdfBoundaryModels.ComponentBoundary> componentBoundaries = new ArrayList<>();
                if (json.has("componentBounds")) {
                    JSONArray boundsArray = json.getJSONArray("componentBounds");
                    Log.d(TAG, "Parsing " + boundsArray.length() + " components...");
                    for (int i = 0; i < boundsArray.length(); i++) {
                        JSONObject bound = boundsArray.getJSONObject(i);
                        int top = bound.getInt("top");
                        int bottom = bound.getInt("bottom");
                        int headerTop = bound.has("headerTop") ? bound.getInt("headerTop") : top;
                        int headerBottom = bound.has("headerBottom") ? bound.getInt("headerBottom") : top;
                        int fieldsTop = bound.has("fieldsTop") ? bound.getInt("fieldsTop") : top;
                        int fieldsBottom = bound.has("fieldsBottom") ? bound.getInt("fieldsBottom") : bottom;
                        componentBoundaries.add(new PdfBoundaryModels.ComponentBoundary(
                                top, bottom, headerTop, headerBottom, fieldsTop, fieldsBottom));
                        Log.d(TAG, "  Component[" + i + "]: top=" + top + ", bottom=" + bottom +
                                ", header=" + headerTop + "-" + headerBottom +
                                ", fields=" + fieldsTop + "-" + fieldsBottom + " px");
                    }
                } else {
                    Log.w(TAG, "No componentBounds in JS result");
                }

                return MeasurementResult.success(contentHeightCss, contentWidthCss,
                        componentBoundaries, sectionHeaderBoundaries,
                        json.has("debug") ? json.getJSONObject("debug") : null);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse JS result: " + jsResult, e);
                return MeasurementResult.error("Failed to parse JS result: " + e.getMessage(), webView.getContentHeight(), null);
            }
        } else {
            Log.e(TAG, "JS result is null or empty, fallback to WebView.getContentHeight(). Content height: " + webView.getContentHeight());
            return MeasurementResult.error("JS returned null or empty", webView.getContentHeight(), null);
        }
    }
}