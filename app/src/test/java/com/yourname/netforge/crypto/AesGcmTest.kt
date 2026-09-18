package com.yourname.netforge.crypto

import org.junit.Assert.*
import org.junit.Test
import java.util.Arrays
import javax.crypto.AEADBadTagException

class AesGcmTest {

    @Test
    fun testEncryptDecryptRoundtrip() {
        val original = "Hello NetForge Clean-Room Security 2024!".toByteArray(Charsets.UTF_8)
        val key = AesGcm.generateRandomBytes(32)
        val nonce = AesGcm.generateRandomBytes(12)
        val aad = "header-aad".toByteArray(Charsets.UTF_8)

        val encrypted = AesGcm.encrypt(original, key, nonce, aad)
        assertNotNull(encrypted)
        assertTrue(encrypted.size > original.size)

        val decrypted = AesGcm.decrypt(encrypted, key, nonce, aad)
        assertArrayEquals(original, decrypted)
    }

    @Test
    fun testWrongKeyThrowsException() {
        val original = "Sensitive payload data".toByteArray(Charsets.UTF_8)
        val key = AesGcm.generateRandomBytes(32)
        val wrongKey = AesGcm.generateRandomBytes(32)
        val nonce = AesGcm.generateRandomBytes(12)
        val aad = "auth-data".toByteArray(Charsets.UTF_8)

        val encrypted = AesGcm.encrypt(original, key, nonce, aad)

        assertThrows(Exception::class.java) {
            AesGcm.decrypt(encrypted, wrongKey, nonce, aad)
        }
    }

    @Test
    fun testWrongNonceThrowsException() {
        val original = "Sensitive payload data".toByteArray(Charsets.UTF_8)
        val key = AesGcm.generateRandomBytes(32)
        val nonce = AesGcm.generateRandomBytes(12)
        val wrongNonce = AesGcm.generateRandomBytes(12)
        val aad = "auth-data".toByteArray(Charsets.UTF_8)

        val encrypted = AesGcm.encrypt(original, key, nonce, aad)

        assertThrows(Exception::class.java) {
            AesGcm.decrypt(encrypted, key, wrongNonce, aad)
        }
    }

    @Test
    fun testModifiedCiphertextThrowsTagVerificationException() {
        val original = "Test tamper resistance".toByteArray(Charsets.UTF_8)
        val key = AesGcm.generateRandomBytes(32)
        val nonce = AesGcm.generateRandomBytes(12)
        val aad = "aad".toByteArray(Charsets.UTF_8)

        val encrypted = AesGcm.encrypt(original, key, nonce, aad)

        // Tamper with one byte in the ciphertext
        encrypted[0] = (encrypted[0].toInt() xor 0xFF).toByte()

        assertThrows(Exception::class.java) {
            AesGcm.decrypt(encrypted, key, nonce, aad)
        }
    }

    @Test
    fun testGzipGunzipRoundtrip() {
        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(20)
        val bytes = text.toByteArray(Charsets.UTF_8)
        val compressed = AesGcm.gzip(bytes)

        assertTrue("Gzip should compress repeating text", compressed.size < bytes.size)

        val decompressed = AesGcm.gunzip(compressed)
        assertArrayEquals(bytes, decompressed)
    }

    @Test
    fun testZeroWipe() {
        val secret = byteArrayOf(1, 2, 3, 4, 5)
        AesGcm.wipe(secret)
        assertTrue(secret.all { it == 0.toByte() })
    }
}
