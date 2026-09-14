package com.example.expensetracker.frontend.important

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

// ─── Data model (lives in `important` package) ────────────────────────────────
// ContactSheet.kt maps this to its own UI-level Contact via toUiContact().

data class Contact(
    val id: String,
    val name: String,
    val phones: List<String>,
    val emails: List<String>,
    val photoUri: Uri?
)

// ─── Fetch all device contacts ────────────────────────────────────────────────

fun fetchContacts(context: Context): List<com.example.expensetracker.frontend.important.Contact> {

    // Permission guard — returns empty list instead of crashing with SecurityException
    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) return emptyList()

    val contacts = mutableListOf<Contact>()

    val contactCursor = context.contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
        ),
        null,
        null,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC"
    ) ?: return emptyList()

    contactCursor.use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getString(
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            )

            val name = cursor.getString(
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            ) ?: "No Name"

            val photoUriStr = cursor.getString(
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
            )
            val photoUri: Uri? = photoUriStr?.toUri()

            val hasPhone = cursor.getInt(
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)
            )

            // ── Phone numbers ─────────────────────────────────────────
            val phones = mutableListOf<String>()
            if (hasPhone > 0) {
                val phoneCursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )
                phoneCursor?.use { pc ->
                    while (pc.moveToNext()) {
                        val number = pc.getString(
                            pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                        if (!number.isNullOrBlank()) phones.add(number)
                    }
                }
            }

            // ── Email addresses ──────────────────────────────────────
            val emails = mutableListOf<String>()
            val emailCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(id),
                null
            )
            emailCursor?.use { ec ->
                while (ec.moveToNext()) {
                    val address = ec.getString(
                        ec.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    )
                    if (!address.isNullOrBlank()) emails.add(address)
                }
            }

            // Only include contacts that have at least a phone number
            if (phones.isNotEmpty()) {
                contacts.add(Contact(id, name, phones, emails, photoUri))
            }
        }
    }
    return contacts
}

//fun insertContact(context: Context, name: String, number: String){
//    val ops = arrayListOf<ContentProviderOperation>()
//
//    // create a raw contact
//    ops.add(
//        ContentProviderOperation.newInsert(
//            ContactsContract.RawContacts.CONTENT_URI
//        )
//            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE,null)
//            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME,null)
//            .build()
//    )
//
//    // insert display name
//    ops.add(
//        ContentProviderOperation.newInsert(
//            ContactsContract.Data.CONTENT_URI
//        )
//            .withValue(
//                ContactsContract.Data.MIMETYPE,
//                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
//            )
//            .withValue(
//                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
//                name
//            )
//            .build()
//    )
//
//    //insert phone number
//    ops.add(
//        ContentProviderOperation.newInsert(
//            ContactsContract.Data.CONTENT_URI,
//        )
//            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,0)
//            .withValue(
//                ContactsContract.Data.MIMETYPE,
//                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
//            )
//            .withValue(
//                ContactsContract.CommonDataKinds.Phone.TYPE,
//                number
//            )
//            .withValue(
//                ContactsContract.CommonDataKinds.Phone.TYPE,
//                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
//            )
//            .build()
//    )
//
//    try {
//        context.contentResolver.applyBatch(ContactsContract.AUTHORITY,ops)
//    }
//    catch (e: Exception){
//        e.printStackTrace()
//    }
//}