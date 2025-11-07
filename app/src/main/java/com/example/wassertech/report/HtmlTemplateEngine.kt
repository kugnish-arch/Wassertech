package com.example.wassertech.report

import android.content.Context
import android.util.Log
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
        
        // Комментарии (опционально)
        if (dto.comments?.isNotBlank() == true) {
            html = html.replace("{{comments}}", escapeHtml(dto.comments))
            html = html.replace(Regex("\\{\\{#comments\\}\\}([\\s\\S]*?)\\{\\{/comments\\}\\}"), "$1")
        } else {
            html = html.replace(Regex("\\{\\{#comments\\}\\}([\\s\\S]*?)\\{\\{/comments\\}\\}"), "")
            html = html.replace("{{comments}}", "")
        }
        
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
        
        // Компоненты с полями
        // Ищем блок {{#componentsWithFields}}...{{/componentsWithFields}}
        val componentsBlockRegex = Regex("\\{\\{#componentsWithFields\\}\\}([\\s\\S]*?)\\{\\{/componentsWithFields\\}\\}")
        // Внутри ищем блок {{#component}}...{{/component}}
        val componentBlockRegex = Regex("\\{\\{#component\\}\\}([\\s\\S]*?)\\{\\{/component\\}\\}")
        // Внутри компонента ищем блок {{#fields}}...{{/fields}}
        val fieldBlockRegex = Regex("\\{\\{#fields\\}\\}([\\s\\S]*?)\\{\\{/fields\\}\\}")
        
        if (dto.componentsWithFields.isNotEmpty()) {
            Log.d("HtmlTemplate", "Processing ${dto.componentsWithFields.size} components")
            val componentsBlockMatch = componentsBlockRegex.find(html)
            if (componentsBlockMatch != null) {
                val componentsBlockContent = componentsBlockMatch.groupValues[1]
                val componentBlockMatch = componentBlockRegex.find(componentsBlockContent)
                
                if (componentBlockMatch != null) {
                    val componentTemplate = componentBlockMatch.groupValues[1]
                    val fieldBlockMatch = fieldBlockRegex.find(componentTemplate)
                    
                    // Генерируем HTML для всех компонентов
                    val componentsHtml = dto.componentsWithFields.mapIndexed { index, component ->
                        Log.d("HtmlTemplate", "Processing component $index: ${component.componentName} with ${component.fields.size} fields")
                        var componentHtml = componentTemplate
                            .replace("{{component.name}}", escapeHtml(component.componentName))
                            .replace("{{component.type}}", escapeHtml(component.componentType ?: ""))
                        
                        // Обрабатываем поля компонента
                        if (fieldBlockMatch != null && component.fields.isNotEmpty()) {
                            val fieldTemplate = fieldBlockMatch.groupValues[1]
                            val unitBlockRegex = Regex("\\{\\{#field\\.unit\\}\\}([\\s\\S]*?)\\{\\{/field\\.unit\\}\\}")
                            val fieldsHtml = component.fields.joinToString("") { field ->
                                var fieldHtml = fieldTemplate
                                    .replace("{{field.label}}", escapeHtml(field.label))
                                    .replace("{{field.value}}", escapeHtml(field.value))
                                    .replace("{{field.checkboxClass}}", field.checkboxClass ?: "")
                                
                                // Обработка условного блока для единиц измерения
                                if (field.unit != null && field.unit.isNotBlank()) {
                                    val unitBlockMatch = unitBlockRegex.find(fieldHtml)
                                    if (unitBlockMatch != null) {
                                        val unitContent = unitBlockMatch.groupValues[1]
                                        fieldHtml = fieldHtml.replace(unitBlockRegex, unitContent.replace("{{field.unit}}", escapeHtml(field.unit)))
                                    } else {
                                        fieldHtml = fieldHtml.replace("{{field.unit}}", escapeHtml(field.unit))
                                    }
                                } else {
                                    fieldHtml = fieldHtml.replace(unitBlockRegex, "")
                                    fieldHtml = fieldHtml.replace("{{field.unit}}", "")
                                }
                                
                                fieldHtml
                            }
                            componentHtml = componentHtml.replace(fieldBlockRegex, fieldsHtml)
                        } else {
                            componentHtml = componentHtml.replace(fieldBlockRegex, "")
                        }
                        
                        componentHtml
                    }.joinToString("\n        ")
                    
                    Log.d("HtmlTemplate", "Generated components HTML length: ${componentsHtml.length}")
                    
                    // Заменяем весь блок componentsWithFields на сгенерированный HTML
                    // Сохраняем контекст вокруг блока component (например, <div class="components-grid">)
                    val componentRange = componentBlockMatch.range
                    val beforeComponent = componentsBlockContent.substring(0, componentRange.first)
                    val afterComponent = componentsBlockContent.substring(componentRange.last + 1)
                    val finalComponentsContent = beforeComponent + componentsHtml + afterComponent
                    
                    Log.d("HtmlTemplate", "Before component: '${beforeComponent.take(50)}...'")
                    Log.d("HtmlTemplate", "After component: '...${afterComponent.takeLast(50)}'")
                    Log.d("HtmlTemplate", "Final content length: ${finalComponentsContent.length}")
                    
                    // Заменяем весь блок componentsWithFields на сгенерированный HTML
                    // Используем replace с Regex для замены всего блока
                    val htmlBeforeReplace = html.length
                    html = componentsBlockRegex.replace(html, finalComponentsContent)
                    val htmlAfterReplace = html.length
                    Log.d("HtmlTemplate", "HTML length before: $htmlBeforeReplace, after: $htmlAfterReplace")
                } else {
                    // Если не найден блок component, удаляем весь блок componentsWithFields
                    html = html.replace(componentsBlockRegex, "")
                }
            }
        } else {
            // Если нет компонентов, удаляем весь блок
            html = html.replace(componentsBlockRegex, "")
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
