package com.xckrt.studentplanner.data

import com.google.gson.annotations.SerializedName
data class UserRegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("firstName") val firstName:String,
    @SerializedName("lastName") val lastName:String
)
data class UserLoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)