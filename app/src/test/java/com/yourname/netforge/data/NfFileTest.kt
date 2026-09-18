package com.yourname.netforge.data

import com.yourname.netforge.data.file.NfFileReader
import com.yourname.netforge.data.file.NfFileWriter
import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.domain.model.DnsConfig
import com.yourname.netforge.domain.model.Payload
import com.yourname.netforge.domain.model.TunnelMode
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class NfFileTest {

    @Test
    fun testNfFileWriteAndReadRoundtrip() {
        val originalConfig = Config(
            name = "Test SSH Roundtrip",
            version = 1,
            host = "203.0.113.10",
            port = 2222,
            payload = Payload(
                mode = TunnelMode.SSH,
                sni = "test.domain.com",
                rawPayload = "CONNECT [host_port] HTTP/1.1[crlf]Host: [host_port][crlf][crlf]",
                sshUser = "testuser",
                sshPass = "testpass",
                proxyHost = "10.0.0.1",
                proxyPort = 8080
            ),
            dns = DnsConfig("1.1.1.1", "8.8.8.8", true),
            keepalive = 45,
            mtu = 1420,
            udp = true
        )

        val passphrase = "testPassphrase123".toCharArray()

        val outputStream = ByteArrayOutputStream()
        NfFileWriter.writeEncryptedNf(originalConfig, passphrase, outputStream)

        val fileBytes = outputStream.toByteArray()
        assertTrue("Output should contain header + encrypted payload", fileBytes.size > 52)

        // Read and decrypt
        val inputStream = ByteArrayInputStream(fileBytes)
        val decryptedConfig = NfFileReader.readEncryptedNf(inputStream, passphrase)

        assertEquals(originalConfig.name, decryptedConfig.name)
        assertEquals(originalConfig.host, decryptedConfig.host)
        assertEquals(originalConfig.port, decryptedConfig.port)
        assertEquals(originalConfig.payload.mode, decryptedConfig.payload.mode)
        assertEquals(originalConfig.payload.sni, decryptedConfig.payload.sni)
        assertEquals(originalConfig.payload.rawPayload, decryptedConfig.payload.rawPayload)
        assertEquals(originalConfig.payload.sshUser, decryptedConfig.payload.sshUser)
        assertEquals(originalConfig.payload.sshPass, decryptedConfig.payload.sshPass)
        assertEquals(originalConfig.dns.primary, decryptedConfig.dns.primary)
        assertEquals(originalConfig.keepalive, decryptedConfig.keepalive)
        assertEquals(originalConfig.mtu, decryptedConfig.mtu)
        assertEquals(originalConfig.udp, decryptedConfig.udp)
    }

    @Test
    fun testWrongPassphraseFails() {
        val config = Config(name = "Secret Config")
        val correctPass = "rightPassphrase".toCharArray()
        val wrongPass = "wrongPassphrase".toCharArray()

        val out = ByteArrayOutputStream()
        NfFileWriter.writeEncryptedNf(config, correctPass, out)

        val input = ByteArrayInputStream(out.toByteArray())
        assertThrows(NfFileReader.DecryptionFailedException::class.java) {
            NfFileReader.readEncryptedNf(input, wrongPass)
        }
    }

    @Test
    fun testCorruptedHeaderFails() {
        val config = Config(name = "Test Header Corrupt")
        val pass = "pass123".toCharArray()

        val out = ByteArrayOutputStream()
        NfFileWriter.writeEncryptedNf(config, pass, out)
        val bytes = out.toByteArray()

        // Corrupt magic header byte
        bytes[0] = 0x00

        val input = ByteArrayInputStream(bytes)
        assertThrows(NfFileReader.InvalidNfFormatException::class.java) {
            NfFileReader.readEncryptedNf(input, pass)
        }
    }

    @Test
    fun testGenerateSampleEncryptedFile() {
        // Generates sample_encrypted.nf in project root using passphrase: "netforge2024"
        val sampleConfig = Config(
            name = "Sample Cloud SSH",
            version = 1,
            host = "198.51.100.1",
            port = 443,
            payload = Payload(
                mode = TunnelMode.SSH,
                sni = "cdn.cloudflare.net",
                rawPayload = "GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf][crlf]",
                sshUser = "netforge_user",
                sshPass = "demo_password"
            ),
            dns = DnsConfig(primary = "1.1.1.1", secondary = "8.8.8.8", customEnabled = true),
            keepalive = 60,
            mtu = 1500,
            udp = false
        )

        val pass = "netforge2024".toCharArray()
        val targetFile = File("../sample_encrypted.nf")
        val outStream = if (targetFile.parentFile?.exists() == true) {
            FileOutputStream(targetFile)
        } else {
            FileOutputStream("sample_encrypted.nf")
        }
        outStream.use { stream ->
            NfFileWriter.writeEncryptedNf(sampleConfig, pass, stream)
        }
        assertTrue(File("sample_encrypted.nf").exists() || targetFile.exists())
    }
}
