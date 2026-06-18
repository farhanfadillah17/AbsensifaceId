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

    suspend fun getFcba() : FcbaResponse {
        return api.getFcba()
    }
    suspend fun getJob(fcba: String) : JobResponse {
        return api.getJob(fcba)
    }
    suspend fun getField(fcba: String) : FieldResponse {
        return api.getField(fcba)
    }
    suspend fun getTph(fcba: String) : TphResponse {
        return api.getTph(fcba)
    }
    suspend fun getAccess() : AccessResponse {
        return api.getAccess()
    }

    interface ApiService {

        @GET("get_data_employee.asp")
        suspend fun getEmployee(
            @Query("fcba") fcba: String
        ): EmployeeResponse

        @GET("get_data_job.asp")
        suspend fun getJob(
            @Query("fcba") fcba: String
        ): JobResponse

        @GET("get_data_field.asp")
        suspend fun getField(
            @Query("fcba") fcba: String
        ): FieldResponse

        @GET("get_data_tph.asp")
        suspend fun getTph(
            @Query("fcba") fcba: String
        ): TphResponse


        @GET("get_data_user.asp")
        suspend fun getUser(): UsersResponse

        @GET("get_data_fcba.asp")
        suspend fun getFcba(): FcbaResponse

        @GET("get_data_access.asp")
        suspend fun getAccess(): AccessResponse

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

    data class Job(
        val fccode: String,
        val fcname: String,
        val job_category: String,
        val job_forfieldstatus: String,
        val unitofmeasurement: String,
        val job_own_tb: String,
        val uom_unit: String,
        val fcba: String
    )

    data class Field(
        val fccode: String,
        val fcname: String,
        val division: String,
        val hectarageplanted: String,
        val ownership: String,
        val activation: String,
        val plantingdate: String,
        val status: String,
        val fcba: String
    )

    data class Tph(
        val fccode: String,
        val fcname: String,
        val fieldcode: String,
        val section: String,
        val fcba: String
    )

    data class Fcba(
        val fccode: String,
        val fcname: String,
        val fccompanycode: String,
    )

    data class Access(
        val emp_id: String,
        val menu_code: String,
        val valid_until: String,
        val is_granted: String,
    )

    data class EmployeeResponse(
        val header: Header,
        val detail: List<Employee>
    )

    data class UsersResponse(
        val header: Header,
        val detail: List<Users>
    )
    data class JobResponse(
        val header: Header,
        val detail: List<Job>
    )
    data class FieldResponse(
        val header: Header,
        val detail: List<Field>
    )
    data class TphResponse(
        val header: Header,
        val detail: List<Tph>
    )
    data class FcbaResponse(
        val header: Header,
        val detail: List<Fcba>
    )
    data class AccessResponse(
        val header: Header,
        val detail: List<Access>
    )

}