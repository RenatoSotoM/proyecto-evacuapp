package com.example.proyecto_evacuapp.data.remote

import com.google.gson.annotations.SerializedName

data class IncidentNetworkDto(
    @SerializedName("type") val type: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("description") val description: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class IncidentResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("description") val description: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("status") val status: String,
    @SerializedName("alpha") val alpha: Double = 1.0,
    @SerializedName("beta") val beta: Double = 1.0,
    @SerializedName("confidence") val confidence: Double? = null
) {
    val computedConfidence: Double
        get() = confidence ?: if (alpha + beta > 0.0) alpha / (alpha + beta) else 0.5
}

data class VoteIncidentNetworkDto(
    @SerializedName("vote") val vote: String
)

data class IncidentVoteResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("alpha") val alpha: Int,
    @SerializedName("beta") val beta: Int,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("status") val status: String
)