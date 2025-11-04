package com.example.wassertech.report

import android.content.Context
import android.print.PrintAttributes
import android.webkit.WebView
import kotlinx.coroutines.suspendCancellableCoroutine
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
        val webView = WebView(context)
        val baseUrl = "file:///android_asset/"

        try {
            // wait for page load
            awaitWebViewLoaded(webView, baseUrl, html)

            val attrs = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            // Call exporter and suspend until callback
            suspendCancellableCoroutine<Unit> { cont ->
                WebViewPdfExporter.export(
                    webView,
                    attrs,
                    outFile,
                    object : WebViewPdfExporter.Callback {
                        override fun onSuccess() {
                            if (cont.isActive) cont.resume(Unit)
                        }

                        override fun onError(t: Throwable) {
                            if (cont.isActive) cont.resumeWithException(t)
                        }
                    }
                )

                // Optional: if coroutine cancelled, there's no cancel in exporter — destroy webview anyway.
                cont.invokeOnCancellation {
                    try { webView.stopLoading() } catch (_: Exception) {}
                }
            }
        } finally {
            try { webView.destroy() } catch (_: Exception) {}
        }
    }

    private suspend fun awaitWebViewLoaded(webView: WebView, baseUrl: String, html: String) {
        suspendCancellableCoroutine<Unit> { cont ->
            webView.settings.javaScriptEnabled = false
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    if (cont.isActive) cont.resumeWithException(RuntimeException("WebView load error: $description"))
                }
            }
            webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null)

            cont.invokeOnCancellation {
                try { webView.stopLoading() } catch (_: Exception) {}
            }
        }
    }
}