package com.example.wassertech.report;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PdfContentMeasurer {
    private static final String TAG = "PdfContentMeasurer";
    
    public interface MeasurementCallback {
        void onResult(MeasurementResult result);
    }

    /**
     * RUN THIS FIRST for DOM test. Log will show "found"/"not found".
     */
    public static void debugPrintRoot(WebView webView) {
        webView.evaluateJavascript("document.getElementById('print-root') ? 'found' : 'not found';", value -> {
            Log.d(TAG, "print-root existence: " + value);
        });
    }

    public static class MeasurementResult {
        public final int contentHeightCss;
        public final int contentWidthCss;
        public final List<PdfBoundaryModels.ComponentBoundary> componentBoundaries;
        public final List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries;
        public final PdfBoundaryModels.SignatureBoundary signatureBoundary;
        public final boolean hasError;
        public final String errorMessage;
        public final JSONObject debugJson;

        private MeasurementResult(int contentHeightCss, int contentWidthCss,
                                  List<PdfBoundaryModels.ComponentBoundary> componentBoundaries,
                                  List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries,
                                  PdfBoundaryModels.SignatureBoundary signatureBoundary,
                                  boolean hasError, String errorMessage, JSONObject debugJson) {
            this.contentHeightCss = contentHeightCss;
            this.contentWidthCss = contentWidthCss;
            this.componentBoundaries = componentBoundaries;
            this.sectionHeaderBoundaries = sectionHeaderBoundaries;
            this.signatureBoundary = signatureBoundary;
            this.hasError = hasError;
            this.errorMessage = errorMessage;
            this.debugJson = debugJson;
        }

        public static MeasurementResult success(int contentHeightCss, int contentWidthCss,
                                                List<PdfBoundaryModels.ComponentBoundary> componentBoundaries,
                                                List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries,
                                                PdfBoundaryModels.SignatureBoundary signatureBoundary,
                                                JSONObject debugJson) {
            return new MeasurementResult(contentHeightCss, contentWidthCss, componentBoundaries,
                    sectionHeaderBoundaries, signatureBoundary, false, null, debugJson);
        }

        public static MeasurementResult error(String errorMessage, int fallbackHeight, JSONObject debugJson) {
            return new MeasurementResult(fallbackHeight, 794, new ArrayList<>(), new ArrayList<>(),
                    null, true, errorMessage, debugJson);
        }
    }

    /**
     * Асинхронное измерение контента WebView.
     * ВАЖНО: Вызывайте только после WebView.onPageFinished, на главном потоке!
     * 
     * @param webView WebView для измерения
     * @param callback Callback для получения результата
     * @param timeoutMs Таймаут в миллисекундах (по умолчанию 10000)
     * @return Runnable для отмены таймаута (если нужно)
     */
    public static Runnable measureContentAsync(WebView webView, MeasurementCallback callback, long timeoutMs) {
        // Проверяем, что мы на главном потоке
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e(TAG, "measureContentAsync must be called on main thread!");
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                callback.onResult(MeasurementResult.error("Must be called on main thread", 
                    webView.getContentHeight(), null));
            });
            return () -> {};
        }

        final long startTime = System.currentTimeMillis();
        final AtomicBoolean expired = new AtomicBoolean(false);
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        
        // Проверяем готовность WebView
        Log.d(TAG, "WebView state: progress=" + webView.getProgress() + 
              ", contentHeight=" + webView.getContentHeight() + 
              ", isMainThread=" + (Looper.myLooper() == Looper.getMainLooper()));

        // Проверяем, что JavaScript включен
        android.webkit.WebSettings settings = webView.getSettings();
        if (!settings.getJavaScriptEnabled()) {
            Log.e(TAG, "JavaScript is disabled in WebView settings!");
            callback.onResult(MeasurementResult.error("JavaScript is disabled", 
                webView.getContentHeight(), null));
            return () -> {};
        }
        Log.d(TAG, "JavaScript is enabled: " + settings.getJavaScriptEnabled());

        // Функция для создания fallback результата
        Runnable createFallbackResult = () -> {
            if (expired.getAndSet(true)) {
                Log.d(TAG, "Fallback already executed, ignoring");
                return;
            }
            long elapsed = System.currentTimeMillis() - startTime;
            Log.w(TAG, "Using fallback after " + elapsed + "ms: WebView.getContentHeight()");
            
            // WebView.getContentHeight() возвращает высоту в CSS пикселях (не device pixels!)
            // Это важно - не нужно делить на density
            int fallbackHeight = webView.getContentHeight();
            int fallbackWidth = webView.getWidth();
            
            // Но width в device pixels, конвертируем в CSS
            float density = webView.getContext().getResources().getDisplayMetrics().density;
            int cssWidth = (int) (fallbackWidth / density);
            
            // Высота уже в CSS пикселях (по документации WebView)
            int cssHeight = fallbackHeight;
            
            Log.d(TAG, "Fallback measurements: webViewHeight=" + fallbackHeight + 
                  " (CSS px), webViewWidth=" + fallbackWidth + " (device px), " +
                  "cssWidth=" + cssWidth + ", density=" + density);
            
            JSONObject fallbackDebug = new JSONObject();
            try {
                fallbackDebug.put("fallback", true);
                fallbackDebug.put("reason", "JS timeout");
                fallbackDebug.put("elapsedMs", elapsed);
                fallbackDebug.put("webViewHeight", fallbackHeight);
                fallbackDebug.put("cssHeight", cssHeight);
            } catch (Exception e) {
                // Игнорируем ошибки создания debug объекта
            }
            
            // Fallback должен возвращать error, но с данными для экспорта
            callback.onResult(MeasurementResult.error(
                "JS measurement timeout, using fallback",
                cssHeight > 0 ? cssHeight : fallbackHeight,
                fallbackDebug
            ));
        };

        // Устанавливаем таймаут для fallback
        Runnable timeoutRunnable = () -> {
            if (!expired.get()) {
                Log.w(TAG, "Measurement timeout after " + timeoutMs + "ms, using fallback");
                createFallbackResult.run();
            }
        };
        mainHandler.postDelayed(timeoutRunnable, timeoutMs);
        
        // Даём небольшое время для стабилизации рендера (не блокируя поток!)
        mainHandler.postDelayed(() -> {
            if (expired.get()) {
                Log.d(TAG, "Measurement already expired, skipping");
                return;
            }
            
            Log.d(TAG, "Starting JS measurement after stabilization delay");
            startMeasurement(webView, callback, expired, startTime, timeoutRunnable, mainHandler);
        }, 300); // Небольшая задержка для стабилизации, но не блокирующая
        
        return timeoutRunnable; // Возвращаем runnable для возможной отмены таймаута
    }
    
    private static void startMeasurement(WebView webView, MeasurementCallback callback, 
                                       AtomicBoolean expired, long startTime, Runnable timeoutRunnable,
                                       Handler mainHandler) {

        // Упрощённый JS код с лучшей обработкой ошибок
        final String measureJs = 
            "(function() { " +
            "  'use strict'; " +
            "  try { " +
            "    console.log('JS measurement started'); " +
            "    var root = document.getElementById('print-root'); " +
            "    if (!root) { root = document.body; } " +
            "    if (!root) { " +
            "      console.error('No root element found'); " +
            "      return JSON.stringify({error: 'No root element', debugMsgs: ['No root']}); " +
            "    } " +
            "    console.log('Root found, measuring...'); " +
            "    var rootRect = root.getBoundingClientRect(); " +
            "    var cssWidth = Math.max(root.scrollWidth || 0, root.offsetWidth || 0, root.clientWidth || 0); " +
            "    var cssHeight = Math.max(root.scrollHeight || 0, root.offsetHeight || 0, root.clientHeight || 0); " +
            "    console.log('Size: ' + cssWidth + 'x' + cssHeight); " +
            "    var result = { " +
            "      cssWidth: cssWidth, " +
            "      cssHeight: cssHeight, " +
            "      componentBounds: [], " +
            "      sectionHeaderBounds: [], " +
            "      debug: { " +
            "        debugMsgs: ['Measurement completed'], " +
            "        rootWidth: cssWidth, " +
            "        rootHeight: cssHeight " +
            "      } " +
            "    }; " +
            "    try { " +
            "      var sectionHeaders = root.querySelectorAll('h2.section-header-red'); " +
            "      for (var i = 0; i < sectionHeaders.length; i++) { " +
            "        var h = sectionHeaders[i]; " +
            "        var hRect = h.getBoundingClientRect(); " +
            "        var top = hRect.top - rootRect.top + root.scrollTop; " +
            "        var bottom = top + hRect.height; " +
            "        result.sectionHeaderBounds.push({top: Math.round(top), bottom: Math.round(bottom)}); " +
            "      } " +
            "      var components = root.querySelectorAll('.component-card'); " +
            "      for (var i = 0; i < components.length; i++) { " +
            "        var c = components[i]; " +
            "        var cRect = c.getBoundingClientRect(); " +
            "        var top = cRect.top - rootRect.top + root.scrollTop; " +
            "        var bottom = top + cRect.height; " +
            "        var header = c.querySelector('.component-header'); " +
            "        var fields = c.querySelector('.component-fields'); " +
            "        var headerTop = top; " +
            "        var headerBottom = top; " +
            "        var fieldsTop = top; " +
            "        var fieldsBottom = bottom; " +
            "        if (header) { " +
            "          var hRect = header.getBoundingClientRect(); " +
            "          headerTop = hRect.top - rootRect.top + root.scrollTop; " +
            "          headerBottom = headerTop + hRect.height; " +
            "        } " +
            "        if (fields) { " +
            "          var fRect = fields.getBoundingClientRect(); " +
            "          fieldsTop = fRect.top - rootRect.top + root.scrollTop; " +
            "          fieldsBottom = fieldsTop + fRect.height; " +
            "        } " +
            "        result.componentBounds.push({ " +
            "          top: Math.round(top), " +
            "          bottom: Math.round(bottom), " +
            "          headerTop: Math.round(headerTop), " +
            "          headerBottom: Math.round(headerBottom), " +
            "          fieldsTop: Math.round(fieldsTop), " +
            "          fieldsBottom: Math.round(fieldsBottom) " +
            "        }); " +
            "      } " +
            "      result.debug.componentCount = components.length; " +
            "      result.debug.sectionHeaderCount = sectionHeaders.length; " +
            "    } catch(e2) { " +
            "      console.warn('Error measuring components: ' + e2); " +
            "      result.debug.componentError = e2.toString(); " +
            "    } " +
            "    try { " +
            "      var signRow = root.querySelector('.sign-row'); " +
            "      if (signRow) { " +
            "        var signRect = signRow.getBoundingClientRect(); " +
            "        var signTop = signRect.top - rootRect.top + root.scrollTop; " +
            "        var signBottom = signTop + signRect.height; " +
            "        result.signatureBounds = {top: Math.round(signTop), bottom: Math.round(signBottom)}; " +
            "      } " +
            "    } catch(e3) { " +
            "      console.warn('Error measuring signature: ' + e3); " +
            "    } " +
            "    console.log('JS measurement completed'); " +
            "    return JSON.stringify(result); " +
            "  } catch(e) { " +
            "    console.error('JS measurement error: ' + e); " +
            "    return JSON.stringify({error: e.toString(), stack: e.stack, debugMsgs: ['Error: ' + e]}); " +
            "  } " +
            "})();";

        Log.d(TAG, "Executing JS measurement (async, non-blocking)...");
        
        // Сохраняем существующий WebChromeClient, если есть
        android.webkit.WebChromeClient existingClient = webView.getWebChromeClient();
        if (existingClient == null) {
            Log.d(TAG, "Setting WebChromeClient for logging");
            webView.setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                    Log.d(TAG, "WebView JS: " + consoleMessage.message() + 
                          " -- From line " + consoleMessage.lineNumber() + 
                          " of " + consoleMessage.sourceId());
                    return true;
                }
            });
            } else {
            Log.d(TAG, "WebChromeClient already set, using existing one");
        }

        // Выполняем JS асинхронно - callback придет когда будет готово
        webView.evaluateJavascript(measureJs, value -> {
            long elapsed = System.currentTimeMillis() - startTime;
            
            // Проверяем, не устарел ли callback
            if (expired.get()) {
                Log.w(TAG, "JS callback received but measurement already expired (elapsed: " + elapsed + "ms), ignoring");
                return;
            }
            
            // Отменяем таймаут, так как получили результат
            mainHandler.removeCallbacks(timeoutRunnable);
            
            Log.d(TAG, "JS callback received after " + elapsed + "ms, value=" + 
                  (value != null ? (value.length() > 100 ? value.substring(0, 100) + "..." : value) : "null"));
            
            if (value == null || value.equals("null") || value.isEmpty()) {
                Log.e(TAG, "JS returned null or empty after " + elapsed + "ms");
                expired.set(true);
                // Используем fallback - WebView.getContentHeight() возвращает CSS пиксели
                int fallbackHeight = webView.getContentHeight();
                int fallbackWidth = webView.getWidth();
                float density = webView.getContext().getResources().getDisplayMetrics().density;
                int cssWidth = (int) (fallbackWidth / density);
                int cssHeight = fallbackHeight; // Уже в CSS пикселях
                
                JSONObject fallbackDebug = new JSONObject();
                try {
                    fallbackDebug.put("fallback", true);
                    fallbackDebug.put("reason", "JS returned null");
                    fallbackDebug.put("elapsedMs", elapsed);
                    fallbackDebug.put("webViewHeight", fallbackHeight);
                    fallbackDebug.put("cssHeight", cssHeight);
                } catch (Exception e) {}
                // Fallback должен возвращать error, но с данными для экспорта
                callback.onResult(MeasurementResult.error(
                    "JS returned null, using fallback",
                    cssHeight,
                    fallbackDebug
                ));
                return;
            }
            
            // Обрабатываем результат
            String jsResult = value;
            // Убираем кавычки и экранирование
            if (jsResult.startsWith("\"") && jsResult.endsWith("\"")) {
                jsResult = jsResult.substring(1, jsResult.length() - 1);
                jsResult = jsResult.replace("\\\"", "\"")
                                  .replace("\\\\", "\\")
                                  .replace("\\n", "\n")
                                  .replace("\\r", "\r")
                                  .replace("\\t", "\t");
            }
            
            try {
                JSONObject json = new JSONObject(jsResult);

                if (json.has("error")) {
                    String errorMsg = json.getString("error");
                    Log.e(TAG, "JS returned error after " + elapsed + "ms: " + errorMsg);
                    expired.set(true);
                    callback.onResult(MeasurementResult.error(errorMsg, webView.getContentHeight(),
                            json.has("debug") ? json.getJSONObject("debug") : null));
                    return;
                }

                int contentHeightCss = (int) Math.round(json.getDouble("cssHeight"));
                int contentWidthCss = (int) Math.round(json.getDouble("cssWidth"));
                Log.d(TAG, "JS measurement SUCCESS after " + elapsed + "ms: cssHeight=" + contentHeightCss + " cssWidth=" + contentWidthCss);

                // Парсим debug info
                if (json.has("debug")) {
                    JSONObject debug = json.getJSONObject("debug");
                    if (debug.has("debugMsgs")) {
                        JSONArray msgs = debug.getJSONArray("debugMsgs");
                        Log.d(TAG, "Debug messages from JS (" + msgs.length() + "):");
                        for (int i = 0; i < msgs.length(); i++) {
                            Log.d(TAG, "  [" + i + "] " + msgs.getString(i));
                        }
                    }
                    if (debug.has("componentCount")) {
                        Log.d(TAG, "Components found: " + debug.getInt("componentCount"));
                    }
                    if (debug.has("sectionHeaderCount")) {
                        Log.d(TAG, "Section headers found: " + debug.getInt("sectionHeaderCount"));
                    }
                }

                // Парсим границы секций
                List<PdfBoundaryModels.SectionHeaderBoundary> sectionHeaderBoundaries = new ArrayList<>();
                if (json.has("sectionHeaderBounds")) {
                    JSONArray boundsArray = json.getJSONArray("sectionHeaderBounds");
                    Log.d(TAG, "Parsing " + boundsArray.length() + " section header boundaries...");
                    for (int i = 0; i < boundsArray.length(); i++) {
                        JSONObject bound = boundsArray.getJSONObject(i);
                        sectionHeaderBoundaries.add(new PdfBoundaryModels.SectionHeaderBoundary(
                            bound.getInt("top"), bound.getInt("bottom")));
                    }
                }

                // Парсим границы компонентов
                List<PdfBoundaryModels.ComponentBoundary> componentBoundaries = new ArrayList<>();
                if (json.has("componentBounds")) {
                    JSONArray boundsArray = json.getJSONArray("componentBounds");
                    Log.d(TAG, "Parsing " + boundsArray.length() + " components...");
                    for (int i = 0; i < boundsArray.length(); i++) {
                        JSONObject bound = boundsArray.getJSONObject(i);
                        componentBoundaries.add(new PdfBoundaryModels.ComponentBoundary(
                            bound.getInt("top"),
                            bound.getInt("bottom"),
                            bound.has("headerTop") ? bound.getInt("headerTop") : bound.getInt("top"),
                            bound.has("headerBottom") ? bound.getInt("headerBottom") : bound.getInt("top"),
                            bound.has("fieldsTop") ? bound.getInt("fieldsTop") : bound.getInt("top"),
                            bound.has("fieldsBottom") ? bound.getInt("fieldsBottom") : bound.getInt("bottom")
                        ));
                    }
                }

                // Парсим границы подписи
                PdfBoundaryModels.SignatureBoundary signatureBoundary = null;
                if (json.has("signatureBounds")) {
                    JSONObject signBounds = json.getJSONObject("signatureBounds");
                    signatureBoundary = new PdfBoundaryModels.SignatureBoundary(
                        signBounds.getInt("top"), signBounds.getInt("bottom"));
                    Log.d(TAG, "Parsed signature bounds: " + signatureBoundary.toString());
                }

                expired.set(true);
                callback.onResult(MeasurementResult.success(contentHeightCss, contentWidthCss,
                        componentBoundaries, sectionHeaderBoundaries, signatureBoundary,
                        json.has("debug") ? json.getJSONObject("debug") : null));
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse JS result after " + elapsed + "ms", e);
                expired.set(true);
                callback.onResult(MeasurementResult.error("Failed to parse JS result: " + e.getMessage(), 
                    webView.getContentHeight(), null));
            }
        });
    }
}