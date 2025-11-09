package com.example.wassertech.report;

import android.graphics.pdf.PdfDocument;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
     * Export with page footer information.
     * Now includes debug of PdfContentMeasurer before main export.
     *
     * ВАЖНО: webView должен быть уже загружен! Вызывайте export() после загрузки HTML.
     */
    public static void export(final WebView webView,
                              final PrintAttributes attrs,
                              final File outFile,
                              final String reportNumber,
                              final Callback cb) {
        Log.d(TAG, "export() called, isMainThread: " + (Looper.myLooper() == Looper.getMainLooper()));

        // Если WebView уже загружен, выполняем экспорт напрямую
        // Иначе устанавливаем WebViewClient для ожидания onPageFinished
        if (webView.getProgress() == 100) {
            // WebView уже загружен, выполняем экспорт
            Log.d(TAG, "WebView already loaded, performing export directly");
            performExportAsync(webView, attrs, outFile, reportNumber, cb);
        } else {
            // Устанавливаем WebViewClient для ожидания загрузки
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d(TAG, "WebView onPageFinished! HTML полностью загружен: " + url);
                    performExportAsync(webView, attrs, outFile, reportNumber, cb);
                }
            });
        }
    }

    private static void performExportAsync(final WebView webView,
                                          final PrintAttributes attrs,
                                          final File outFile,
                                          final String reportNumber,
                                          final Callback cb) {
        // Ждём стабилизации рендера и проверяем готовность WebView
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                // Проверяем, что WebView все еще жив и готов
                if (webView == null) {
                    Log.e(TAG, "WebView is null!");
                    cb.onError(new IllegalStateException("WebView is null"));
                    return;
                }
                
                // Проверяем прогресс загрузки
                int progress = webView.getProgress();
                Log.d(TAG, "WebView progress before measurement: " + progress);
                
                if (progress < 100) {
                    Log.w(TAG, "WebView not fully loaded (progress: " + progress + "), waiting more...");
                    // Даём еще время для загрузки
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        performMeasurementAndExport(webView, attrs, outFile, reportNumber, cb);
                    }, 2000);
                    return;
                }
                
                performMeasurementAndExport(webView, attrs, outFile, reportNumber, cb);
            } catch (Throwable t) {
                Log.e(TAG, "PDF export failed", t);
                cb.onError(t);
            }
        }, 2000); // Увеличена задержка до 2 секунд для стабилизации рендера
    }
    
    private static void performMeasurementAndExport(WebView webView, PrintAttributes attrs, 
                                                   File outFile, String reportNumber, Callback cb) {
        final long measurementStartTime = System.currentTimeMillis();
        
        Log.d(TAG, "Запуск асинхронного замера PdfContentMeasurer.measureContentAsync...");
        
        // Используем асинхронный метод с callback
        PdfContentMeasurer.measureContentAsync(webView, new PdfContentMeasurer.MeasurementCallback() {
            @Override
            public void onResult(PdfContentMeasurer.MeasurementResult result) {
                long measurementElapsed = System.currentTimeMillis() - measurementStartTime;
                Log.d(TAG, "Measurement completed in " + measurementElapsed + "ms");
                
                try {
                    if (result.hasError) {
                        Log.e(TAG, "Ошибка замера: " + result.errorMessage);
                        if (result.debugJson != null) {
                            try {
                                Log.e(TAG, "Debug JS info: " + result.debugJson.toString());
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to log debug info", e);
                            }
                        }
                        // Даже при ошибке пытаемся экспортировать с fallback данными
                        Log.w(TAG, "Measurement had errors, but continuing with fallback data...");
                    }

                    Log.d(TAG, "Замер завершён: Высота CSS = " + result.contentHeightCss +
                            ", Ширина CSS = " + result.contentWidthCss);
                    Log.d(TAG, "Секции: " + result.sectionHeaderBoundaries.size() +
                            ", Компоненты: " + result.componentBoundaries.size());
                    
                    if (result.contentHeightCss <= 0) {
                        Log.e(TAG, "Invalid content height: " + result.contentHeightCss);
                        cb.onError(new IllegalStateException("Invalid content height: " + result.contentHeightCss));
                        return;
                    }

                    // Выполняем экспорт (на главном потоке, так как мы в callback)
                    try {
                        long exportStartTime = System.currentTimeMillis();
                        performExport(webView, attrs, outFile, reportNumber, result);
                        long exportElapsed = System.currentTimeMillis() - exportStartTime;
                        Log.d(TAG, "performExport() завершён успешно за " + exportElapsed + "ms");
                        Log.d(TAG, "Total PDF export time: " + (System.currentTimeMillis() - measurementStartTime) + "ms");
                        cb.onSuccess();
                    } catch (IOException e) {
                        Log.e(TAG, "PDF export IO error", e);
                        cb.onError(e);
                    } catch (Throwable t) {
                        Log.e(TAG, "PDF export failed", t);
                        cb.onError(t);
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Error processing measurement result", t);
                    cb.onError(t);
                }
            }
        }, 10000); // Таймаут 10 секунд
    }

    private static void performExport(WebView webView, PrintAttributes attrs, File outFile, String reportNumber,
                                     PdfContentMeasurer.MeasurementResult measurementResult) throws IOException {
        Log.d(TAG, "Starting performExport, contentHeight: " + measurementResult.contentHeightCss + " CSS px");

        // Получаем размеры страницы из PrintAttributes
        int pageWidthPx = (int) (attrs.getMediaSize().getWidthMils() / 1000f * 72f); // Convert mils to points, then to pixels
        int pageHeightPx = (int) (attrs.getMediaSize().getHeightMils() / 1000f * 72f);
        
        // Используем фиксированные размеры A4 в CSS пикселях
        float pageHeightCss = A4_HEIGHT_CSS;
        float pageWidthCss = A4_WIDTH_CSS;
        
        // Вычисляем границы страниц
        List<Float> pageBoundaries = PdfPageBoundaryCalculator.calculatePageBoundaries(
                measurementResult.contentHeightCss,
                pageHeightCss,
                measurementResult.componentBoundaries,
                measurementResult.sectionHeaderBoundaries
        );

        // Получаем density для конвертации CSS пикселей в device пиксели
        float density = webView.getContext().getResources().getDisplayMetrics().density;
        int pageWidthDevicePx = (int) (pageWidthCss * density);
        int pageHeightDevicePx = (int) (pageHeightCss * density);

        Log.d(TAG, "Page dimensions: CSS=" + pageWidthCss + "x" + pageHeightCss + 
              ", Device=" + pageWidthDevicePx + "x" + pageHeightDevicePx + 
              ", density=" + density);
        Log.d(TAG, "Page boundaries count: " + pageBoundaries.size());

        // Создаём PDF документ
        PdfDocument pdfDocument = new PdfDocument();
        
        // Рендерим каждую страницу
        for (int pageIndex = 0; pageIndex < pageBoundaries.size() - 1; pageIndex++) {
            float pageStartCss = pageBoundaries.get(pageIndex);
            float pageEndCss = pageBoundaries.get(pageIndex + 1);
            float pageHeightCssActual = pageEndCss - pageStartCss;
            
            // Конвертируем в device пиксели
            int pageStartDevicePx = (int) (pageStartCss * density);
            int pageHeightDevicePxActual = (int) (pageHeightCssActual * density);
            
            Log.d(TAG, "Rendering page " + (pageIndex + 1) + 
                  ": CSS range [" + pageStartCss + "-" + pageEndCss + "], " +
                  "Device range [" + pageStartDevicePx + "-" + (pageStartDevicePx + pageHeightDevicePxActual) + "]");

            // Создаём страницу PDF
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidthDevicePx, pageHeightDevicePxActual, pageIndex + 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            
            // Получаем Canvas для рисования
            android.graphics.Canvas canvas = page.getCanvas();
            
            // Создаём Bitmap для рендеринга части WebView
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                    pageWidthDevicePx, 
                    pageHeightDevicePxActual, 
                    android.graphics.Bitmap.Config.ARGB_8888
            );
            android.graphics.Canvas bitmapCanvas = new android.graphics.Canvas(bitmap);
            
            // Смещаем Canvas вверх, чтобы показать нужную часть WebView
            bitmapCanvas.translate(0, -pageStartDevicePx);
            
            // Рисуем WebView на Bitmap Canvas
            webView.draw(bitmapCanvas);
            
            // Рисуем Bitmap на PDF Canvas
            canvas.drawBitmap(bitmap, 0, 0, null);
            
            // Освобождаем память
            bitmap.recycle();
            
            // Debug линии отключены (убраны синие и зеленые линии)
            // PdfDebugDrawer.drawDebugLines(...) - закомментировано
            
            // Добавляем надпись "Продолжение отчёта смотри на странице Nx..." 
            // если есть большое пустое пространство внизу страницы (не на последней странице)
            if (pageIndex < pageBoundaries.size() - 2) { // Не последняя страница
                // Находим последний элемент на текущей странице
                float lastElementEndCss = pageStartCss;
                
                // Проверяем компоненты на текущей странице
                for (PdfBoundaryModels.ComponentBoundary comp : measurementResult.componentBoundaries) {
                    if (comp.topCss >= pageStartCss && comp.topCss < pageEndCss) {
                        // Компонент попадает на эту страницу
                        if (comp.bottomCss > lastElementEndCss) {
                            lastElementEndCss = comp.bottomCss;
                        }
                    }
                }
                
                // Проверяем заголовки секций на текущей странице
                for (PdfBoundaryModels.SectionHeaderBoundary header : measurementResult.sectionHeaderBoundaries) {
                    if (header.topCss >= pageStartCss && header.topCss < pageEndCss) {
                        // Заголовок попадает на эту страницу
                        if (header.bottomCss > lastElementEndCss) {
                            lastElementEndCss = header.bottomCss;
                        }
                    }
                }
                
                // Вычисляем пустое пространство внизу страницы (в CSS пикселях)
                float emptySpaceCss = pageEndCss - lastElementEndCss;
                
                // Минимальный размер пустого пространства для вставки надписи: 80px CSS (примерно 20mm)
                // И максимальный размер: если пространство слишком большое (больше 200px), точно вставляем
                float minEmptySpaceCss = 80f; // Минимум для вставки надписи
                float maxEmptySpaceCss = 200f; // Если больше этого, точно вставляем
                
                if (emptySpaceCss >= minEmptySpaceCss) {
                    // Конвертируем позицию последнего элемента в device пиксели относительно начала страницы
                    // (на canvas координаты начинаются с 0 для каждой страницы)
                    float lastElementEndDevicePx = (lastElementEndCss - pageStartCss) * density;
                    
                    // Вычисляем позицию для надписи: немного выше центра пустого пространства
                    float continuationTextY = lastElementEndDevicePx + (emptySpaceCss * density) * 0.4f;
                    
                    // Проверяем, что надпись влезает на страницу
                    if (continuationTextY < pageHeightDevicePxActual - 20) {
                        // Рисуем надпись
                        android.graphics.Paint textPaint = new android.graphics.Paint();
                        textPaint.setColor(0xFF6B7280); // Серый цвет (--muted)
                        textPaint.setTextSize(28f * density / 2.625f); // 10pt в device пикселях (примерно)
                        textPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
                        textPaint.setAntiAlias(true);
                        textPaint.setStyle(android.graphics.Paint.Style.FILL);
                        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL));
                        
                        int nextPageNumber = pageIndex + 2; // Номер следующей страницы
                        String continuationText = "Продолжение отчёта смотри на странице " + nextPageNumber + "...";
                        
                        // Рисуем текст по центру страницы по горизонтали
                        canvas.drawText(continuationText, pageWidthDevicePx / 2f, continuationTextY, textPaint);
                        
                        Log.d(TAG, "Added continuation text on page " + (pageIndex + 1) + 
                              ": empty space=" + emptySpaceCss + " CSS px, text Y=" + continuationTextY + " device px");
                    }
                }
            }
            
            // Завершаем страницу
            pdfDocument.finishPage(page);
        }

        // Сохраняем PDF в файл
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            pdfDocument.writeTo(fos);
            Log.d(TAG, "PDF saved to: " + outFile.getAbsolutePath() + ", size: " + outFile.length() + " bytes");
        } finally {
            pdfDocument.close();
        }
    }
}