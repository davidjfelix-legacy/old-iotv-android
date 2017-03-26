package tv.mg4.app.api

import io.reactivex.Observable
import retrofit2.http.*
import tv.mg4.app.api.requests.NewVideo
import tv.mg4.app.api.requests.PartialVideo
import tv.mg4.app.api.responses.ServerResponse
import tv.mg4.app.api.responses.Video

interface Mg4API {

    @DELETE("/videos/{id}")
    fun deleteVideo(@Path("id") id: String): Observable<ServerResponse>

    @GET("/videos")
    fun getVideos(): Observable<List<Video>>

    @GET("/videos/{id}")
    fun getVideo(@Path("id") id: String): Observable<Video>

    @PATCH("/videos/{id}")
    fun patchVideo(@Path("id") id: String, updates: PartialVideo): Observable<Video>

    @POST("/videos")
    fun postVideo(newVideo: NewVideo): Observable<Video>
    
    @PUT("/videos/{id}")
    fun putVideo(@Path("id") id: String): Observable<Video>
}