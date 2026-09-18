package com.yourname.netforge.data.repo

import com.yourname.netforge.crypto.KeystoreHelper
import com.yourname.netforge.data.db.ConfigDao
import com.yourname.netforge.data.db.ConfigEntity
import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.domain.model.DnsConfig
import com.yourname.netforge.domain.model.Payload
import com.yourname.netforge.domain.model.TunnelMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConfigRepository(private val dao: ConfigDao) {

    val configsFlow: Flow<List<Config>> = dao.getAllConfigs().map { entities ->
        entities.map { it.toDomain() }
    }

    val activeConfigFlow: Flow<Config?> = dao.getActiveConfigFlow().map { entity ->
        entity?.toDomain()
    }

    suspend fun getAllConfigs(): List<Config> {
        val flow = dao.getAllConfigs()
        var list: List<Config> = emptyList()
        // Quick fetch helper
        return list
    }

    suspend fun getConfigById(id: Long): Config? {
        return dao.getConfigById(id)?.toDomain()
    }

    suspend fun getActiveConfig(): Config? {
        return dao.getActiveConfig()?.toDomain()
    }

    suspend fun saveConfig(config: Config): Long {
        val entity = config.toEntity()
        val id = dao.insertConfig(entity)
        if (config.isActive) {
            dao.clearAllActive()
            dao.setActiveConfig(if (config.id != 0L) config.id else id)
        }
        return id
    }

    suspend fun setActiveConfig(id: Long) {
        dao.clearAllActive()
        dao.setActiveConfig(id)
    }

    suspend fun renameConfig(id: Long, newName: String) {
        dao.renameConfig(id, newName)
    }

    suspend fun deleteConfig(id: Long) {
        dao.deleteConfigById(id)
    }

    suspend fun seedDefaultsIfEmpty() {
        val active = dao.getActiveConfig()
        if (active != null) return

        // Seed initial sample configs
        val sample1 = Config(
            name = "SSH Cloud Fast (Sample)",
            host = "198.51.100.1",
            port = 443,
            payload = Payload(
                mode = TunnelMode.SSH,
                sni = "cloud.example.com",
                rawPayload = "GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf][crlf]",
                sshUser = "netforge_user",
                sshPass = "demo_password"
            ),
            dns = DnsConfig(primary = "1.1.1.1", secondary = "8.8.8.8", customEnabled = true),
            isActive = true
        )

        val sample2 = Config(
            name = "HTTP Connect Proxy (Sample)",
            host = "203.0.113.5",
            port = 8080,
            payload = Payload(
                mode = TunnelMode.HTTP_CONNECT,
                sni = "",
                rawPayload = "CONNECT [host_port] HTTP/1.1[crlf]Host: [host_port][crlf]Proxy-Connection: Keep-Alive[crlf][crlf]",
                proxyHost = "203.0.113.5",
                proxyPort = 8080
            ),
            dns = DnsConfig("8.8.8.8", "8.8.4.4", true),
            isActive = false
        )

        val sample3 = Config(
            name = "SNI Fronting Direct (Sample)",
            host = "104.16.123.96",
            port = 443,
            payload = Payload(
                mode = TunnelMode.SNI,
                sni = "cdn.cloudflare.net",
                rawPayload = "GET / HTTP/1.1[crlf]Host: [front_host][crlf][crlf]"
            ),
            dns = DnsConfig("1.1.1.1", "1.0.0.1", true),
            isActive = false
        )

        saveConfig(sample1)
        saveConfig(sample2)
        saveConfig(sample3)
    }

    private fun Config.toEntity(): ConfigEntity {
        val ini = this.toIniString()
        val encrypted = KeystoreHelper.encryptLocalString(ini)
        return ConfigEntity(
            id = this.id,
            name = this.name,
            version = this.version,
            host = this.host,
            port = this.port,
            mode = this.payload.mode.id,
            encryptedData = encrypted,
            createdAt = this.createdAt,
            isActive = this.isActive
        )
    }

    private fun ConfigEntity.toDomain(): Config {
        val decryptedIni = try {
            KeystoreHelper.decryptLocalString(this.encryptedData)
        } catch (_: Exception) {
            ""
        }
        val domain = if (decryptedIni.isNotBlank()) {
            Config.fromIniString(decryptedIni)
        } else {
            Config(
                name = this.name,
                version = this.version,
                host = this.host,
                port = this.port,
                payload = Payload(mode = TunnelMode.fromString(this.mode))
            )
        }
        return domain.copy(
            id = this.id,
            name = this.name,
            createdAt = this.createdAt,
            isActive = this.isActive
        )
    }
}
