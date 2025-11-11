package com.example.wassertech.core.network

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Информация о пользователе
 */
data class UserInfo(
    val id: String,
    val login: String,
    val password: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val role: String = "USER",
    val permissions: String? = null,
    val lastLoginAtEpoch: Long? = null,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

/**
 * API сервис для авторизации пользователей
 */
object AuthApiService {
    
    private const val DB_URL = "jdbc:mysql://kugnis.beget.tech:3306/kugnis_app?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    private const val DB_USER = "kugnis_app"
    private const val DB_PASSWORD = "Umnik1985!"
    
    private const val TAG = "AuthApiService"
    
    /**
     * Регистрация нового пользователя
     */
    suspend fun registerUser(
        login: String,
        password: String,
        name: String? = null,
        email: String? = null,
        phone: String? = null
    ): Pair<Boolean, String> {
        var connection: Connection? = null
        try {
            Log.d(TAG, "Начало регистрации пользователя: $login")
            
            // Загружаем драйвер MySQL
            try {
                Class.forName("com.mysql.jdbc.Driver")
            } catch (e: ClassNotFoundException) {
                Log.e(TAG, "Ошибка загрузки MySQL драйвера", e)
                return false to "Ошибка загрузки драйвера БД: ${e.message}"
            }
            
            // Подключаемся к БД
            try {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
                connection.autoCommit = false
            } catch (e: SQLException) {
                Log.e(TAG, "Ошибка подключения к MySQL БД", e)
                return false to "Ошибка подключения к БД: ${e.message}"
            }
            
            // Создаём таблицу пользователей, если её нет
            createUsersTableIfNotExist(connection)
            
            // Проверяем, существует ли пользователь с таким логином (регистронезависимый поиск)
            val checkSql = "SELECT COUNT(*) FROM users WHERE LOWER(login) = LOWER(?)"
            val checkPs = connection.prepareStatement(checkSql)
            checkPs.setString(1, login)
            val rs = checkPs.executeQuery()
            rs.next()
            val count = rs.getInt(1)
            rs.close()
            checkPs.close()
            
            if (count > 0) {
                connection.rollback()
                return false to "Пользователь с логином \"$login\" уже существует"
            }
            
            // Создаём нового пользователя (по умолчанию роль USER и права по умолчанию)
            val userId = java.util.UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val defaultPermissions = "{}" // Пустые права по умолчанию
            
            val insertSql = """
                INSERT INTO users (id, login, password, name, email, phone, role, permissions, createdAtEpoch, updatedAtEpoch)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            
            val insertPs = connection.prepareStatement(insertSql)
            insertPs.setString(1, userId)
            insertPs.setString(2, login)
            insertPs.setString(3, password)
            insertPs.setString(4, name)
            insertPs.setString(5, email)
            insertPs.setString(6, phone)
            insertPs.setString(7, "USER")
            insertPs.setString(8, defaultPermissions)
            insertPs.setLong(9, now)
            insertPs.setLong(10, now)
            
            insertPs.executeUpdate()
            insertPs.close()
            
            connection.commit()
            Log.d(TAG, "Пользователь успешно зарегистрирован: $login")
            return true to "Пользователь успешно зарегистрирован"
            
        } catch (e: Exception) {
            try {
                connection?.rollback()
            } catch (rollbackEx: Exception) {
                Log.e(TAG, "Ошибка при откате транзакции", rollbackEx)
            }
            Log.e(TAG, "Ошибка при регистрации пользователя", e)
            return false to "Ошибка при регистрации: ${e.message}"
        } finally {
            try {
                connection?.close()
            } catch (closeEx: Exception) {
                Log.e(TAG, "Ошибка при закрытии соединения", closeEx)
            }
        }
    }
    
    /**
     * Проверяет логин и пароль пользователя и возвращает полную информацию о пользователе
     * @return Triple<Boolean, UserInfo?, String?> - (успех, пользователь или null, сообщение об ошибке)
     */
    suspend fun loginUser(login: String, password: String): Triple<Boolean, UserInfo?, String?> {
        var connection: Connection? = null
        try {
            Log.d(TAG, "Попытка входа пользователя: $login")
            
            // Загружаем драйвер MySQL
            try {
                Class.forName("com.mysql.jdbc.Driver")
            } catch (e: ClassNotFoundException) {
                Log.e(TAG, "Ошибка загрузки MySQL драйвера", e)
                return Triple(false, null, "Ошибка загрузки драйвера БД")
            }
            
            // Подключаемся к БД
            try {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
                connection.autoCommit = false
            } catch (e: SQLException) {
                Log.e(TAG, "Ошибка подключения к MySQL БД", e)
                return Triple(false, null, "Ошибка подключения к БД")
            }
            
            // Создаём таблицу пользователей, если её нет
            createUsersTableIfNotExist(connection)
            
            // Получаем полную информацию о пользователе (регистронезависимый поиск)
            val sql = "SELECT id, login, password, name, email, phone, role, permissions, lastLoginAtEpoch, createdAtEpoch, updatedAtEpoch FROM users WHERE LOWER(login) = LOWER(?)"
            val ps = connection.prepareStatement(sql)
            ps.setString(1, login)
            val rs = ps.executeQuery()
            
            if (!rs.next()) {
                rs.close()
                ps.close()
                Log.d(TAG, "Пользователь не найден: $login")
                return Triple(false, null, "Пользователь не найден")
            }
            
            val userId = rs.getString("id")
            val storedPassword = rs.getString("password")
            
            if (storedPassword != password) {
                rs.close()
                ps.close()
                Log.d(TAG, "Неверный пароль для пользователя: $login")
                return Triple(false, null, "Неверный пароль")
            }
            
            // Читаем данные пользователя
            val user = UserInfo(
                id = userId,
                login = rs.getString("login"),
                password = storedPassword, // Возвращаем пароль (в реальном приложении лучше не возвращать)
                name = rs.getString("name"),
                email = rs.getString("email"),
                phone = rs.getString("phone"),
                role = rs.getString("role") ?: "USER",
                permissions = rs.getString("permissions"),
                lastLoginAtEpoch = rs.getObject("lastLoginAtEpoch") as? Long,
                createdAtEpoch = rs.getLong("createdAtEpoch"),
                updatedAtEpoch = rs.getLong("updatedAtEpoch")
            )
            
            rs.close()
            ps.close()
            
            // Обновляем время последнего входа
            val now = System.currentTimeMillis()
            val updateSql = "UPDATE users SET lastLoginAtEpoch = ?, updatedAtEpoch = ? WHERE id = ?"
            val updatePs = connection.prepareStatement(updateSql)
            updatePs.setLong(1, now)
            updatePs.setLong(2, now)
            updatePs.setString(3, userId)
            updatePs.executeUpdate()
            updatePs.close()
            
            connection.commit()
            
            Log.d(TAG, "Пользователь успешно авторизован: $login")
            return Triple(true, user.copy(lastLoginAtEpoch = now), null)
            
        } catch (e: Exception) {
            try {
                connection?.rollback()
            } catch (rollbackEx: Exception) {
                Log.e(TAG, "Ошибка при откате транзакции", rollbackEx)
            }
            Log.e(TAG, "Ошибка при входе пользователя", e)
            return Triple(false, null, "Ошибка при входе: ${e.message}")
        } finally {
            try {
                connection?.close()
            } catch (closeEx: Exception) {
                Log.e(TAG, "Ошибка при закрытии соединения", closeEx)
            }
        }
    }
    
    /**
     * Создаёт таблицу пользователей, если её нет
     */
    private fun createUsersTableIfNotExist(connection: Connection) {
        val createTableSql = """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(36) PRIMARY KEY,
                login VARCHAR(255) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                name VARCHAR(255),
                email VARCHAR(255),
                phone VARCHAR(255),
                role VARCHAR(50) DEFAULT 'USER',
                permissions TEXT,
                lastLoginAtEpoch BIGINT,
                createdAtEpoch BIGINT NOT NULL,
                updatedAtEpoch BIGINT NOT NULL,
                INDEX idx_login (login)
            )
        """.trimIndent()
        
        try {
            val stmt = connection.createStatement()
            stmt.execute(createTableSql)
            stmt.close()
        } catch (e: SQLException) {
            Log.e(TAG, "Ошибка при создании таблицы users", e)
        }
    }
}

