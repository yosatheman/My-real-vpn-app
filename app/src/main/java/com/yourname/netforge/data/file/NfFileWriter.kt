package com.yourname.netforge.data.file

import com.yourname.netforge.crypto.AesGcm
import com.yourname.netforge.crypto.Argon2Kdf
import com.yourname.netforge.domain.model.Config
import java.io.OutputStream
import java.util.Arrays

object NfFileWriter {

    fun writeEncryptedNf(
        config: Config,
        passphrase: CharArray,
        outputStream: OutputStream
    ) {
        val iniString = config.toIniString()
        val plaintextBytes = iniString.toByteArray(Charsets.UTF_8)
        val gzipBytes = AesGcm.gzip(plaintextBytes)

        val salt = AesGcm.generateRandomBytes(16)
        val nonce = AesGcm.generateRandomBytes(12)

        val version: Byte = 0x01
        val kdfId: Byte = 0x01
        val cipherId: Byte = 0x01
        val reserved: Byte = 0x00

        val aad = byteArrayOf(
            NfFileReader.MAGIC[0],
            NfFileReader.MAGIC[1],
            NfFileReader.MAGIC[2],
            NfFileReader.MAGIC[3],
            version,
            kdfId,
            cipherId
        )

        var derivedKey: ByteArray? = null
        var ciphertextWithTag: ByteArray? = null

        try {
            derivedKey = Argon2Kdf.deriveKey(passphrase, salt)
            ciphertextWithTag = AesGcm.encrypt(gzipBytes, derivedKey, nonce, aad)

            val header = ByteArray(NfFileReader.HEADER_SIZE)
            // Magic [0..3]
            System.arraycopy(NfFileReader.MAGIC, 0, header, 0, 4)
            // Version [4]
            header[4] = version
            // Kdf id [5]
            header[5] = kdfId
            // Cipher id [6]
            header[6] = cipherId
            // Reserved [7]
            header[7] = reserved
            // Salt [8..23]
            System.arraycopy(salt, 0, header, 8, 16)
            // Nonce [24..35]
            System.arraycopy(nonce, 0, header, 24, 12)

            outputStream.write(header)
            outputStream.write(ciphertextWithTag)
            outputStream.flush()
        } finally {
            AesGcm.wipe(derivedKey)
            AesGcm.wipe(ciphertextWithTag)
            AesGcm.wipe(plaintextBytes)
            Arrays.fill(salt, 0.toByte())
            Arrays.fill(nonce, 0.toByte())
        }
    }
}
