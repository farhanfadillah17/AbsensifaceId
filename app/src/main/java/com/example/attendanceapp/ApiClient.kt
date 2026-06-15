package com.example.attendanceapp

import android.util.Log
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val username = "SIP"
private const val password = "Sejahtera2025"


class ApiClient {

    private val api: ApiService

    init {

        val credential = Credentials.basic(
            username,
            password
        )

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->

                val request = chain.request()
                    .newBuilder()
                    .header("Authorization", credential)
                    .build()

                chain.proceed(request)
            }
            .build()

        api = Retrofit.Builder()
            .baseUrl("http://66.96.237.103:1000/API_ANDROID/")
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)
    }


    suspend fun getEmployee(fcba: String): EmployeeResponse {
//        Log.d("req fcba", "getEmployee: $fcba")
        return api.getEmployee(fcba)
    }

    suspend fun getUser() : UsersResponse {
        return api.getUser()
    }

    interface ApiService {

        @GET("get_data_employee.asp")
        suspend fun getEmployee(
            @Query("fcba") fcba: String
        ): EmployeeResponse


        @GET("get_data_user.asp")
        suspend fun getUser(): UsersResponse
    }

    data class Header(
        val total: Int
    )

    data class Employee(
        val fccode: String,
        val fcname: String,
        val section: String,
        val gangcode: String,
        val fcba: String
    )

    data class Users(
        val username: String,
        val password: String,
        val empcode: String,
        val fcba: String,
        val role: String
    )

    data class EmployeeResponse(
        val header: Header,
        val detail: List<Employee>
    )

    data class UsersResponse(
        val header: Header,
        val detail: List<Users>
    )

}