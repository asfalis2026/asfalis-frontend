package com.yourname.womensafety.data

import android.content.Context
import com.yourname.womensafety.data.local.TokenManager
import com.yourname.womensafety.data.network.RetrofitClient
import com.yourname.womensafety.data.network.api.*
import com.yourname.womensafety.data.repository.*

/**
 * Manual service locator — provides all repositories without a DI framework.
 * Call [init] once in Application.onCreate() or lazily before first use.
 */
object AppServiceLocator {

    private lateinit var _tokenManager: TokenManager
    val tokenManager: TokenManager get() = _tokenManager

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            RetrofitClient.createService<AuthApiService>(_tokenManager),
            _tokenManager
        )
    }

    val userRepository: UserRepository by lazy {
        UserRepository(RetrofitClient.createService<UserApiService>(_tokenManager))
    }

    val sosRepository: SosRepository by lazy {
        SosRepository(RetrofitClient.createService<SosApiService>(_tokenManager))
    }

    val contactsRepository: ContactsRepository by lazy {
        ContactsRepository(RetrofitClient.createService<ContactsApiService>(_tokenManager))
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(RetrofitClient.createService<SettingsApiService>(_tokenManager))
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepository(RetrofitClient.createService<LocationApiService>(_tokenManager))
    }

    val supportRepository: SupportRepository by lazy {
        SupportRepository(RetrofitClient.createService<SupportApiService>(_tokenManager))
    }

    fun init(context: Context) {
        _tokenManager = TokenManager(context.applicationContext)
    }
}
