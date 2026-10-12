package com.example.proyecto_evacuapp.data.remote

import com.google.gson.annotations.SerializedName

data class UserMeResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("role") val role: String? = "USER",
    @SerializedName("active") val active: Boolean? = true,
    @SerializedName("mobilityProfile") val mobilityProfile: MobilityProfileResponse? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class MobilityProfileResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("mobilityType") val mobilityType: String? = "VEHICULO",
    @SerializedName("requiresAccessibleRoute") val requiresAccessibleRoute: Boolean = false,
    @SerializedName("travelsWithMinors") val travelsWithMinors: Boolean = false,
    @SerializedName("companionCount") val companionCount: Int = 0,
    @SerializedName("updatedAt") val updatedAt: String? = null
) {
    val companionDescription: String = when {
        companionCount > 0 && travelsWithMinors -> "$companionCount acompañantes + menores"
        companionCount > 0 -> "$companionCount acompañantes"
        travelsWithMinors -> "Menores a cargo"
        else -> "Solo"
    }
}

data class UpdateMobilityProfileRequest(
    @SerializedName("mobilityType") val mobilityType: String? = null,
    @SerializedName("requiresAccessibleRoute") val requiresAccessibleRoute: Boolean? = null,
    @SerializedName("travelsWithMinors") val travelsWithMinors: Boolean? = null,
    @SerializedName("companionCount") val companionCount: Int? = null
)
