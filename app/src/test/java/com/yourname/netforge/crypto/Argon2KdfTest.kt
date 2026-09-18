package com.yourname.netforge.crypto

import org.junit.Assert.*
import org.junit.Test

class Argon2KdfTest {

    @Test
    fun testArgon2idDeterminism() {
        val passphrase = "netforge2024".toCharArray()
        val salt = "sixteenbytesalt1".toByteArray(Charsets.UTF_8)

        val key1 = Argon2Kdf.deriveKey(passphrase, salt)
        val key2 = Argon2Kdf.deriveKey(passphrase, salt)

        assertEquals(32, key1.size)
        assertEquals(32, key2.size)
        assertArrayEquals("Same passphrase and salt must produce identical key", key1, key2)

        AesGcm.wipe(key1)
        AesGcm.wipe(key2)
    }

    @Test
    fun testDifferentSaltProducesDifferentKey() {
        val passphrase = "securepassphrase".toCharArray()
        val salt1 = "sixteenbytesalt1".toByteArray(Charsets.UTF_8)
        val salt2 = "sixteenbytesalt2".toByteArray(Charsets.UTF_8)

        val key1 = Argon2Kdf.deriveKey(passphrase, salt1)
        val key2 = Argon2Kdf.deriveKey(passphrase, salt2)

        assertFalse("Different salts must produce distinct keys", key1.contentEquals(key2))

        AesGcm.wipe(key1)
        AesGcm.wipe(key2)
    }

    @Test
    fun testDifferentPassphraseProducesDifferentKey() {
        val passphrase1 = "passwordOne".toCharArray()
        val passphrase2 = "passwordTwo".toCharArray()
        val salt = "constantsalt1234".toByteArray(Charsets.UTF_8)

        val key1 = Argon2Kdf.deriveKey(passphrase1, salt)
        val key2 = Argon2Kdf.deriveKey(passphrase2, salt)

        assertFalse("Different passphrases must produce distinct keys", key1.contentEquals(key2))

        AesGcm.wipe(key1)
        AesGcm.wipe(key2)
    }
}
