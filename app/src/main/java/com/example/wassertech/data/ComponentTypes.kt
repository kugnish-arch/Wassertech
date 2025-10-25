package com.example.wassertech.data

enum class ComponentType {
    FILTER,
    RO,
    COMPRESSOR,
    AERATION,
    DOSING,
    SOFTENER   // 🔹 новый тип для ионообменных умягчителей
}

enum class FieldType { CHECKBOX, NUMBER, TEXT }
enum class Severity { LOW, MEDIUM, HIGH }
