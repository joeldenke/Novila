package se.novila.data

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * An [Aead] every DataStore file is sealed with. Kept behind an interface so tests can hand the stores a
 * plain Tink key instead of the phone's Android Keystore, which Robolectric cannot provide.
 */
interface DatastoreCipher {
    /** The cipher to read and write with. The first call may be slow (it opens the Keystore); later ones are cheap. */
    fun aead(): Aead
}

/**
 * The app's cipher: an AES-256-GCM keyset whose master key lives in the Android Keystore, so the key material
 * never leaves the phone and cannot be read from a backup. The keyset is stored in the app's private
 * SharedPreferences ([PREF_FILE_NAME]); both it and the Keystore entry are excluded from Auto Backup
 * (see app/src/main/res/xml), so a restored copy on another phone cannot be decrypted here.
 *
 * One instance for the whole app, so the keyset is created once and the stores share it.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class KeystoreDatastoreCipher(private val context: Context) : DatastoreCipher {
    private val lock = Any()

    @Volatile
    private var created: Aead? = null

    override fun aead(): Aead = created ?: synchronized(lock) {
        created ?: openKeyset().also { created = it }
    }

    private fun openKeyset(): Aead {
        val keyset =
            AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
                .withKeyTemplate(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
        return keyset.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private companion object {
        /** The keyset's name inside [PREF_FILE_NAME]; changing it starts a new keyset. */
        const val KEYSET_NAME = "novila_datastore_keyset"
        /** A private SharedPreferences file that holds only the encrypted keyset. */
        const val PREF_FILE_NAME = "novila_datastore_keys"
        /** The master key in the Android Keystore, named after the app. */
        const val MASTER_KEY_URI = "android-keystore://novila_datastore_master_key"
    }
}
