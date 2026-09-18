package com.yourname.netforge.crypto

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Arrays
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesGcm {
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ALGORITHM = "AES"

    val secureRandom = SecureRandom()

    fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    fun encrypt(
        plaintext: ByteArray,
        keyBytes: ByteArray,
        nonce: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        val secretKey: SecretKey = SecretKeySpec(keyBytes, ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        if (aad != null && aad.isNotEmpty()) {
            cipher.updateAAD(aad)
        }
        return cipher.doFinal(plaintext)
    }

    fun decrypt(
        ciphertextWithTag: ByteArray,
        keyBytes: ByteArray,
        nonce: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        val secretKey: SecretKey = SecretKeySpec(keyBytes, ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        if (aad != null && aad.isNotEmpty()) {
            cipher.updateAAD(aad)
        }
        return cipher.doFinal(ciphertextWithTag)
    }

    fun gzip(input: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(input) }
        return bos.toByteArray()
    }

    fun gunzip(input: ByteArray): ByteArray {
        val bis = ByteArrayInputStream(input)
        val bos = ByteArrayOutputStream()
        GZIPInputStream(bis).use { it.copyTo(bos) }
        return bos.toByteArray()
    }

    fun wipe(byteArray: ByteArray?) {
        if (byteArray != null) {
            Arrays.fill(byteArray, 0.toByte())
        }
    }

    fun wipe(charArray: CharArray?) {
        if (charArray != null) {
            Arrays.fill(charArray, '\u0000')
        }
    }
}
