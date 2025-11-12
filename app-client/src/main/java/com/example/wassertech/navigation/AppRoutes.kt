package ru.wassertech.navigation

/**
 * Маршруты приложения
 */
object AppRoutes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SESSION_DETAIL = "session_detail/{sessionId}"
    
    fun sessionDetail(sessionId: String) = "session_detail/$sessionId"
}

