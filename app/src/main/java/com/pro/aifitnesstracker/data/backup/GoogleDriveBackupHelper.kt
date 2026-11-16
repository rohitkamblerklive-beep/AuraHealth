package com.pro.aifitnesstracker.data.backup

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveBackupHelper(private val context: Context) {

    private val client = OkHttpClient()
    private val backupManager = BackupManager(context)

    suspend fun getAccessToken(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        try {
            val scopeString = "oauth2:https://www.googleapis.com/auth/drive.appdata"
            val googleAccount = account.account ?: return@withContext null
            
            // Get current cached token
            var token = GoogleAuthUtil.getToken(context, googleAccount, scopeString)
            
            // Clear the token immediately to force cache invalidation
            try {
                GoogleAuthUtil.clearToken(context, token)
            } catch (ex: Exception) {
                // Ignore clearing errors
            }

            // Fetch a fresh token with the newly enabled API permissions
            GoogleAuthUtil.getToken(
                context,
                googleAccount,
                scopeString
            )
        } catch (e: Exception) {
            Log.e("GDriveBackup", "Token retrieval failed", e)
            null
        }
    }

    suspend fun backupToDrive(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Generate JSON string of all stats
            val backupFile = context.cacheDir.resolve("temp_backup.json")
            val uri = android.net.Uri.fromFile(backupFile)
            val successExport = backupManager.exportBackup(uri)
            if (!successExport) {
                Log.e("GDriveBackup", "Backup export to temp file failed")
                return@withContext false
            }
            val backupJsonString = backupFile.readText()
            backupFile.delete()

            // 2. Search if file already exists in AppData
            val fileId = searchBackupFile(accessToken)

            if (fileId != null) {
                // Update existing file
                val request = Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
                    .header("Authorization", "Bearer $accessToken")
                    .patch(backupJsonString.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("GDriveBackup", "Update failed. Code: ${response.code}, Body: ${response.body?.string()}")
                    }
                    response.isSuccessful
                }
            } else {
                // Create new file
                val metadata = JSONObject().apply {
                    put("name", "AuraHealth_Backup.json")
                    put("parents", org.json.JSONArray().apply { put("appDataFolder") })
                }
                
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(
                        MultipartBody.Part.createFormData(
                            "metadata", 
                            null, 
                            metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
                        )
                    )
                    .addPart(
                        MultipartBody.Part.createFormData(
                            "media", 
                            "AuraHealth_Backup.json", 
                            backupJsonString.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
                        )
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    .header("Authorization", "Bearer $accessToken")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("GDriveBackup", "Create failed. Code: ${response.code}, Body: ${response.body?.string()}")
                    }
                    response.isSuccessful
                }
            }
        } catch (e: Exception) {
            Log.e("GDriveBackup", "Backup error", e)
            false
        }
    }

    suspend fun restoreFromDrive(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Search for backup file
            val fileId = searchBackupFile(accessToken)
            if (fileId == null) {
                Log.e("GDriveBackup", "Restore failed: Backup file not found on Google Drive appDataFolder")
                return@withContext false
            }

            // 2. Download content
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                .header("Authorization", "Bearer $accessToken")
                .build()
            
            val jsonString = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GDriveBackup", "Download failed. Code: ${response.code}, Body: ${response.body?.string()}")
                    return@withContext false
                }
                response.body?.string()
            } ?: return@withContext false

            // 3. Write to temporary cache file and import
            val backupFile = context.cacheDir.resolve("temp_restore.json")
            backupFile.writeText(jsonString)
            val uri = android.net.Uri.fromFile(backupFile)
            val successImport = backupManager.importBackup(uri)
            backupFile.delete()

            successImport
        } catch (e: Exception) {
            Log.e("GDriveBackup", "Restore error", e)
            false
        }
    }

    private fun searchBackupFile(accessToken: String): String? {
        return try {
            val encodedQuery = java.net.URLEncoder.encode("name='AuraHealth_Backup.json'", "UTF-8")
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?q=$encodedQuery&spaces=appDataFolder")
                .header("Authorization", "Bearer $accessToken")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GDriveBackup", "Search request failed. Code: ${response.code}, Body: ${response.body?.string()}")
                    return null
                }
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val files = json.getJSONArray("files")
                if (files.length() > 0) {
                    files.getJSONObject(0).getString("id")
                } else null
            }
        } catch (e: Exception) {
            Log.e("GDriveBackup", "Search error", e)
            null
        }
    }
}
