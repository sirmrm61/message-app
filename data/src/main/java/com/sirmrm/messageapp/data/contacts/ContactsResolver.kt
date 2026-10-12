package com.sirmrm.messageapp.data.contacts

import android.content.Context
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Resolved contact info for a phone number/address. */
data class ContactInfo(
    val displayName: String,
    val photoUri: String?,
)

/** Looks up contact display names and photos by phone number. */
@Singleton
class ContactsResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Returns contact info for [address], or null when unknown/unreadable. */
    fun lookup(address: String): ContactInfo? {
        if (address.isBlank()) return null
        return try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon().appendPath(address).build()
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_URI,
                ),
                null, null, null,
            )?.use { c ->
                if (!c.moveToFirst()) return null
                ContactInfo(
                    displayName = c.getString(0) ?: return null,
                    photoUri = c.getString(1),
                )
            }
        } catch (e: SecurityException) {
            null
        }
    }
}
