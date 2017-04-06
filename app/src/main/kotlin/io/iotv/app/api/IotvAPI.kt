package io.iotv.app.api

import io.reactivex.Observable
import retrofit2.http.*
import io.iotv.app.api.requests.NewVideo
import io.iotv.app.api.requests.PartialVideo
import io.iotv.app.api.responses.ServerResponse
import io.iotv.app.api.responses.Video

interface IotvAPI {

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