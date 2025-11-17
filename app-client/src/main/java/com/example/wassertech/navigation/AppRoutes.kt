package ru.wassertech.navigation

/**
 * Маршруты приложения
 */
object AppRoutes {
    const val LOGIN = "login"
    const val SYNC = "sync"
    const val HOME = "home"
    const val HOME_SETTINGS = "home_settings"
    const val SITES = "sites/{clientId}"
    const val SITE_DETAIL = "site/{siteId}"
    const val INSTALLATION = "installation/{installationId}"
    const val SESSION_DETAIL = "session_detail/{sessionId}"
    const val MAINTENANCE_HISTORY = "maintenance_history/{installationId}"
    const val TEMPLATES = "templates"
    const val TEMPLATE_EDITOR = "template_editor/{templateId}"
    const val CLIENT_ICON_PACKS = "client_icon_packs"
    const val CLIENT_ICON_PACK_DETAIL = "client_icon_packs/{packId}"
    
    fun sites(clientId: String) = "sites/$clientId"
    fun siteDetail(siteId: String) = "site/$siteId"
    fun installation(installationId: String) = "installation/$installationId"
    fun sessionDetail(sessionId: String) = "session_detail/$sessionId"
    fun maintenanceHistory(installationId: String) = "maintenance_history/$installationId"
    fun templateEditor(templateId: String) = "template_editor/$templateId"
    fun clientIconPackDetail(packId: String) = "client_icon_packs/$packId"
}

