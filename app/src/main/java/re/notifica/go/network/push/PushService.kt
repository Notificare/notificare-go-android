package re.notifica.go.network.push

import re.notifica.go.network.push.models.Configuration
import retrofit2.http.GET
import retrofit2.http.Path

interface PushService {
    @GET("/download/demo/code/{code}")
    suspend fun getConfiguration(@Path("code") code: String): Configuration
}
