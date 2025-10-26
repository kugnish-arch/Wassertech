package com.example.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.wassertech.data.types.ComponentType

@Entity(tableName = "components")
data class ComponentEntity(
    @PrimaryKey val id: String,
    val installationId: String,
    val name: String,
    val type: ComponentType,          // kept for backward compatibility
    val orderIndex: Int = 0,

    // NEW: dynamic templates support
    val templateId: String? = null,   // FK to component_templates.id (logical; FK not enforced yet)
    val paramsJson: String? = null,   // instance-level parameters (override defaults)
    val nameOverride: String? = null  // local display name overriding template.name (optional)
)
