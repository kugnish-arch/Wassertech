package com.example.wassertech.report

import android.content.Context
import com.example.wassertech.report.model.ReportDTO
import java.io.BufferedReader

object HtmlTemplateEngine {

    fun render(context: Context, templateAssetPath: String, dto: ReportDTO): String {
        val template = context.assets.open(templateAssetPath).bufferedReader().use(BufferedReader::readText)
        
        var html = template
        
        // Заменяем плейсхолдеры компании
        dto.companyConfig?.let { company ->
            html = html.replace("{{company.legal_name}}", escapeHtml(company.legal_name))
            html = html.replace("{{company.inn}}", escapeHtml(company.inn))
            html = html.replace("{{company.phone1}}", escapeHtml(company.phone1))
            html = html.replace("{{company.phone2}}", escapeHtml(company.phone2))
            html = html.replace("{{company.email}}", escapeHtml(company.email))
            html = html.replace("{{company.website}}", escapeHtml(company.website))
            html = html.replace("{{company.sign_name}}", escapeHtml(company.sign_name))
            html = html.replace("{{company.sign_short}}", escapeHtml(company.sign_short))
        }
        
        // Логотип как data URI
        dto.logoAssetPath?.let { path ->
            val logoDataUri = CompanyConfigLoader.logoToDataUri(context, path)
            html = html.replace("{{company.logo_data_uri}}", logoDataUri)
        }
        
        // Документ
        html = html.replace("{{doc.number}}", escapeHtml(dto.reportNumber))
        html = html.replace("{{doc.date_rus}}", escapeHtml(dto.reportDateRus))
        
        // Договор
        dto.contractConfig?.let { contract ->
            html = html.replace("{{contract.number}}", escapeHtml(contract.number))
            html = html.replace("{{contract.date_rus}}", escapeHtml(contract.date_rus))
        }
        
        // Клиент
        html = html.replace("{{client.name}}", escapeHtml(dto.clientName))
        html = html.replace("{{client.sign_name}}", escapeHtml(dto.clientSignName ?: ""))
        
        // Объект и установка
        html = html.replace("{{site.name}}", escapeHtml(dto.siteName ?: ""))
        html = html.replace("{{installation.name}}", escapeHtml(dto.installationName))
        
        // Выполненные работы
        val worksBlockRegex = Regex("\\{\\{#works\\}\\}([\\s\\S]*?)\\{\\{/works\\}\\}")
        val worksElseBlockRegex = Regex("\\{\\{^works\\}\\}([\\s\\S]*?)\\{\\{/works\\}\\}")
        
        if (dto.works.isNotEmpty()) {
            // Находим шаблон внутри блока {{#works}}...{{/works}}
            val worksTemplateMatch = worksBlockRegex.find(html)
            if (worksTemplateMatch != null) {
                val template = worksTemplateMatch.groupValues[1]
                // Заменяем {{.}} на реальные значения
                val worksHtml = dto.works.joinToString("") { work ->
                    template.replace("{{.}}", escapeHtml(work))
                }
                html = html.replace(worksBlockRegex, worksHtml)
            }
            // Удаляем блок {{^works}}...{{/works}}
            html = html.replace(worksElseBlockRegex, "")
        } else {
            // Удаляем блок {{#works}}...{{/works}}
            html = html.replace(worksBlockRegex, "")
            // Оставляем содержимое блока {{^works}}...{{/works}}
            html = html.replace(worksElseBlockRegex, "$1")
        }
        
        // Комментарии
        html = html.replace("{{comments}}", escapeHtml(dto.comments ?: ""))
        
        // Результаты анализов воды
        val waterBlockRegex = Regex("\\{\\{#water\\}\\}([\\s\\S]*?)\\{\\{/water\\}\\}")
        val waterElseBlockRegex = Regex("\\{\\{^water\\}\\}([\\s\\S]*?)\\{\\{/water\\}\\}")
        
        if (dto.waterAnalyses.isNotEmpty()) {
            // Находим шаблон внутри блока {{#water}}...{{/water}}
            val waterTemplateMatch = waterBlockRegex.find(html)
            if (waterTemplateMatch != null) {
                val template = waterTemplateMatch.groupValues[1]
                // Заменяем плейсхолдеры на реальные значения для каждого элемента
                val waterHtml = dto.waterAnalyses.joinToString("") { item ->
                    template.replace("{{name}}", escapeHtml(item.name))
                        .replace("{{value}}", escapeHtml(item.value))
                        .replace("{{unit}}", escapeHtml(item.unit))
                        .replace("{{norm}}", escapeHtml(item.norm))
                }
                html = html.replace(waterBlockRegex, waterHtml)
            }
            // Удаляем блок {{^water}}...{{/water}}
            html = html.replace(waterElseBlockRegex, "")
        } else {
            // Удаляем блок {{#water}}...{{/water}}
            html = html.replace(waterBlockRegex, "")
            // Оставляем содержимое блока {{^water}}...{{/water}}
            html = html.replace(waterElseBlockRegex, "$1")
        }
        
        // Заключение (опционально)
        if (dto.conclusions?.isNotBlank() == true) {
            html = html.replace("{{conclusion}}", escapeHtml(dto.conclusions))
            html = html.replace(Regex("\\{\\{#conclusion\\}\\}([\\s\\S]*?)\\{\\{/conclusion\\}\\}"), "$1")
        } else {
            html = html.replace(Regex("\\{\\{#conclusion\\}\\}([\\s\\S]*?)\\{\\{/conclusion\\}\\}"), "")
        }
        
        return html
    }

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}
