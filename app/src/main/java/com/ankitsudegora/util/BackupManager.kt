package com.ankitsudegora.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import com.ankitsudegora.data.AppDatabase
import java.io.File
import java.util.zip.ZipException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class RestoreResult {
    object Success : RestoreResult()
    data class Failure(val errorCode: String, val message: String) : RestoreResult()
}

object BackupManager {

    /**
     * Backs up the current database and shared preferences into a single structured ZIP file.
     * Forces a Write-Ahead Logging (WAL) checkpoint prior to copy to avoid data loss.
     */
    suspend fun backupDatabase(context: Context, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // Close active database instance to flush WAL logs to main file
            try {
                AppDatabase.closeAndResetInstance()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // 1. Pack database file
                    val dbFile = context.getDatabasePath("kharcha_dekh_db")
                    if (!dbFile.exists()) {
                        throw IllegalStateException("Database file does not exist at path: ${dbFile.absolutePath}")
                    }
                    zipOut.putNextEntry(ZipEntry("database.db"))
                    dbFile.inputStream().use { inputStream ->
                        inputStream.copyTo(zipOut)
                    }
                    zipOut.closeEntry()

                    // 2. Pack shared preferences
                    val sharedPrefsFile = File(context.dataDir, "shared_prefs/kharchadekh_prefs.xml")
                    if (!sharedPrefsFile.exists()) {
                        throw IllegalStateException("Preferences file does not exist at path: ${sharedPrefsFile.absolutePath}")
                    }
                    zipOut.putNextEntry(ZipEntry("preferences.xml"))
                    sharedPrefsFile.inputStream().use { inputStream ->
                        inputStream.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Restores the application state from a provided URI. 
     * Automatically sniffs the file format to handle new .zip archives or legacy raw .db files,
     * providing robust error reporting to simplify developer diagnostics.
     */
    suspend fun restoreDatabase(context: Context, sourceUri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        try {
            // 1. Terminate current Room connections to release persistent file locks
            try {
                AppDatabase.closeAndResetInstance()
            } catch (e: Exception) {
                return@withContext RestoreResult.Failure(
                    errorCode = "ERR_DB_CLOSE_FAILED",
                    message = "Database lock release failed. The active Room instance could not be closed programmatically. Internal error: ${e.localizedMessage}"
                )
            }

            val dbFile = context.getDatabasePath("kharcha_dekh_db")
            val walFile = context.getDatabasePath("kharcha_dekh_db-wal")
            val shmFile = context.getDatabasePath("kharcha_dekh_db-shm")

            // 2. Safely wipe out local WAL journal sidecars to prevent state pollution or corruption post-restore
            if (walFile.exists() && !walFile.delete()) {
                return@withContext RestoreResult.Failure("ERR_WAL_DELETE_FAILED", "Failed to clear the active Write-Ahead Log (-wal) cache file before streaming data.")
            }
            if (shmFile.exists() && !shmFile.delete()) {
                return@withContext RestoreResult.Failure("ERR_SHM_DELETE_FAILED", "Failed to clear the active Shared Memory (-shm) cache file before streaming data.")
            }

            val sharedPrefsFile = File(context.dataDir, "shared_prefs/kharchadekh_prefs.xml")
            dbFile.parentFile?.mkdirs()

            // 3. Inspect stream magic bytes to evaluate file format type
            var isZip = false
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    val header = ByteArray(4)
                    val bytesRead = inputStream.read(header)
                    // Match ZIP local file header signature signature bytes: 0x50, 0x4B, 0x03, 0x04 ("PK..")
                    if (bytesRead == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
                        isZip = true
                    }
                }
            } catch (e: java.io.FileNotFoundException) {
                return@withContext RestoreResult.Failure("ERR_FILE_NOT_FOUND", "The requested backup file could not be opened because it no longer exists on this path.")
            } catch (e: Exception) {
                return@withContext RestoreResult.Failure("ERR_HEADER_READ_FAILED", "Security or IO exception encountered while analyzing file magic bytes: ${e.localizedMessage}")
            }

            var dbFileExtracted = false

            if (isZip) {
                // --- Flow A: Extraction logic for structured multi-file .zip packages ---
                try {
                    context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                        ZipInputStream(inputStream).use { zipIn ->
                            var entry = zipIn.nextEntry
                            while (entry != null) {
                                when (entry.name) {
                                    "database.db" -> {
                                        dbFile.outputStream().use { outputStream ->
                                            zipIn.copyTo(outputStream)
                                        }
                                        dbFileExtracted = true
                                    }
                                    "preferences.xml" -> {
                                        sharedPrefsFile.parentFile?.let { parent ->
                                            if (!parent.exists()) parent.mkdirs()
                                        }
                                        sharedPrefsFile.outputStream().use { outputStream ->
                                            zipIn.copyTo(outputStream)
                                        }
                                    }
                                }
                                zipIn.closeEntry()
                                entry = zipIn.nextEntry
                            }
                        }
                    }
                } catch (e: ZipException) {
                    return@withContext RestoreResult.Failure("ERR_CORRUPT_ZIP", "ZIP structure validation failed. The archive might be partially extracted or corrupted. Internal details: ${e.localizedMessage}")
                } catch (e: Exception) {
                    return@withContext RestoreResult.Failure("ERR_ZIP_IO_FAILURE", "An unhandled IO error occurred during archive extraction loop: ${e.localizedMessage}")
                }

                if (!dbFileExtracted) {
                    return@withContext RestoreResult.Failure("ERR_INVALID_KHARCHADEKH_PACKAGE", "The selected archive is a valid ZIP file but does not match the KharchaDekh format layout (missing internal 'database.db' entry).")
                }

            } else {
                // --- Flow B: Backward-compatibility stream mapping for loose legacy .db files ---
                try {
                    context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                        dbFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        dbFileExtracted = true
                    }
                } catch (e: Exception) {
                    return@withContext RestoreResult.Failure("ERR_LEGACY_STREAM_FAILED", "Failed to stream copy loose raw database over target path: ${e.localizedMessage}")
                }
            }

            // 4. Set up an exact relaunch via AlarmManager to reset runtime hooks
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_CANCEL_CURRENT
                    }
                )

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                // Schedule clear restart 100 milliseconds into the future
                alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
            } catch (e: Exception) {
                return@withContext RestoreResult.Failure("ERR_SCHEDULER_RELAUNCH_FAILED", "Backup file was successfully written, but AlarmManager failed to coordinate an app restart: ${e.localizedMessage}")
            }

            // 5. Instantly kill the thread process to purge Room's dirty layout caches
            Process.killProcess(Process.myPid())
            RestoreResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult.Failure("ERR_CRITICAL_UNKNOWN", "An unhandled global exception occurred inside BackupManager: ${e.localizedMessage}")
        }
    }

    /**
     * Automated nightly task backup generation logic.
     */
    suspend fun backupDatabaseToDefaultFile(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            // Close active database instance to flush WAL logs to main file
            try {
                AppDatabase.closeAndResetInstance()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val backupFile = File(backupDir, "kharchadekh_auto_backup.zip")
            backupFile.outputStream().use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    val dbFile = context.getDatabasePath("kharcha_dekh_db")
                    if (!dbFile.exists()) return@withContext null
                    
                    zipOut.putNextEntry(ZipEntry("database.db"))
                    dbFile.inputStream().use { inputStream ->
                        inputStream.copyTo(zipOut)
                    }
                    zipOut.closeEntry()

                    val sharedPrefsFile = File(context.dataDir, "shared_prefs/kharchadekh_prefs.xml")
                    if (sharedPrefsFile.exists()) {
                        zipOut.putNextEntry(ZipEntry("preferences.xml"))
                        sharedPrefsFile.inputStream().use { inputStream ->
                            inputStream.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                }
            }
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
