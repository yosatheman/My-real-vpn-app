package com.yourname.netforge

import android.app.Application
import com.yourname.netforge.data.db.NetForgeDatabase
import com.yourname.netforge.data.prefs.SecurePrefs
import com.yourname.netforge.data.repo.ConfigRepository
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class NetForgeApp : Application() {

    lateinit var database: NetForgeDatabase
        private set

    lateinit var configRepository: ConfigRepository
        private set

    lateinit var securePrefs: SecurePrefs
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Register BouncyCastle Security Provider cleanly
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        database = NetForgeDatabase.getDatabase(this)
        configRepository = ConfigRepository(database.configDao())
        securePrefs = SecurePrefs(this)
    }

    companion object {
        lateinit var instance: NetForgeApp
            private set
    }
}
