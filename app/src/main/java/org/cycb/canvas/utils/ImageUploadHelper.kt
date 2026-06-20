package org.cycb.canvas.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object ImageUploadHelper {
    private const val TAG = "ImageUploadHelper"

    suspend fun uploadToBackend(
        context: Context,
        imageUri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting upload for URI: $imageUri")

            val file = uriToFile(context, imageUri)
            Log.d(TAG, "File created: ${file.name}, size: ${file.length()} bytes")

            val mimeType = when {
                file.name.endsWith(".jpg", ignoreCase = true) ||
                file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                file.name.endsWith(".gif", ignoreCase = true) -> "image/gif"
                file.name.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }

            Log.d(TAG, "MIME type: $mimeType")

            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val imagePart = okhttp3.MultipartBody.Part.createFormData("image", file.name, requestFile)

            Log.d(TAG, "Uploading to backend...")

            val response = org.cycb.canvas.data.api.RetrofitClient.apiService.uploadImage(imagePart)

            Log.d(TAG, "Upload response: success=${response.success}, url=${response.url}, message=${response.message}")

            file.delete()

            if (response.success && response.url.isNotEmpty()) {
                Result.success(response.url)
            } else {
                Result.failure(Exception("Upload failed: ${response.message}"))
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "HTTP ${e.code()} error: $errorBody", e)
            Result.failure(Exception("Upload failed (${e.code()}): $errorBody"))
        } catch (e: Exception) {
            Log.e(TAG, "Upload error", e)
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePicture(
        context: Context,
        imageUri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = uriToFile(context, imageUri)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = okhttp3.MultipartBody.Part.createFormData("profilePicture", file.name, requestFile)

            val response = org.cycb.canvas.data.api.RetrofitClient.apiService.uploadProfilePicture(imagePart)
            file.delete()

            if (response.success && response.url.isNotEmpty()) {
                Result.success(response.url)
            } else {
                Result.failure(Exception("Profile picture upload failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Profile picture upload error", e)
            Result.failure(e)
        }
    }

    suspend fun uploadToCloudinary(
        context: Context,
        imageUri: Uri,
        cloudName: String,
        uploadPreset: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = uriToFile(context, imageUri)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
                .addFormDataPart("upload_preset", uploadPreset)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                file.delete()
                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    Result.success(json.getString("secure_url"))
                } else {
                    Result.failure(Exception("Cloudinary upload failed: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary upload error", e)
            Result.failure(e)
        }
    }

    suspend fun uploadToImgBB(
        context: Context,
        imageUri: Uri,
        apiKey: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = uriToFile(context, imageUri)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
                .build()

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload?key=$apiKey")
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                file.delete()
                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    Result.success(json.getJSONObject("data").getString("url"))
                } else {
                    Result.failure(Exception("ImgBB upload failed: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ImgBB upload error", e)
            Result.failure(e)
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File {
        val contentResolver = context.contentResolver
        val fileName = getFileName(context, uri)
        val tempFile = File(context.cacheDir, fileName)

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "image_${System.currentTimeMillis()}.jpg"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    suspend fun compressImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024,
        quality: Int = 80
    ): Uri = withContext(Dispatchers.IO) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(uri)
            ) ?: return@withContext uri

            var width = bitmap.width
            var height = bitmap.height

            if (width > maxWidth || height > maxHeight) {
                val ratio = width.toFloat() / height.toFloat()
                if (ratio > 1) {
                    width = maxWidth
                    height = (maxWidth / ratio).toInt()
                } else {
                    height = maxHeight
                    width = (maxHeight * ratio).toInt()
                }
            }

            val resized = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)

            val tempFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { out ->
                resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
            }

            bitmap.recycle()
            resized.recycle()

            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            Log.e(TAG, "Compression error", e)
            uri
        }
    }
}
