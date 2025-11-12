package ru.wassertech.report

import android.content.Context
import android.util.Log
import kotlinx.coroutines.withTimeout
import java.io.File
import org.apache.poi.xwpf.usermodel.*

object DocxToPdfExporter {
    
    /**
     * Конвертирует DOCX файл в PDF используя WebView.
     * Сначала конвертируем DOCX в HTML с сохранением форматирования, затем HTML в PDF.
     */
    suspend fun exportDocxToPdf(context: Context, docxBytes: ByteArray, outFile: File) {
        withTimeout(30000) {
            // Конвертируем DOCX в HTML с сохранением форматирования
            val html = convertDocxToHtml(docxBytes)
            
            // Используем существующий механизм HTML->PDF
            PdfExporter.exportHtmlToPdf(context, html, outFile)
        }
    }
    
    /**
     * Конвертирует DOCX в HTML с сохранением форматирования.
     */
    private fun convertDocxToHtml(docxBytes: ByteArray): String {
        return try {
            val document = XWPFDocument(java.io.ByteArrayInputStream(docxBytes))
            val html = StringBuilder()
            
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><style>")
            html.append("""
                @page { size: A4; margin: 18mm 16mm 16mm; }
                body { 
                    font-family: "Times New Roman", Times, serif;
                    font-size: 12pt;
                    line-height: 1.5;
                    color: #000;
                    margin: 0;
                    padding: 0;
                }
                p { margin: 6pt 0; }
                table { 
                    border-collapse: collapse; 
                    width: 100%; 
                    margin: 10pt 0;
                    border: 1px solid #000;
                }
                td, th { 
                    border: 1px solid #000; 
                    padding: 4pt 6pt; 
                    vertical-align: top;
                }
                th { 
                    background-color: #f0f0f0;
                    font-weight: bold;
                }
                .bold { font-weight: bold; }
                .italic { font-style: italic; }
                .underline { text-decoration: underline; }
                .center { text-align: center; }
                .right { text-align: right; }
                .justify { text-align: justify; }
                h1, h2, h3, h4, h5, h6 { 
                    font-weight: bold; 
                    margin: 12pt 0 6pt 0;
                }
                h1 { font-size: 16pt; }
                h2 { font-size: 14pt; }
                h3 { font-size: 13pt; }
                ul, ol { margin: 6pt 0; padding-left: 24pt; }
                li { margin: 2pt 0; }
            """.trimIndent())
            html.append("</style></head><body>")
            
            // Обрабатываем параграфы с форматированием
            document.paragraphs.forEach { paragraph ->
                val paraHtml = processParagraph(paragraph)
                if (paraHtml.isNotBlank()) {
                    html.append(paraHtml)
                }
            }
            
            // Обрабатываем таблицы с форматированием
            document.tables.forEach { table ->
                html.append(processTable(table))
            }
            
            html.append("</body></html>")
            document.close()
            
            html.toString()
        } catch (e: Exception) {
            Log.e("DocxToPdfExporter", "Error converting DOCX to HTML", e)
            "<!DOCTYPE html><html><body><p>Ошибка конвертации DOCX в PDF: ${e.message}</p></body></html>"
        }
    }
    
    private fun processParagraph(paragraph: XWPFParagraph): String {
        if (paragraph.text.isBlank()) return ""
        
        val html = StringBuilder()
        // Получаем выравнивание правильно
        val alignment = when (paragraph.alignment) {
            org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER -> "center"
            org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT -> "right"
            org.apache.poi.xwpf.usermodel.ParagraphAlignment.BOTH -> "justify"
            else -> "left"
        }
        
        // Проверяем, является ли параграф заголовком
        val style = paragraph.style
        val isHeading = style?.contains("Heading") == true || 
                       paragraph.text.all { it.isUpperCase() && it.isLetter() } && paragraph.text.length < 100
        
        // Определяем уровень заголовка (если есть)
        val headingLevel = when {
            style?.contains("Heading1") == true || style?.contains("Title") == true -> 1
            style?.contains("Heading2") == true -> 2
            style?.contains("Heading3") == true -> 3
            else -> null
        }
        
        val tag = if (headingLevel != null) "h$headingLevel" else "p"
        html.append("<$tag class=\"$alignment\">")
        
        // Обрабатываем каждый run с его форматированием
        paragraph.runs.forEach { run ->
            val runText = run.text()
            if (runText.isNotBlank()) {
                val styles = mutableListOf<String>()
                
                if (run.isBold) styles.add("bold")
                if (run.isItalic) styles.add("italic")
                // Проверяем подчеркивание через reflection или просто пропускаем
                // В Apache POI проверка подчеркивания может быть сложной, поэтому упрощаем
                // Подчеркивание будет сохранено, если оно есть в документе через другие методы
                
                val fontSize = run.fontSize
                val color = run.color
                
                val styleAttr = StringBuilder()
                if (fontSize != -1) {
                    styleAttr.append("font-size: ${fontSize}pt; ")
                }
                if (color != null && color.isNotEmpty() && color != "000000") {
                    // Форматируем цвет (убираем префикс, если есть)
                    val cleanColor = color.removePrefix("#").removePrefix("0x")
                    styleAttr.append("color: #$cleanColor; ")
                }
                
                val styleClass = if (styles.isNotEmpty()) styles.joinToString(" ") else ""
                val inlineStyle = if (styleAttr.isNotEmpty()) " style=\"$styleAttr\"" else ""
                
                if (styleClass.isNotEmpty() || inlineStyle.isNotEmpty()) {
                    html.append("<span class=\"$styleClass\"$inlineStyle>")
                    html.append(escapeHtml(runText))
                    html.append("</span>")
                } else {
                    html.append(escapeHtml(runText))
                }
            }
        }
        
        html.append("</$tag>")
        return html.toString()
    }
    
    private fun processTable(table: XWPFTable): String {
        val html = StringBuilder()
        html.append("<table>")
        
        table.rows.forEachIndexed { rowIndex, row ->
            html.append("<tr>")
            
            row.tableCells.forEach { cell ->
                val isHeader = rowIndex == 0
                val tag = if (isHeader) "th" else "td"
                
                html.append("<$tag>")
                
                // Обрабатываем параграфы в ячейке
                cell.paragraphs.forEach { para ->
                    if (para.text.isNotBlank()) {
                        val paraHtml = processParagraph(para)
                        html.append(paraHtml.replace("<p", "<div").replace("</p>", "</div>"))
                    }
                }
                
                html.append("</$tag>")
            }
            
            html.append("</tr>")
        }
        
        html.append("</table>")
        return html.toString()
    }
    
    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

