package se.novila.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException

/**
 * The bytes every encrypted DataStore file starts with, so a file can be told apart from the plaintext one
 * an older version of the app wrote. `NVLDST` and a version byte; a future change to the format gets a new one.
 */
private val MAGIC = byteArrayOf(0x4E, 0x56, 0x4C, 0x44, 0x53, 0x54, 0x01)

/**
 * Seals a Preferences DataStore file with Tink ([AeadSerializer]), behind a header so the app can still read the
 * plaintext file an older version wrote.
 *
 * Preferences DataStore keeps the same file name across the move to encryption, so on the first start after an
 * update the file on disk is plaintext and has to be read as such; the next write stores it encrypted. The header,
 * not a successful decrypt, is what decides: a decrypt failure on an encrypted file is a [CorruptionException] the
 * caller's corruption handler replaces with empty data, as an unreadable file used to.
 *
 * [aead] is a supplier, not an [Aead], so opening the Android Keystore happens on DataStore's IO thread the first
 * time a file is read or written, never while the store is being created.
 */
internal class EncryptingPreferencesSerializer(
    aead: () -> Aead,
    associatedData: ByteArray,
) : Serializer<Preferences> {
    private val encrypted = AeadSerializer(LazyAead(aead), PreferencesFileSerializer, associatedData)

    override val defaultValue: Preferences = emptyPreferences()

    override suspend fun readFrom(input: InputStream): Preferences {
        val bytes = input.readBytes()
        if (!bytes.startsWith(MAGIC)) return PreferencesFileSerializer.readFrom(bytes.inputStream())
        val sealed = bytes.copyOfRange(MAGIC.size, bytes.size)
        return try {
            encrypted.readFrom(sealed.inputStream())
        } catch (e: GeneralSecurityException) {
            throw CorruptionException("The encrypted DataStore could not be decrypted", e)
        }
    }

    override suspend fun writeTo(t: Preferences, output: OutputStream) {
        val sealed = ByteArrayOutputStream()
        encrypted.writeTo(t, sealed)
        output.write(MAGIC)
        output.write(sealed.toByteArray())
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}

/**
 * An [Aead] that opens the real one on first use, so the Keystore is touched on DataStore's IO thread rather than
 * while a store is constructed.
 */
private class LazyAead(private val aead: () -> Aead) : Aead {
    private val delegate: Aead by lazy(aead)

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        delegate.encrypt(plaintext, associatedData)

    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
        delegate.decrypt(ciphertext, associatedData)
}
