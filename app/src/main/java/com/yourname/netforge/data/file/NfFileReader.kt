package com.yourname.netforge.data.file

import com.yourname.netforge.crypto.AesGcm
import com.yourname.netforge.crypto.Argon2Kdf
import com.yourname.netforge.domain.model.Config
import java.io.InputStream
import java.util.Arrays

object NfFileReader {
    val MAGIC = byteArrayOf(0x4E, 0x46, 0x52, 0x47) // "NFRG"
    const val VERSION: Byte = 0x01
    const val KDF_ARGON2ID: Byte = 0x01
    const val CIPHER_AES_256_GCM: Byte = 0x01
    const val HEADER_SIZE = 36
    const val MIN_FILE_SIZE = HEADER_SIZE + 16 // Header + 16-byte GCM tag

    class InvalidNfFormatException(message: String) : Exception(message)
    class DecryptionFailedException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun readEncryptedNf(inputStream: InputStream, passphrase: CharArray): Config {
        val fileBytes = inputStream.readBytes()
        if (fileBytes.size < MIN_FILE_SIZE) {
            throw InvalidNfFormatException("File is too small to be a valid .nf config (${fileBytes.size} bytes)")
        }

        // 1. Verify Magic
        if (fileBytes[0] != MAGIC[0] || fileBytes[1] != MAGIC[1] ||
            fileBytes[2] != MAGIC[2] || fileBytes[3] != MAGIC[3]
        ) {
            throw InvalidNfFormatException("Invalid magic header, expected NFRG")
        }

        val version = fileBytes[4]
        val kdfId = fileBytes[5]
        val cipherId = fileBytes[6]

        if (version != VERSION) {
            throw InvalidNfFormatException("Unsupported .nf version: $version")
        }
        if (kdfId != KDF_ARGON2ID) {
            throw InvalidNfFormatException("Unsupported KDF algorithm ID: $kdfId")
        }
        if (cipherId != CIPHER_AES_256_GCM) {
            throw InvalidNfFormatException("Unsupported Cipher ID: $cipherId")
        }

        // Salt: [8..23] (16 bytes)
        val salt = ByteArray(16)
        System.arraycopy(fileBytes, 8, salt, 0, 16)

        // Nonce: [24..35] (12 bytes)
        val nonce = ByteArray(12)
        System.arraycopy(fileBytes, 24, nonce, 0, 12)

        // Ciphertext with tag: [36..EOF]
        val ciphertextWithTag = ByteArray(fileBytes.size - HEADER_SIZE)
        System.arraycopy(fileBytes, HEADER_SIZE, ciphertextWithTag, 0, ciphertextWithTag.size)

        // AAD = magic || version || kdf_id || cipher_id
        val aad = byteArrayOf(MAGIC[0], MAGIC[1], MAGIC[2], MAGIC[3], version, kdfId, cipherId)

        var derivedKey: ByteArray? = null
        var decryptedGzip: ByteArray? = null
        var plaintextBytes: ByteArray? = null

        try {
            // Key derivation
            derivedKey = Argon2Kdf.deriveKey(passphrase, salt)

            // AES-GCM decrypt + verify auth tag
            decryptedGzip = AesGcm.decrypt(ciphertextWithTag, derivedKey, nonce, aad)

            // Gunzip
            plaintextBytes = AesGcm.gunzip(decryptedGzip)

            val iniString = String(plaintextBytes, Charsets.UTF_8)
            return Config.fromIniString(iniString)
        } catch (e: Exception) {
            throw DecryptionFailedException("Decryption failed. Incorrect passphrase or corrupted file.", e)
        } finally {
            // Zero all sensitive key material
            AesGcm.wipe(derivedKey)
            AesGcm.wipe(decryptedGzip)
            AesGcm.wipe(plaintextBytes)
            Arrays.fill(salt, 0.toByte())
            Arrays.fill(nonce, 0.toByte())
        }
    }
}
