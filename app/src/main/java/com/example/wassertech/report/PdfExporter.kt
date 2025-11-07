package com.example.wassertech.report

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.webkit.WebView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PdfExporter {

    /**
     * Renders provided HTML into a PDF file using a WebView and the headless WebViewPdfExporter.
     *
     * Note: webView will be created and destroyed inside this call. Call from a coroutine (Dispatcher.Main
     * if you want WebView on UI thread is recommended, but WebViewPdfExporter posts to main internally).
     */
    suspend fun exportHtmlToPdf(context: Context, html: String, outFile: File) {
        withTimeout(30000) { // Таймаут 30 секунд
            // Получаем Activity для добавления WebView в иерархию View
            val activity = context as? Activity
            if (activity == null) {
                throw IllegalStateException("Context должен быть Activity для создания WebView")
            }
            
            // WebView должен быть создан на Main потоке
            val webView = WebView(context)
            val baseUrl = "file:///android_asset/"
            
            // Вычисляем размеры A4 в пикселях (210mm x 297mm при 300 DPI)
            val densityDpi = context.resources.displayMetrics.densityDpi
            val a4WidthMm = 210
            val a4HeightMm = 297
            val a4WidthPx = (a4WidthMm * densityDpi / 25.4f).toInt()
            val a4HeightPx = (a4HeightMm * densityDpi / 25.4f).toInt()
            
            // Создаем невидимый контейнер и добавляем его в Activity
            // WebView не должен иметь огромную высоту - это вызывает OOM
            // WebViewPdfExporter использует contentHeight для расчета страниц и рендерит по частям
            // Достаточно установить разумную высоту (несколько страниц), чтобы contentHeight вычислялся правильно
            val container = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(a4WidthPx, a4HeightPx * 3) // Достаточно для вычисления contentHeight
                visibility = android.view.View.INVISIBLE // Невидимый, но с правильными размерами
                addView(webView, ViewGroup.LayoutParams(a4WidthPx, a4HeightPx * 3))
            }
            
            // Добавляем контейнер в Activity (в корневой ViewGroup)
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            rootView.addView(container)
            
            // Настраиваем WebView
            webView.settings.apply {
                javaScriptEnabled = false
                domStorageEnabled = false
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(false)
                builtInZoomControls = false
            }
            
            // Измеряем контейнер и WebView с правильными размерами
            // Используем UNSPECIFIED для высоты, чтобы WebView мог определить свою высоту
            container.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(a4WidthPx, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            container.layout(0, 0, a4WidthPx, container.measuredHeight)
            
            webView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(a4WidthPx, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            webView.layout(0, 0, a4WidthPx, webView.measuredHeight)

            try {
                // Загружаем HTML
                Log.d("PDF", "Starting HTML load, HTML length: ${html.length}")
                awaitWebViewLoaded(webView, baseUrl, html)
                
                // Даем время WebView для полного рендеринга контента
                // Проверяем, что контент действительно загружен и стабилизировался
                var attempts = 0
                var lastContentHeight = 0
                var stableCount = 0
                
                while (attempts < 30) {
                    delay(300)
                    attempts++
                    val currentHeight = webView.contentHeight
                    Log.d("PDF", "Waiting for contentHeight, attempt $attempts, contentHeight: $currentHeight")
                    
                    if (currentHeight > 0) {
                        if (currentHeight == lastContentHeight) {
                            stableCount++
                            if (stableCount >= 3) {
                                Log.d("PDF", "Content height stabilized at $currentHeight")
                                break
                            }
                        } else {
                            stableCount = 0
                        }
                        lastContentHeight = currentHeight
                    }
                }
                
                // Дополнительная задержка для завершения рендеринга
                delay(2000)
                
                // Пересчитываем layout после загрузки контента
                webView.post {
                    webView.requestLayout()
                }
                delay(1000)
                
                // Принудительно пересчитываем размеры WebView на основе contentHeight
                val finalContentHeight = webView.contentHeight
                val density = context.resources.displayMetrics.density
                val contentHeightPx = (finalContentHeight * density).toInt()
                
                Log.d("PDF", "Final contentHeight (CSS): $finalContentHeight, contentHeightPx: $contentHeightPx")
                Log.d("PDF", "WebView width: ${webView.width}, height: ${webView.height}, measuredHeight: ${webView.measuredHeight}")
                
                if (finalContentHeight == 0) {
                    Log.w("PDF", "Warning: WebView contentHeight is 0 after loading")
                } else {
                    // НЕ устанавливаем огромную высоту WebView - это вызывает OOM!
                    // WebViewPdfExporter использует contentHeight для расчета страниц и рендерит их по частям через Canvas
                    // Достаточно убедиться, что WebView имеет правильную ширину и contentHeight вычислен
                    // WebView может иметь небольшую физическую высоту, но contentHeight будет правильным
                    Log.d("PDF", "ContentHeight ready: $finalContentHeight (CSS), $contentHeightPx (device px)")
                    Log.d("PDF", "WebView dimensions: width=${webView.width}, height=${webView.height}, contentHeight=${webView.contentHeight}")
                    
                    // Убеждаемся, что WebView имеет правильную ширину для рендеринга
                    if (webView.width != a4WidthPx) {
                        val webViewParams = webView.layoutParams as? FrameLayout.LayoutParams
                            ?: FrameLayout.LayoutParams(a4WidthPx, a4HeightPx * 3)
                        webViewParams.width = a4WidthPx
                        webView.layoutParams = webViewParams
                        webView.measure(
                            android.view.View.MeasureSpec.makeMeasureSpec(a4WidthPx, android.view.View.MeasureSpec.EXACTLY),
                            android.view.View.MeasureSpec.makeMeasureSpec(a4HeightPx * 3, android.view.View.MeasureSpec.AT_MOST)
                        )
                        webView.layout(0, 0, a4WidthPx, webView.measuredHeight)
                        delay(500)
                    }
                }

                val attrs = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                Log.d("PDF", "Starting PDF export, contentHeight: ${webView.contentHeight}, width: ${webView.width}")
                
                // Убеждаемся, что мы на главном потоке
                if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                    throw IllegalStateException("PDF export must be called on main thread")
                }
                
                // Call exporter and suspend until callback с таймаутом
                withTimeout(60000) { // Увеличен таймаут до 60 секунд для экспорта
                    suspendCancellableCoroutine<Unit> { cont ->
                        Log.d("PDF", "Calling WebViewPdfExporter.export()")
                        
                        try {
                            WebViewPdfExporter.export(
                                webView,
                                attrs,
                                outFile,
                                object : WebViewPdfExporter.Callback {
                                    override fun onSuccess() {
                                        Log.d("PDF", "PDF export successful, file size: ${outFile.length()} bytes")
                                        if (cont.isActive) {
                                            cont.resume(Unit)
                                        } else {
                                            Log.w("PDF", "Coroutine is not active, cannot resume")
                                        }
                                    }

                                    override fun onError(t: Throwable) {
                                        Log.e("PDF", "PDF export error", t)
                                        if (cont.isActive) {
                                            cont.resumeWithException(t)
                                        } else {
                                            Log.w("PDF", "Coroutine is not active, cannot resume with exception")
                                        }
                                    }
                                }
                            )
                            Log.d("PDF", "WebViewPdfExporter.export() called, waiting for callback")
                        } catch (e: Exception) {
                            Log.e("PDF", "Exception calling WebViewPdfExporter.export()", e)
                            if (cont.isActive) {
                                cont.resumeWithException(e)
                            }
                        }

                        // Optional: if coroutine cancelled, there's no cancel in exporter — destroy webview anyway.
                        cont.invokeOnCancellation {
                            Log.w("PDF", "PDF export coroutine cancelled")
                            try { webView.stopLoading() } catch (_: Exception) {}
                        }
                    }
                }
                Log.d("PDF", "PDF export completed")
            } finally {
                try {
                    webView.stopLoading()
                    // Удаляем контейнер из Activity
                    rootView.removeView(container)
                    webView.destroy()
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun awaitWebViewLoaded(webView: WebView, baseUrl: String, html: String) {
        // Упрощенный подход: просто загружаем и ждем фиксированное время
        // WebView может не вызывать onPageFinished для data URLs
        suspendCancellableCoroutine<Unit> { cont ->
            var isResumed = false
            
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.postDelayed({
                        if (cont.isActive && !isResumed) {
                            isResumed = true
                            cont.resume(Unit)
                        }
                    }, 800) // Увеличена задержка для рендеринга
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    // Не считаем ошибку критичной - продолжаем
                    if (cont.isActive && !isResumed) {
                        isResumed = true
                        cont.resume(Unit)
                    }
                }
            }
            
            // Загружаем HTML
            try {
                webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null)
            } catch (e: Exception) {
                // Если не получилось с baseUrl, пробуем без него
                webView.loadData(html, "text/html", "utf-8")
            }
            
            // Fallback: если onPageFinished не вызывается, продолжаем через 3 секунды
            webView.postDelayed({
                if (cont.isActive && !isResumed) {
                    isResumed = true
                    cont.resume(Unit)
                }
            }, 3000)

            cont.invokeOnCancellation {
                try { webView.stopLoading() } catch (_: Exception) {}
            }
        }
    }
}