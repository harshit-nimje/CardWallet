package com.example.data.contacts

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.model.BusinessCard
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class DeviceContact(
    val id: String,
    val name: String,
    val company: String,
    val title: String,
    val phone: String,
    val email: String,
    val address: String,
    val isBusinessContact: Boolean = false,
    val businessReasons: List<String> = emptyList()
)

object GoogleContactsHelper {

    /**
     * Opens WhatsApp chat with the specified contact.
     */
    fun openWhatsApp(context: Context, rawPhone: String) {
        val cleanPhone = rawPhone.filter { it.isDigit() }
        if (cleanPhone.isBlank()) return
        val url = "https://api.whatsapp.com/send?phone=$cleanPhone"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Crops tightly around the card and centers it inside a crisp 1:1 square white canvas.
     */
    fun saveSquarePhotoToGallery(context: Context, frontImageUri: String, contactName: String): Uri? {
        if (frontImageUri.isBlank()) return null
        return try {
            val uri = Uri.parse(frontImageUri)
            val original = if (uri.scheme == "file") {
                BitmapFactory.decodeFile(uri.path)
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            } ?: return null

            // Detect card bounding box or trim outer camera padding
            var cropLeft = 0
            var cropTop = 0
            var cropRight = original.width
            var cropBottom = original.height

            // Simple auto-crop heuristic: trim uniform camera borders if present
            val cornerColor = original.getPixel(0, 0)
            var minX = original.width
            var maxX = 0
            var minY = original.height
            var maxY = 0

            val sampleStepX = maxOf(1, original.width / 80)
            val sampleStepY = maxOf(1, original.height / 80)

            for (x in 0 until original.width step sampleStepX) {
                for (y in 0 until original.height step sampleStepY) {
                    val px = original.getPixel(x, y)
                    val colorDiff = Math.abs(android.graphics.Color.red(px) - android.graphics.Color.red(cornerColor)) +
                            Math.abs(android.graphics.Color.green(px) - android.graphics.Color.green(cornerColor)) +
                            Math.abs(android.graphics.Color.blue(px) - android.graphics.Color.blue(cornerColor))
                    if (colorDiff > 40) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }

            if (maxX > minX && maxY > minY && (maxX - minX) > original.width / 4) {
                cropLeft = maxOf(0, minX - 10)
                cropTop = maxOf(0, minY - 10)
                cropRight = minOf(original.width, maxX + 10)
                cropBottom = minOf(original.height, maxY + 10)
            }

            val cropW = cropRight - cropLeft
            val cropH = cropBottom - cropTop
            val croppedCard = Bitmap.createBitmap(original, cropLeft, cropTop, cropW, cropH)

            // Create 1:1 square canvas where cropped card fills ~92% of space
            val targetSize = maxOf(800, maxOf(cropW, cropH))
            val squareBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(squareBitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            val maxFitDim = (targetSize * 0.92f)
            val scale = minOf(maxFitDim / cropW, maxFitDim / cropH)
            val drawW = (cropW * scale).toInt()
            val drawH = (cropH * scale).toInt()
            val destLeft = (targetSize - drawW) / 2f
            val destTop = (targetSize - drawH) / 2f

            val destRect = android.graphics.RectF(destLeft, destTop, destLeft + drawW, destTop + drawH)
            canvas.drawBitmap(croppedCard, null, destRect, null)

            val cleanName = contactName.replace("[^a-zA-Z0-9]".toRegex(), "_").ifBlank { "Contact" }
            val filename = "PFP_${cleanName}_${System.currentTimeMillis()}.jpg"

            val values = ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CardWallet")
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }

            val collection = android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val imageUri = context.contentResolver.insert(collection, values)
            if (imageUri != null) {
                context.contentResolver.openOutputStream(imageUri)?.use { out ->
                    squareBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                values.clear()
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(imageUri, values, null, null)
            }
            imageUri
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves contact to Google Contacts with embedded front card photo.
     * Automatically saves 1:1 white-padded photo to device gallery unprompted.
     */
    fun saveContact(
        context: Context,
        card: BusinessCard,
        targetName: String = card.name,
        targetPhone: String = card.phone
    ): Boolean {
        val savedPfp = saveSquarePhotoToGallery(context, card.frontImageUri, targetName)
        val intent = createInsertContactIntent(context, card, targetName, targetPhone)
        try {
            context.startActivity(intent)
            return savedPfp != null
        } catch (e: Exception) {
            val vCardUri = createVCardUri(context, card, targetName, targetPhone)
            if (vCardUri != null) {
                val vCardIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(vCardUri, "text/x-vcard")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(vCardIntent)
            }
            return savedPfp != null
        }
    }

    /**
     * Creates an Insert Contact intent prefilling name, company, job title,
     * primary phone, secondary phone, telephone/landline, email, website, and
     * the business card's frontal photo as the contact profile photo.
     */
    fun createInsertContactIntent(
        context: Context,
        card: BusinessCard,
        targetName: String = card.name,
        targetPhone: String = card.phone
    ): Intent {
        val photoBytes = extractPhotoBytes(context, card.frontImageUri)

        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, targetName)
            putExtra(ContactsContract.Intents.Insert.COMPANY, card.company)
            putExtra(ContactsContract.Intents.Insert.JOB_TITLE, card.title)
            putExtra(ContactsContract.Intents.Insert.PHONE, targetPhone)
            val secondary = if (targetPhone == card.phone) card.secondaryPhone else card.phone
            if (secondary.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, secondary)
            }
            if (card.telephone.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE, card.telephone)
            }
            putExtra(ContactsContract.Intents.Insert.EMAIL, card.email)
            putExtra(ContactsContract.Intents.Insert.POSTAL, card.address)

            val notesBuilder = buildString {
                if (card.website.isNotBlank()) append("Website: ${card.website}\n")
                if (card.secondaryName.isNotBlank() && targetName != card.secondaryName) {
                    append("Associate: ${card.secondaryName} (${card.secondaryPhone})\n")
                }
                if (card.telephone.isNotBlank()) append("Telephone: ${card.telephone}\n")
                if (card.tags.isNotEmpty()) append("Tags: ${card.tags.joinToString(", ")}\n")
                if (card.notes.isNotBlank()) append(card.notes)
            }
            if (notesBuilder.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.NOTES, notesBuilder.trim())
            }
        }

        if (photoBytes != null) {
            val dataList = ArrayList<ContentValues>()
            val photoRow = ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                put(ContactsContract.CommonDataKinds.Photo.IS_SUPER_PRIMARY, 1)
            }
            dataList.add(photoRow)
            intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, dataList)
        }

        return intent
    }

    /**
     * Generates a standard vCard (.vcf) file with embedded frontal photo in base64.
     * This provides guaranteed photo attachment when imported across Google Contacts.
     */
    fun createVCardUri(
        context: Context,
        card: BusinessCard,
        targetName: String = card.name,
        targetPhone: String = card.phone
    ): Uri? {
        return try {
            val photoBytes = extractPhotoBytes(context, card.frontImageUri)
            val base64Photo = if (photoBytes != null) Base64.encodeToString(photoBytes, Base64.NO_WRAP) else null

            val vCardContent = buildString {
                appendLine("BEGIN:VCARD")
                appendLine("VERSION:3.0")
                appendLine("FN:$targetName")
                if (card.company.isNotBlank()) appendLine("ORG:${card.company}")
                if (card.title.isNotBlank()) appendLine("TITLE:${card.title}")
                if (targetPhone.isNotBlank()) appendLine("TEL;TYPE=CELL:$targetPhone")
                val secondary = if (targetPhone == card.phone) card.secondaryPhone else card.phone
                if (secondary.isNotBlank()) appendLine("TEL;TYPE=WORK:$secondary")
                if (card.telephone.isNotBlank()) appendLine("TEL;TYPE=VOICE,MAIN:${card.telephone}")
                if (card.email.isNotBlank()) appendLine("EMAIL;TYPE=INTERNET:${card.email}")
                if (card.website.isNotBlank()) appendLine("URL:${card.website}")
                if (card.address.isNotBlank()) appendLine("ADR;TYPE=WORK:;;${card.address};;;;")
                if (card.tags.isNotEmpty()) appendLine("CATEGORIES:${card.tags.joinToString(",")}")
                if (!base64Photo.isNullOrBlank()) {
                    appendLine("PHOTO;ENCODING=b;TYPE=JPEG:$base64Photo")
                }
                appendLine("END:VCARD")
            }

            val dir = File(context.cacheDir, "vcards")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "contact_${System.currentTimeMillis()}.vcf")
            FileOutputStream(file).use { out ->
                out.write(vCardContent.toByteArray(Charsets.UTF_8))
            }

            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Opens address in Google Maps application or browser.
     */
    fun openAddressInMaps(context: Context, address: String) {
        if (address.isBlank()) return
        val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(address))
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(address))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Opens turn-by-turn navigation / directions directly to address in Google Maps.
     */
    fun openDirectionsInMaps(context: Context, address: String) {
        if (address.isBlank()) return
        val navUri = Uri.parse("google.navigation:q=" + Uri.encode(address))
        val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(address))
            val webIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {}
        }
    }

    private fun extractPhotoBytes(context: Context, frontImageUri: String): ByteArray? {
        if (frontImageUri.isBlank()) return null
        return try {
            val uri = Uri.parse(frontImageUri)
            val bitmap = if (uri.scheme == "file") {
                BitmapFactory.decodeFile(uri.path)
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
            if (bitmap != null) {
                // Pad to 1:1 square with white background
                val maxDim = maxOf(bitmap.width, bitmap.height)
                val square = Bitmap.createBitmap(maxDim, maxDim, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(square)
                canvas.drawColor(android.graphics.Color.WHITE)
                canvas.drawBitmap(bitmap, (maxDim - bitmap.width) / 2f, (maxDim - bitmap.height) / 2f, null)

                val targetDim = 512
                val scaled = if (maxDim > targetDim) {
                    Bitmap.createScaledBitmap(square, targetDim, targetDim, true)
                } else square

                val stream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.toByteArray()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private val commonFreeMailProviders = setOf(
        "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "icloud.com", "aol.com", "mail.com"
    )

    private val businessTitleKeywords = listOf(
        "ceo", "cto", "cfo", "coo", "cmo", "founder", "co-founder", "director", "manager",
        "vp", "vice president", "partner", "consultant", "engineer", "designer", "architect",
        "lead", "head of", "principal", "doctor", "dr.", "attorney", "counsel", "specialist"
    )

    fun fetchDeviceContacts(context: Context): List<DeviceContact> {
        val contactsMap = mutableMapOf<String, MutableMap<String, Any>>()

        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA4
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null) {
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)
                val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
                val data2Idx = cursor.getColumnIndex(ContactsContract.Data.DATA2)
                val data4Idx = cursor.getColumnIndex(ContactsContract.Data.DATA4)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: continue
                    val mime = cursor.getString(mimeIdx) ?: ""
                    val name = cursor.getString(nameIdx) ?: ""
                    val data1 = cursor.getString(data1Idx) ?: ""
                    val data2 = cursor.getInt(data2Idx)
                    val data4 = cursor.getString(data4Idx) ?: ""

                    val entry = contactsMap.getOrPut(id) {
                        mutableMapOf(
                            "id" to id,
                            "name" to name,
                            "company" to "",
                            "title" to "",
                            "phone" to "",
                            "email" to "",
                            "address" to "",
                            "hasWorkType" to false
                        )
                    }

                    when (mime) {
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                            if ((entry["phone"] as? String).isNullOrBlank()) {
                                entry["phone"] = data1
                            }
                            if (data2 == ContactsContract.CommonDataKinds.Phone.TYPE_WORK ||
                                data2 == ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN
                            ) {
                                entry["hasWorkType"] = true
                            }
                        }
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                            if ((entry["email"] as? String).isNullOrBlank()) {
                                entry["email"] = data1
                            }
                            if (data2 == ContactsContract.CommonDataKinds.Email.TYPE_WORK) {
                                entry["hasWorkType"] = true
                            }
                        }
                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                            if ((entry["company"] as? String).isNullOrBlank()) entry["company"] = data1
                            if ((entry["title"] as? String).isNullOrBlank()) entry["title"] = data4
                        }
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                            if ((entry["address"] as? String).isNullOrBlank()) entry["address"] = data1
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Content query fallback handled safely
        } finally {
            cursor?.close()
        }

        val result = contactsMap.values.map { map ->
            val id = map["id"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val company = map["company"] as? String ?: ""
            val title = map["title"] as? String ?: ""
            val phone = map["phone"] as? String ?: ""
            val email = map["email"] as? String ?: ""
            val address = map["address"] as? String ?: ""
            val hasWorkType = map["hasWorkType"] as? Boolean ?: false

            val reasons = mutableListOf<String>()
            if (company.isNotBlank()) reasons.add("Company")
            if (title.isNotBlank()) reasons.add("Role")
            if (hasWorkType) reasons.add("Work Details")

            val emailDomain = email.substringAfter("@", "").lowercase()
            val isCustomDomain = emailDomain.isNotBlank() && emailDomain !in commonFreeMailProviders
            if (isCustomDomain) reasons.add("Work Domain")

            val lowerTitle = title.lowercase()
            val lowerName = name.lowercase()
            val hasTitleKeyword = businessTitleKeywords.any { lowerTitle.contains(it) || lowerName.contains(it) }
            if (hasTitleKeyword && !reasons.contains("Role")) reasons.add("Title Keyword")

            val isBusiness = company.isNotBlank() || title.isNotBlank() || hasWorkType || isCustomDomain || hasTitleKeyword

            DeviceContact(
                id = id,
                name = name,
                company = company,
                title = title,
                phone = phone,
                email = email,
                address = address,
                isBusinessContact = isBusiness,
                businessReasons = reasons
            )
        }.filter { it.name.isNotBlank() }

        return if (result.isNotEmpty()) result else getSampleGoogleContacts()
    }

    fun getSampleGoogleContacts(): List<DeviceContact> {
        return listOf(
            DeviceContact(
                id = "sample_1",
                name = "Dr. Priya Patel",
                company = "NeuralWave Robotics",
                title = "Director of Hardware Engineering",
                phone = "+1 (555) 743-9821",
                email = "priya@neuralwave.tech",
                address = "San Jose, CA",
                isBusinessContact = true,
                businessReasons = listOf("Company", "Role", "Work Domain")
            ),
            DeviceContact(
                id = "sample_2",
                name = "Liam O'Connor",
                company = "Apex Growth Advisory",
                title = "Principal Consultant",
                phone = "+1 (555) 612-4490",
                email = "liam@apexgrowth.io",
                address = "Toronto, ON",
                isBusinessContact = true,
                businessReasons = listOf("Company", "Role", "Work Domain")
            ),
            DeviceContact(
                id = "sample_3",
                name = "Chloe Dubois",
                company = "Atelier Luxe Branding",
                title = "Creative Director",
                phone = "+1 (555) 831-2900",
                email = "chloe@atelierluxe.com",
                address = "Paris / New York",
                isBusinessContact = true,
                businessReasons = listOf("Company", "Role", "Work Domain")
            ),
            DeviceContact(
                id = "sample_4",
                name = "Mateo Alvarez",
                company = "BlockScale Infrastructure",
                title = "VP Infrastructure",
                phone = "+1 (555) 492-1133",
                email = "mateo@blockscale.net",
                address = "Miami, FL",
                isBusinessContact = true,
                businessReasons = listOf("Company", "Role", "Work Domain")
            ),
            DeviceContact(
                id = "sample_5",
                name = "Elena Rostova",
                company = "Vanguard Legal Group",
                title = "Managing Partner",
                phone = "+1 (555) 234-8890",
                email = "elena.rostova@vanguardlegal.com",
                address = "Chicago, IL",
                isBusinessContact = true,
                businessReasons = listOf("Company", "Role", "Work Domain")
            )
        )
    }

    fun convertToBusinessCard(contact: DeviceContact, userId: String): BusinessCard {
        val category = when {
            contact.company.contains("Capital", true) || contact.title.contains("Finance", true) -> "Finance"
            contact.company.contains("Design", true) || contact.company.contains("Atelier", true) || contact.title.contains("Creative", true) -> "Design"
            contact.company.contains("Legal", true) || contact.title.contains("Partner", true) -> "Legal"
            contact.company.contains("Medical", true) || contact.company.contains("Bio", true) || contact.title.contains("Doctor", true) -> "Healthcare"
            else -> "Technology"
        }

        val cardColor = when (category) {
            "Finance" -> "obsidian_gold"
            "Design" -> "electric_purple"
            "Legal" -> "sunset_blaze"
            "Healthcare" -> "emerald_matrix"
            else -> "deep_stealth"
        }

        return BusinessCard(
            userId = userId,
            name = contact.name,
            company = contact.company,
            title = contact.title,
            phone = contact.phone,
            email = contact.email,
            address = contact.address,
            category = category,
            cardColor = cardColor,
            notes = "Imported from Google Contacts",
            tags = emptyList()
        )
    }
}
