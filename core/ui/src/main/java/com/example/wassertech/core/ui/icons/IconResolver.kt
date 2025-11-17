package ru.wassertech.core.ui.icons

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.R
import java.io.File

/**
 * Утилита для разрешения иконок из ресурсов Android и локальных файлов.
 * 
 * Поддерживает:
 * - Разрешение иконок по androidResName из IconEntity
 * - Дефолтные иконки для каждого типа сущности
 * - Загрузку изображений из локальных файлов (если загружены с сервера)
 */
object IconResolver {
    
    /**
     * Разрешает drawable ресурс для иконки.
     * 
     * @param androidResName Имя ресурса Android (например, "ic_site_default")
     * @param entityType Тип сущности для выбора дефолтной иконки
     * @param context Android Context для доступа к ресурсам
     * @return ID drawable ресурса или null, если не найден
     */
    @DrawableRes
    fun resolveDrawableRes(
        androidResName: String?,
        entityType: IconEntityType,
        context: Context,
        code: String? = null // Код иконки для fallback поиска
    ): Int {
        // Пытаемся найти ресурс по androidResName
        if (!androidResName.isNullOrBlank()) {
            val resId = context.resources.getIdentifier(
                androidResName,
                "drawable",
                context.packageName
            )
            if (resId != 0) {
                android.util.Log.d("IconResolver", 
                    "Found resource: androidResName=$androidResName, resId=$resId"
                )
                return resId
            } else {
                android.util.Log.w("IconResolver", 
                    "Resource not found: androidResName=$androidResName, packageName=${context.packageName}, " +
                    "trying code fallback: code=$code"
                )
            }
        }
        
        // Fallback: пытаемся найти ресурс по code, если androidResName не задан или не найден
        if (!code.isNullOrBlank()) {
            // Пробуем разные варианты имени ресурса на основе code
            val codeVariants = listOf(
                code.lowercase().replace(" ", "_").replace("-", "_"),
                code.lowercase().replace(" ", "").replace("-", ""),
                "ic_${code.lowercase().replace(" ", "_").replace("-", "_")}",
                "icon_${code.lowercase().replace(" ", "_").replace("-", "_")}"
            )
            
            for (variant in codeVariants) {
                val resId = context.resources.getIdentifier(
                    variant,
                    "drawable",
                    context.packageName
                )
                if (resId != 0) {
                    android.util.Log.d("IconResolver", 
                        "Found resource by code fallback: code=$code, variant=$variant, resId=$resId"
                    )
                    return resId
                }
            }
            android.util.Log.d("IconResolver", 
                "No resource found for code=$code, tried variants: $codeVariants"
            )
        }
        
        // Если ресурс не найден, возвращаем дефолтную иконку для типа сущности
        val defaultResId = getDefaultIconForEntityType(entityType)
        android.util.Log.d("IconResolver", 
            "Using default icon: entityType=${entityType.name}, resId=$defaultResId, " +
            "androidResName=$androidResName, code=$code"
        )
        return defaultResId
    }
    
    /**
     * Получает дефолтную иконку для типа сущности.
     */
    @DrawableRes
    private fun getDefaultIconForEntityType(entityType: IconEntityType): Int {
        return when (entityType) {
            IconEntityType.SITE -> R.drawable.object_house_blue
            IconEntityType.INSTALLATION -> R.drawable.installation
            IconEntityType.COMPONENT -> R.drawable.ui_gear
            IconEntityType.ANY -> R.drawable.ui_gear // Fallback
        }
    }
    
    /**
     * Composable функция для отображения иконки.
     * 
     * @param androidResName Имя ресурса Android из IconEntity
     * @param entityType Тип сущности для выбора дефолтной иконки
     * @param imageUrl URL изображения (для загрузки с сервера, опционально)
     * @param localImagePath Локальный путь к файлу изображения (если загружено локально)
     * @param contentDescription Описание для accessibility
     * @param modifier Modifier для настройки размера и позиции
     * @param size Размер иконки (по умолчанию 48.dp)
     */
    @Composable
    fun IconImage(
        androidResName: String?,
        entityType: IconEntityType,
        imageUrl: String? = null,
        localImagePath: String? = null,
        contentDescription: String? = null,
        modifier: Modifier = Modifier,
        size: androidx.compose.ui.unit.Dp = 48.dp,
        code: String? = null // Код иконки для fallback поиска ресурса
    ) {
        val context = LocalContext.current

        android.util.Log.d("IconResolver",
            "localImagePath=${localImagePath}"
        )

        // Пытаемся загрузить из локального файла
        val localBitmap = remember(localImagePath) {
            localImagePath?.let { path ->
                try {
                    val file = File(path)


                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
        android.util.Log.d("IconResolver",
            "localBitmap=${localBitmap}"
        )
        // Если есть локальное изображение, используем его
        if (localBitmap != null) {
            Image(
                bitmap = localBitmap,
                contentDescription = contentDescription,
                modifier = modifier.size(size),
                contentScale = ContentScale.Fit
            )
        } else {
            // Иначе используем ресурс Android или дефолтную иконку
            val resId = remember(androidResName, entityType, code) {
                resolveDrawableRes(androidResName, entityType, context, code)
            }
            Image(
                painter = painterResource(id = resId),
                contentDescription = contentDescription,
                modifier = modifier.size(size),
                contentScale = ContentScale.Fit
            )
        }
    }
}


