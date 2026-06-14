package com.xckrt.studentplanner.data
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("api/Schedule/group/{groupId}")
    suspend fun getSchedule(@Path("groupId") groupId: Int): List<ScheduleItem>
    @POST("api/auth/register")
    suspend fun register(@Body request:UserRegisterRequest): Response<Unit>
    @POST("api/auth/login")
    suspend fun login(@Body request: UserLoginRequest): Response<LoginResponse>
    @GET("api/Groups")
    suspend fun getGroups(): List<GroupItem>
    @GET("api/Changes/{groupName}/{date}")
    suspend fun getDailySchedule(
        @Path("groupName") groupName: String,
        @Path("date") date: String
    ): List<ScheduleItem>
    @GET("api/changes/weekparity")
    suspend fun getWeekParity(
        @Query("groupName") groupName: String,
        @Query("date") date: String
    ): WeekParityResponse
    @GET("api/Groups/name/{groupId}")
    suspend fun getGroupName(@Path("groupId") groupId: Int):GroupNameResponse
    @Multipart
    @POST("api/Parser/upload")
    suspend fun uploadSchedule(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>
    @GET("api/teachers/all")
    suspend fun getAllTeachers(): List<String>
    @GET("api/notes/group/{groupId}")
    suspend fun getGroupNotes(@Path("groupId") groupId: Int): List<SharedNoteDTO>
    @GET("api/teachers/now")
    suspend fun getTeacherNow(
        @Query("lastName") lastName: String,
        @Query("clientTime") clientTime: String
    ): TeacherNowResponse
    @POST("api/notes/shared")
    suspend fun saveSharedNote(@Body request: NoteRequest): Response<Unit>

    @GET("api/notes/history")
    suspend fun getNoteHistory(
        @Query("groupId") groupId: Int,
        @Query("subjectTitle") subjectTitle: String
    ): List<NoteHistoryDTO>
    @Multipart
    @PUT("api/auth/profile")
    suspend fun updateProfile(
        @Part("firstName") firstName: RequestBody?,
        @Part("lastName") lastName: RequestBody?,
        @Part avatar: MultipartBody.Part?
    ): Response<ProfileResponseDTO>
    @PUT("api/auth/update-group")
    suspend fun updateGroup(@Body request: UpdateGroupRequest):Response<Void>
    @GET("api/tasks")
    suspend fun getTasks(): List<TaskDto>

    @POST("api/tasks")
    suspend fun createTask(@Body task: TaskDto): Response<TaskDto>
    @GET("api/parser/absences")
    suspend fun getAbsentTeachers(
        @Query("date") date: String
    ): List<TeacherAbsenceDto>

    @PUT("api/tasks/{id}/status")
    suspend fun updateTaskStatus(
        @Path("id") id: Int,
        @Body isCompleted: Boolean
    ): Response<Void>
    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int):Response<Void>
}
data class LoginResponse(
    val token: String,
    val username: String,
    val groupId: Int,
    val userId: Int,
    val firstName: String?,
    val lastName: String?,
    val avatarUrl: String?
)
data class WeekParityResponse(
    val weekParity: Int
)
data class TeacherNowResponse(
    val status: String,
    val message: String? = null,
    val isChange: Boolean = false,
    val lessonNumber: Int? = null,
    val auditorium: String? = null,
    val groupName: String? = null,
    val subject: String? = null,
    val nextSubject: String? = null,
    val nextRoom: String? = null,
    val nextTime: String? = null
)
data class TeacherAbsenceDto(
    val teacherId: Int,
    val teacherName: String
)
data class TaskDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("subjectName") val subjectName: String?,
    @SerializedName("isCompleted") val isCompleted: Boolean,
    @SerializedName("deadline") val deadline: String?,
    @SerializedName("weight") val weight: Int
)
data class UpdateGroupRequest(val groupId: Int)
data class GroupNameResponse(val name: String)
data class GroupItem(
    val id: Int,
    val name: String
)
data class ProfileResponseDTO(
    val message: String,
    val firstName: String?,
    val lastName: String?,
    val avatarUrl: String?
)
data class UploadResponse(val groupId: Int, val message: String)