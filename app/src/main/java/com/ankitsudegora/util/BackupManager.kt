package com.ankitsudegora.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import com.ankitsudegora.data.AppDatabase
import java.io.File
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

    suspend fun backupDatabase(context: Context, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            // Force write-ahead log checkpoint to write all pending transactions to the main db file
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // 1. Write database
                    val dbFile = context.getDatabasePath("kharcha_dekh_db")
                    if (!dbFile.exists()) {
                        throw IllegalStateException("Database file does not exist: ${dbFile.absolutePath}")
                    }
                    zipOut.putNextEntry(ZipEntry("database.db"))
                    dbFile.inputStream().use { inputStream ->
                        inputStream.copyTo(zipOut)
                    }
                    zipOut.closeEntry()

                    // 2. Write preferences
                    val sharedPrefsFile = File(context.dataDir, "shared_prefs/kharchadekh_prefs.xml")
                    if (!sharedPrefsFile.exists()) {
                        throw IllegalStateException("Preferences file does not exist: ${sharedPrefsFile.absolutePath}")
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

    suspend fun restoreDatabase(context: Context, sourceUri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        try {
            // 1. Close active database instance to release file locks and clear singleton reference
            try {
                AppDatabase.closeAndResetInstance()
            } catch (e: Exception) {
                return@withContext RestoreResult.Failure("ERR_DB_CLOSE_FAILED", "Could not close active database: ${e.localizedMessage}")
            }

            val dbFile = context.getDatabasePath("kharcha_dekh_db")
            val walFile = context.getDatabasePath("kharcha_dekh_db-wal")
            val shmFile = context.getDatabasePath("kharcha_dekh_db-shm")

            // 2. Clear WAL and SHM sidecars so they don't corrupt the restored state
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            val sharedPrefsFile = File(context.dataDir, "shared_prefs/kharchadekh_prefs.xml")

            // Ensure parent directory for database exists
            dbFile.parentFile?.mkdirs()

            var dbFileExtracted = false
            var prefsFileExtracted = false

            // 3. Extract zip package contents
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipIn ->
                        var entry = zipIn.nextEntry
                        while (entry != null) {
                            if (entry.name == "database.db") {
                                dbFile.outputStream().use { outputStream ->
                                    zipIn.copyTo(outputStream)
                                }
                                dbFileExtracted = true
                            } else if (entry.name == "preferences.xml") {
                                sharedPrefsFile.parentFile?.let { parent ->
                                    if (!parent.exists()) parent.mkdirs()
                                }
                                sharedPrefsFile.outputStream().use { outputStream ->
                                    zipIn.copyTo(outputStream)
                                }
                                prefsFileExtracted = true
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                }
            } catch (e: java.util.zip.ZipException) {
                return@withContext RestoreResult.Failure("ERR_INVALID_ZIP", "Invalid backup zip format: ${e.localizedMessage}")
            } catch (e: java.io.FileNotFoundException) {
                return@withContext RestoreResult.Failure("ERR_FILE_NOT_FOUND", "Backup file not found or inaccessible: ${e.localizedMessage}")
            } catch (e: Exception) {
                return@withContext RestoreResult.Failure("ERR_FILE_IO_FAILED", "Failed during file extraction: ${e.localizedMessage}")
            }

            if (!dbFileExtracted) {
                return@withContext RestoreResult.Failure("ERR_MISSING_DB_FILE", "Backup package is missing database.db")
            }

            // 4. Set up a clean relaunch via AlarmManager
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
                // Schedule relaunch 100ms from now
                alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
            } catch (e: Exception) {
                return@withContext RestoreResult.Failure("ERR_RELAUNCH_FAILED", "Failed to schedule app relaunch: ${e.localizedMessage}")
            }

            // 5. Forcefully kill the process instantly
            // This stops activity lifecycles completely, blocking them from overwriting preferences
            Process.killProcess(Process.myPid())
            RestoreResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult.Failure("ERR_UNKNOWN", "An unknown error occurred: ${e.localizedMessage}")
        }
    }

    suspend fun backupDatabaseToDefaultFile(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            // Force write-ahead log checkpoint to write all pending transactions to the main db file
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val backupFile = File(backupDir, "kharchadekh_auto_backup.zip")
            backupFile.outputStream().use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // 1. Write database
                    val dbFile = context.getDatabasePath("kharcha_dekh_db")
                    if (!dbFile.exists()) {
                        throw IllegalStateException("Database file does not exist: ${dbFile.absolutePath}")
                    }
                    zipOut.putNextEntry(ZipEntry("database.db"))
                    dbFile.inputStream().use { inputStream ->
                        inputStream.copyTo(zipOut)
                    }
                    zipOut.closeEntry()

                    // 2. Write preferences
                    val sharedPrefsFile = File(context.dataDir, "shared_prefs/kharchadekh_prefs.xml")
                    if (!sharedPrefsFile.exists()) {
                        throw IllegalStateException("Preferences file does not exist: ${sharedPrefsFile.absolutePath}")
                    }
                    zipOut.putNextEntry(ZipEntry("preferences.xml"))
                    sharedPrefsFile.inputStream().use { inputStream ->
                        inputStream.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
