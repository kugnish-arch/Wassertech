package ru.wassertech.util

object Translit {
    private val map = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
        'е' to "e", 'ё' to "e", 'ж' to "zh", 'з' to "z", 'и' to "i",
        'й' to "i", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
        'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
        'у' to "u", 'ф' to "f", 'х' to "h", 'ц' to "c", 'ч' to "ch",
        'ш' to "sh", 'щ' to "sch", 'ъ' to "",  'ы' to "y", 'ь' to "",
        'э' to "e", 'ю' to "yu", 'я' to "ya"
    )

    fun ruToEnKey(input: String): String {
        if (input.isBlank()) return ""
        val sb = StringBuilder()
        input.lowercase().forEach { ch ->
            when {
                ch in 'a'..'z' || ch.isDigit() -> sb.append(ch)
                map.containsKey(ch) -> sb.append(map[ch])
                else -> sb.append('_')
            }
        }
        var s = sb.toString()
        s = s.replace(Regex("_+"), "_").trim('_')
        return s
    }
}