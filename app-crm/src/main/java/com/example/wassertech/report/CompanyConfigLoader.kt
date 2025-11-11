package com.example.wassertech.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.wassertech.report.model.CompanyConfig
import com.example.wassertech.report.model.ContractConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream

object CompanyConfigLoader {
    
    fun loadConfig(context: Context): Pair<CompanyConfig?, ContractConfig?> {
        return try {
            val jsonString = context.assets.open("config/company_config.json")
                .bufferedReader().use(BufferedReader::readText)
            val json = JSONObject(jsonString)
            
            val companyJson = json.optJSONObject("company")
            val contractJson = json.optJSONObject("contract")
            
            val companyConfig = companyJson?.let {
                CompanyConfig(
                    legal_name = it.optString("legal_name", ""),
                    inn = it.optString("inn", ""),
                    phone1 = it.optString("phone1", ""),
                    phone2 = it.optString("phone2", ""),
                    email = it.optString("email", ""),
                    website = it.optString("website", ""),
                    sign_name = it.optString("sign_name", ""),
                    sign_short = it.optString("sign_short", "")
                )
            }
            
            val contractConfig = contractJson?.let {
                ContractConfig(
                    number = it.optString("number", ""),
                    date_rus = it.optString("date_rus", "")
                )
            }
            
            Pair(companyConfig, contractConfig)
        } catch (e: Exception) {
            Log.e("CompanyConfigLoader", "Error loading config", e)
            Pair(null, null)
        }
    }
    
    /**
     * Конвертирует изображение из assets в data URI для использования в HTML
     */
    fun logoToDataUri(context: Context, assetPath: String): String {
        return try {
            val inputStream = context.assets.open(assetPath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/png;base64,$base64"
        } catch (e: Exception) {
            Log.e("CompanyConfigLoader", "Error loading logo", e)
            ""
        }
    }
}

