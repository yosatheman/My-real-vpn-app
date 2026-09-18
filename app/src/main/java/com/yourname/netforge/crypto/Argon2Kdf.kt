package com.yourname.netforge.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.nio.charset.StandardCharsets

object Argon2Kdf {

    private const val ITERATIONS = 3
    private const val MEMORY_KB = 65536 // 64 MB
    private const val PARALLELISM = 2
    private const val OUTPUT_LENGTH = 32 // 256 bits

    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = ITERATIONS,
        memoryKb: Int = MEMORY_KB,
        parallelism: Int = PARALLELISM,
        outputLength: Int = OUTPUT_LENGTH
    ): ByteArray {
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(iterations)
            .withMemoryAsKB(memoryKb)
            .withParallelism(parallelism)
            .withSalt(salt)

        val generator = Argon2BytesGenerator()
        generator.init(builder.build())

        val derivedKey = ByteArray(outputLength)
        generator.generateBytes(password, derivedKey)
        return derivedKey
    }

    fun deriveKeyFromBytes(
        passwordBytes: ByteArray,
        salt: ByteArray,
        iterations: Int = ITERATIONS,
        memoryKb: Int = MEMORY_KB,
        parallelism: Int = PARALLELISM,
        outputLength: Int = OUTPUT_LENGTH
    ): ByteArray {
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(iterations)
            .withMemoryAsKB(memoryKb)
            .withParallelism(parallelism)
            .withSalt(salt)

        val generator = Argon2BytesGenerator()
        generator.init(builder.build())

        val derivedKey = ByteArray(outputLength)
        generator.generateBytes(passwordBytes, derivedKey)
        return derivedKey
    }
}
