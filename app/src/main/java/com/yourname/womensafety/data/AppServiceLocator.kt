package com.yourname.womensafety.data

import android.app.Application
import android.content.Context
import com.yourname.womensafety.data.local.AppCache
import com.yourname.womensafety.data.local.TokenManager
import com.yourname.womensafety.data.network.RetrofitClient
import com.yourname.womensafety.data.network.api.*
import com.yourname.womensafety.data.repository.*

/**
 * Manual service locator — provides all repositories and shared services.
 * Call [init] once in Application.onCreate() before any repository is accessed.
 */
object AppServiceLocator {

    private lateinit var _tokenManager: TokenManager
    val tokenManager: TokenManager get() = _tokenManager

    private lateinit var _application: Application
    /** Application context — used by ViewModels that need a Context (e.g. IotViewModel). */
    val application: Application get() = _application

    /** DataStore-backed app-level cache for contacts, settings, history, FAQs, user name. */
    val appCache: AppCache by lazy { AppCache(_application.applicationContext) }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            RetrofitClient.createService<AuthApiService>(_tokenManager),
            _tokenManager
        )
    }

    val userRepository: UserRepository by lazy {
        UserRepository(
            userApi  = RetrofitClient.createService<UserApiService>(_tokenManager),
            appCache = appCache
        )
    }

    val sosRepository: SosRepository by lazy {
        SosRepository(
            sosApi   = RetrofitClient.createService<SosApiService>(_tokenManager),
            appCache = appCache
        )
    }

    val contactsRepository: ContactsRepository by lazy {
        ContactsRepository(
            contactsApi = RetrofitClient.createService<ContactsApiService>(_tokenManager),
            appCache    = appCache
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            settingsApi = RetrofitClient.createService<SettingsApiService>(_tokenManager),
            appCache    = appCache
        )
    }

    val protectionRepository: ProtectionRepository by lazy {
        ProtectionRepository(RetrofitClient.createService<ProtectionApiService>(_tokenManager))
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepository(RetrofitClient.createService<LocationApiService>(_tokenManager))
    }

    val supportRepository: SupportRepository by lazy {
        SupportRepository(
            supportApi = RetrofitClient.createService<SupportApiService>(_tokenManager),
            appCache   = appCache
        )
    }

    val deviceRepository: DeviceRepository by lazy {
        DeviceRepository(RetrofitClient.createService<DeviceApiService>(_tokenManager))
    }

    fun init(context: Context) {
        _tokenManager = TokenManager(context.applicationContext)
        _application  = context.applicationContext as Application
    }
}
