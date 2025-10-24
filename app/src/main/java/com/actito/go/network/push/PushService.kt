package com.actito.go.network.push

import com.actito.go.network.push.models.Configuration
import com.actito.go.network.push.payloads.EnrollmentPayload
import com.actito.go.network.push.responses.EnrollmentResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PushService {
    @GET("/download/demo/code/{code}")
    suspend fun getConfiguration(@Path("code") code: String): Configuration

    @POST("/loyalty/profile/enrollment/{id}")
    suspend fun createEnrollment(
        @Path(value = "id") programId: String,
        @Body payload: EnrollmentPayload,
    ): EnrollmentResponse
}
