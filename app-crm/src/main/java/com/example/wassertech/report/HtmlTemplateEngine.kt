package ru.wassertech.report

import android.content.Context
import android.util.Log
import ru.wassertech.report.model.ReportDTO
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
        
        // Логотип как data URI (используем logo-wassertech.png)
        val logoDataUri = CompanyConfigLoader.logoToDataUri(context, "img/logo-wassertech.png")
        html = html.replace("{{company.logo_data_uri}}", logoDataUri)
        
        // Подпись и печать как data URI
        val signatureDataUri = CompanyConfigLoader.logoToDataUri(context, "img/signature.png")
        html = html.replace("{{company.signature_data_uri}}", signatureDataUri)
        
        val stampDataUri = CompanyConfigLoader.logoToDataUri(context, "img/stamp.png")
        html = html.replace("{{company.stamp_data_uri}}", stampDataUri)
        
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
        
        // Компоненты с полями - новая логика с разделами для HEAD компонентов
        // Ищем блок {{#componentsWithFields}}...{{/componentsWithFields}}
        val componentsBlockRegex = Regex("\\{\\{#componentsWithFields\\}\\}([\\s\\S]*?)\\{\\{/componentsWithFields\\}\\}")
        // Внутри ищем блок {{#component}}...{{/component}}
        val componentBlockRegex = Regex("\\{\\{#component\\}\\}([\\s\\S]*?)\\{\\{/component\\}\\}")
        // Внутри компонента ищем блок {{#fields}}...{{/fields}}
        val fieldBlockRegex = Regex("\\{\\{#fields\\}\\}([\\s\\S]*?)\\{\\{/fields\\}\\}")
        
        if (dto.componentsWithFields.isNotEmpty()) {
            Log.d("HtmlTemplate", "Processing ${dto.componentsWithFields.size} components")
            // Логируем типы компонентов для отладки
            val headCount = dto.componentsWithFields.count { it.componentType == "HEAD" }
            val commonCount = dto.componentsWithFields.count { it.componentType == "COMMON" }
            Log.d("HtmlTemplate", "HEAD components: $headCount, COMMON components: $commonCount")
            dto.componentsWithFields.forEachIndexed { index, comp ->
                Log.d("HtmlTemplate", "  Component[$index]: ${comp.componentName}, type=${comp.componentType}, fields=${comp.fields.size}")
            }
            val componentsBlockMatch = componentsBlockRegex.find(html)
            if (componentsBlockMatch != null) {
                val componentsBlockContent = componentsBlockMatch.groupValues[1]
                val componentBlockMatch = componentBlockRegex.find(componentsBlockContent)
                
                if (componentBlockMatch != null) {
                    val componentTemplate = componentBlockMatch.groupValues[1]
                    val fieldBlockMatch = fieldBlockRegex.find(componentTemplate)
                    
                    // Функция для генерации HTML компонента
                    fun generateComponentHtml(
                        component: ru.wassertech.report.model.ComponentWithFieldsDTO,
                        isHead: Boolean,
                        hideHeader: Boolean = false
                    ): String {
                        var componentHtml = componentTemplate
                            .replace("{{component.name}}", escapeHtml(component.componentName))
                            .replace("{{component.type}}", escapeHtml(component.componentType ?: ""))
                        
                        // Добавляем классы
                        var classAttr = "component-card"
                        if (isHead) {
                            classAttr += " component-card-head"
                        }
                        if (hideHeader) {
                            classAttr += " component-card-no-header"
                        }
                        componentHtml = componentHtml.replace(
                            "class=\"component-card\"",
                            "class=\"$classAttr\""
                        )
                        
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
                        
                        return componentHtml
                    }
                    
                    // Разделяем компоненты на группы:
                    // 1. HEAD компоненты в начале (непрерывная последовательность)
                    // 2. COMMON компоненты в середине
                    // 3. HEAD компоненты в конце (непрерывная последовательность)
                    // ВАЖНО: HEAD компоненты в середине списка (между COMMON) также обрабатываются как COMMON
                    // для упрощения логики, но они должны отрисовываться во всю ширину
                    
                    // Находим HEAD компоненты в начале (непрерывная последовательность)
                    val headAtStart = mutableListOf<ru.wassertech.report.model.ComponentWithFieldsDTO>()
                    var startIndex = 0
                    for (component in dto.componentsWithFields) {
                        if (component.componentType == "HEAD") {
                            headAtStart.add(component)
                            startIndex++
                        } else {
                            break
                        }
                    }
                    
                    // Находим HEAD компоненты в конце (непрерывная последовательность)
                    val headAtEnd = mutableListOf<ru.wassertech.report.model.ComponentWithFieldsDTO>()
                    var endIndex = dto.componentsWithFields.size - 1
                    for (i in dto.componentsWithFields.size - 1 downTo 0) {
                        val component = dto.componentsWithFields[i]
                        if (component.componentType == "HEAD") {
                            headAtEnd.add(0, component) // Добавляем в начало списка для сохранения порядка
                            endIndex = i - 1
                        } else {
                            break
                        }
                    }
                    
                    // Проверяем, не пересекаются ли HEAD в начале и конце
                    // Если startIndex > endIndex + 1, значит все компоненты HEAD
                    val allHead = startIndex > endIndex + 1
                    
                    // Если все компоненты HEAD, они должны быть только в начале
                    if (allHead) {
                        headAtEnd.clear()
                        endIndex = dto.componentsWithFields.size - 1
                    }
                    
                    // COMMON компоненты - это все компоненты между HEAD в начале и конце
                    // ВАЖНО: HEAD компоненты в середине списка также включаются в commonOnly,
                    // но они будут обработаны с флагом isHead=true в generateComponentHtml
                    val commonOnly = if (!allHead && startIndex <= endIndex) {
                        dto.componentsWithFields.filterIndexed { index, component ->
                            // Пропускаем HEAD компоненты в начале и конце
                            // Включаем все компоненты между ними (и COMMON, и HEAD в середине)
                            index >= startIndex && index <= endIndex
                        }
                    } else {
                        emptyList()
                    }
                    
                    // Генерируем HTML для разделов
                    val sectionsHtml = StringBuilder()
                    
                    // 1. Раздел HEAD компонентов в начале
                    if (headAtStart.isNotEmpty()) {
                        val firstHeadName = headAtStart.first().componentName
                        sectionsHtml.append("<!-- HEAD компоненты в начале -->\n")
                        sectionsHtml.append("<section class=\"section head-components-section\">\n")
                        sectionsHtml.append("    <h2 class=\"section-header-red\">${escapeHtml(firstHeadName)}</h2>\n")
                        sectionsHtml.append("    <div class=\"components-grid\">\n")
                        
                        headAtStart.forEachIndexed { index, component ->
                            val hideHeader = index == 0 // Первый компонент без заголовка
                            sectionsHtml.append("        ")
                            sectionsHtml.append(generateComponentHtml(component, true, hideHeader))
                            sectionsHtml.append("\n")
                        }
                        
                        sectionsHtml.append("    </div>\n")
                        sectionsHtml.append("</section>\n\n")
                    }
                    
                    // 2. Раздел COMMON компонентов (и HEAD компонентов в середине)
                    if (commonOnly.isNotEmpty()) {
                        sectionsHtml.append("<!-- COMMON компоненты и HEAD в середине -->\n")
                        sectionsHtml.append("<section class=\"section\">\n")
                        sectionsHtml.append("    <h2 class=\"section-header-red\">Результаты проверки компонентов</h2>\n")
                        sectionsHtml.append("    <div class=\"components-grid\">\n")
                        
                        commonOnly.forEach { component ->
                            // HEAD компоненты в середине обрабатываются с isHead=true для отрисовки во всю ширину
                            val isHead = component.componentType == "HEAD"
                            sectionsHtml.append("        ")
                            sectionsHtml.append(generateComponentHtml(component, isHead))
                            sectionsHtml.append("\n")
                        }
                        
                        sectionsHtml.append("    </div>\n")
                        sectionsHtml.append("</section>\n\n")
                    }
                    
                    // 3. Раздел HEAD компонентов в конце
                    if (headAtEnd.isNotEmpty()) {
                        val firstHeadName = headAtEnd.first().componentName
                        sectionsHtml.append("<!-- HEAD компоненты в конце -->\n")
                        sectionsHtml.append("<section class=\"section head-components-section\">\n")
                        sectionsHtml.append("    <h2 class=\"section-header-red\">${escapeHtml(firstHeadName)}</h2>\n")
                        sectionsHtml.append("    <div class=\"components-grid\">\n")
                        
                        headAtEnd.forEachIndexed { index, component ->
                            val hideHeader = index == 0 // Первый компонент без заголовка
                            sectionsHtml.append("        ")
                            sectionsHtml.append(generateComponentHtml(component, true, hideHeader))
                            sectionsHtml.append("\n")
                        }
                        
                        sectionsHtml.append("    </div>\n")
                        sectionsHtml.append("</section>\n\n")
                    }
                    
                    // Заменяем весь блок componentsWithFields
                    val componentRange = componentBlockMatch.range
                    val beforeComponent = componentsBlockContent.substring(0, componentRange.first)
                    val afterComponent = componentsBlockContent.substring(componentRange.last + 1)
                    
                    // Удаляем старую структуру секции, оставляем только контент разделов
                    val finalContent = sectionsHtml.toString()
                    
                    Log.d("HtmlTemplate", "Generated sections HTML length: ${finalContent.length}")
                    Log.d("HtmlTemplate", "Head at start: ${headAtStart.size}, Common: ${commonOnly.size}, Head at end: ${headAtEnd.size}")
                    headAtStart.forEachIndexed { idx, comp ->
                        Log.d("HtmlTemplate", "  Head at start[$idx]: ${comp.componentName}, fields=${comp.fields.size}")
                    }
                    headAtEnd.forEachIndexed { idx, comp ->
                        Log.d("HtmlTemplate", "  Head at end[$idx]: ${comp.componentName}, fields=${comp.fields.size}")
                    }
                    
                    // Заменяем весь блок componentsWithFields
                    html = componentsBlockRegex.replace(html, finalContent)
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
