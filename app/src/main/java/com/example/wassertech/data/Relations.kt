package com.example.wassertech.data

import androidx.room.Embedded
import androidx.room.Relation

data class ClientWithSites(
    @Embedded val client: ClientEntity,
    @Relation(parentColumn = "id", entityColumn = "clientId")
    val sites: List<SiteEntity>
)

data class SiteWithInstallations(
    @Embedded val site: SiteEntity,
    @Relation(parentColumn = "id", entityColumn = "siteId")
    val installations: List<InstallationEntity>
)

data class InstallationWithComponents(
    @Embedded val installation: InstallationEntity,
    @Relation(parentColumn = "id", entityColumn = "installationId")
    val components: List<ComponentEntity>
)
