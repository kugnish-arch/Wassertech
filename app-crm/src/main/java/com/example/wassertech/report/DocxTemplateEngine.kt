package ru.wassertech.report

import android.content.Context
import ru.wassertech.report.model.ReportDTO
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableRow
import java.io.InputStream
import java.io.ByteArrayOutputStream

object DocxTemplateEngine {
    
    fun processTemplate(context: Context, templateAssetPath: String, dto: ReportDTO): ByteArray {
        val templateStream = context.assets.open(templateAssetPath)
        val document = XWPFDocument(templateStream)
        
        try {
            // Обрабатываем параграфы
            document.paragraphs.forEach { paragraph ->
                processParagraph(paragraph, dto)
            }
            
            // Обрабатываем таблицы
            document.tables.forEach { table ->
                processTable(table, dto)
            }
            
            // Обрабатываем заголовки/футеры, если есть
            document.headerList.forEach { header ->
                header.paragraphs.forEach { paragraph ->
                    processParagraph(paragraph, dto)
                }
            }
            
            document.footerList.forEach { footer ->
                footer.paragraphs.forEach { paragraph ->
                    processParagraph(paragraph, dto)
                }
            }
            
            // Сохраняем в массив байтов
            val outputStream = ByteArrayOutputStream()
            document.write(outputStream)
            return outputStream.toByteArray()
        } finally {
            document.close()
            templateStream.close()
        }
    }
    
    private fun processParagraph(paragraph: XWPFParagraph, dto: ReportDTO) {
        val text = paragraph.text
        if (text.isBlank()) return
        
        var newText = text
        
        // Заменяем плейсхолдеры компании
        dto.companyConfig?.let { company ->
            newText = newText.replace("{{company.legal_name}}", company.legal_name)
            newText = newText.replace("{{company.inn}}", company.inn)
            newText = newText.replace("{{company.phone1}}", company.phone1)
            newText = newText.replace("{{company.phone2}}", company.phone2)
            newText = newText.replace("{{company.email}}", company.email)
            newText = newText.replace("{{company.website}}", company.website)
            newText = newText.replace("{{company.sign_name}}", company.sign_name)
            newText = newText.replace("{{company.sign_short}}", company.sign_short)
        }
        
        // Документ
        newText = newText.replace("{{doc.number}}", dto.reportNumber)
        newText = newText.replace("{{doc.date_rus}}", dto.reportDateRus)
        
        // Договор
        dto.contractConfig?.let { contract ->
            newText = newText.replace("{{contract.number}}", contract.number)
            newText = newText.replace("{{contract.date_rus}}", contract.date_rus)
        }
        
        // Клиент
        newText = newText.replace("{{client.name}}", dto.clientName)
        newText = newText.replace("{{client.sign_name}}", dto.clientSignName ?: "")
        
        // Объект и установка
        newText = newText.replace("{{site.name}}", dto.siteName ?: "")
        newText = newText.replace("{{installation.name}}", dto.installationName)
        
        // Комментарии
        newText = newText.replace("{{comments}}", dto.comments ?: "")
        
        // Заключение
        newText = newText.replace("{{conclusion}}", dto.conclusions ?: "")
        
        // Если текст изменился, заменяем его
        if (newText != text) {
            // Удаляем все существующие runs (обратным порядком, чтобы индексы не смещались)
            val runsCount = paragraph.runs.size
            for (i in runsCount - 1 downTo 0) {
                paragraph.removeRun(i)
            }
            // Создаем новый run с обновленным текстом
            paragraph.createRun().setText(newText)
        }
    }
    
    private fun processTable(table: XWPFTable, dto: ReportDTO) {
        table.rows.forEach { row ->
            row.tableCells.forEach { cell ->
                cell.paragraphs.forEach { paragraph ->
                    processParagraph(paragraph, dto)
                }
            }
        }
        
        // Обрабатываем блоки {{#works}} и {{#water}}
        // Это более сложная логика, которая требует поиска и замены строк в таблице
        // Пока оставляем простую замену в ячейках
    }
}

