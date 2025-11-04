package com.example.wassertech.report

import android.content.Context
import com.example.wassertech.report.model.ReportDTO
import java.io.BufferedReader

object HtmlTemplateEngine {

    fun render(context: Context, templateAssetPath: String, dto: ReportDTO): String {
        val template = context.assets.open(templateAssetPath).bufferedReader().use(BufferedReader::readText)

        // Плейсхолдеры 1:1 с нашим планом
        val map = mapOf(
            "\${report.number}" to dto.reportNumber,
            "\${report.date}" to dto.reportDate,

            "\${company.name}" to (dto.companyName),
            "\${engineer.name}" to (dto.engineerName ?: ""),

            "\${client.name}" to dto.clientName,
            "\${client.address}" to (dto.clientAddress ?: ""),
            "\${client.phone}" to (dto.clientPhone ?: ""),

            "\${site.name}" to (dto.siteName ?: ""),
            "\${installation.name}" to dto.installationName,
            "\${installation.location}" to (dto.installationLocation ?: ""),

            "\${conclusions}" to (dto.conclusions ?: ""),
            "\${next.date}" to (dto.nextMaintenanceDate ?: "")
        )

        var html = template
        map.forEach { (k, v) -> html = html.replace(k, escapeHtml(v)) }

        // Строки таблицы компонентов
        val componentsRows = buildString {
            dto.components.forEachIndexed { idx, c ->
                appendLine(
                    """
                    <tr class="row">
                      <td class="col idx">${idx + 1}</td>
                      <td class="col name">${escapeHtml(c.name)}</td>
                      <td class="col type">${escapeHtml(c.type ?: "")}</td>
                      <td class="col serial">${escapeHtml(c.serial ?: "")}</td>
                      <td class="col status">${escapeHtml(c.status)}</td>
                      <td class="col notes">${escapeHtml(c.notes ?: "")}</td>
                    </tr>
                    """.trimIndent()
                )
            }
        }
        html = html.replace("<!-- COMPONENTS_ROWS -->", componentsRows)

        // Наблюдения списком
        val obsList = if (dto.observations.isNotEmpty()) {
            buildString {
                appendLine("<ul class=\"observations\">")
                dto.observations.forEach { line ->
                    appendLine("<li>${escapeHtml(line)}</li>")
                }
                appendLine("</ul>")
            }
        } else ""
        html = html.replace("<!-- OBSERVATIONS_LIST -->", obsList)

        // Лого (если есть)
        dto.logoAssetPath?.let {
            html = html.replace("\${assets.logo}", it)
        }

        return html
    }

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
