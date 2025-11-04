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
            val container = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(a4WidthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
                visibility = android.view.View.INVISIBLE // Невидимый, но с правильными размерами
                addView(webView, ViewGroup.LayoutParams(a4WidthPx, ViewGroup.LayoutParams.WRAP_CONTENT))
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
            container.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(a4WidthPx, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(a4HeightPx, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            container.layout(0, 0, container.measuredWidth, container.measuredHeight)
            
            webView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(a4WidthPx, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(a4HeightPx, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            webView.layout(0, 0, webView.measuredWidth, webView.measuredHeight)

            try {
                // Загружаем HTML
                Log.d("PDF", "Starting HTML load, HTML length: ${html.length}")
                awaitWebViewLoaded(webView, baseUrl, html)
                
                // Даем время WebView для полного рендеринга контента
                // Проверяем, что контент действительно загружен
                var attempts = 0
                while (attempts < 10 && webView.contentHeight == 0) {
                    delay(200)
                    attempts++
                    Log.d("PDF", "Waiting for contentHeight, attempt $attempts, contentHeight: ${webView.contentHeight}")
                }
                
                // Дополнительная задержка для завершения рендеринга
                delay(1500)
                
                // Проверяем, что контент действительно есть
                val finalContentHeight = webView.contentHeight
                Log.d("PDF", "Final contentHeight: $finalContentHeight, WebView width: ${webView.width}, height: ${webView.height}")
                
                if (finalContentHeight == 0) {
                    Log.w("PDF", "Warning: WebView contentHeight is 0 after loading")
                }

                val attrs = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                Log.d("PDF", "Starting PDF export")
                // Call exporter and suspend until callback с таймаутом
                withTimeout(20000) { // Таймаут 20 секунд для экспорта
                    suspendCancellableCoroutine<Unit> { cont ->
                        WebViewPdfExporter.export(
                            webView,
                            attrs,
                            outFile,
                            object : WebViewPdfExporter.Callback {
                                override fun onSuccess() {
                                    Log.d("PDF", "PDF export successful")
                                    if (cont.isActive) cont.resume(Unit)
                                }

                                override fun onError(t: Throwable) {
                                    Log.e("PDF", "PDF export error", t)
                                    if (cont.isActive) cont.resumeWithException(t)
                                }
                            }
                        )

                        // Optional: if coroutine cancelled, there's no cancel in exporter — destroy webview anyway.
                        cont.invokeOnCancellation {
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