package com.example.proyecto_evacuapp.data.remote

import com.google.gson.annotations.SerializedName

data class UserMeResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("role") val role: String,
    @SerializedName("active") val active: Boolean,
    @SerializedName("mobilityProfile") val mobilityProfile: MobilityProfileResponse?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class MobilityProfileResponse(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("mobilityType") val mobilityType: String,
    @SerializedName("requiresAccessibleRoute") val requiresAccessibleRoute: Boolean,
    @SerializedName("travelsWithMinors") val travelsWithMinors: Boolean,
    @SerializedName("companionCount") val companionCount: Int,
    @SerializedName("updatedAt") val updatedAt: String
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