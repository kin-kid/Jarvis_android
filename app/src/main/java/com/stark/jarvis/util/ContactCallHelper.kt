package com.stark.jarvis.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

data class ContactMatch(
    val displayName: String,
    val phoneNumber: String
)

class ContactCallHelper(private val context: Context) {

    fun findContactsByName(nameQuery: String): List<ContactMatch> {
        val normalized = nameQuery.trim()
        if (normalized.isEmpty()) return emptyList()

        val matches = mutableListOf<ContactMatch>()
        val resolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$normalized%")

        try {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                args,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)?.trim().orEmpty()
                    val number = cursor.getString(numberIndex)?.trim().orEmpty()

                    if (name.isNotEmpty() && number.isNotEmpty()) {
                        matches.add(ContactMatch(name, number))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ContactCallHelper", "Error searching contacts", e)
        }

        return matches
    }

    fun resolveContactAlias(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("mum") || lower.contains("mom") || lower.contains("mother") -> "mum"
            lower.contains("dad") || lower.contains("father") -> "dad"
            lower.contains("brother") || lower.contains("bro") -> "brother"
            lower.contains("sister") -> "sister"
            else -> input
        }
    }

    fun buildDialIntent(phoneNumber: String): Intent {
        val uri = Uri.parse("tel:${Uri.encode(phoneNumber)}")
        return Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildCallIntent(phoneNumber: String): Intent {
        val uri = Uri.parse("tel:${Uri.encode(phoneNumber)}")
        return Intent(Intent.ACTION_CALL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
