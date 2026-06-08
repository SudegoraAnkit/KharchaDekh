package com.ankitsudegora.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ankitsudegora.data.AppDatabase
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    fun backupDatabase(context: Context, destinationUri: Uri): Boolean {
        return try {
            val db = AppDatabase.getDatabase(context)
            // Force write-ahead log checkpoint to write all pending transactions to the main db file
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val dbFile = context.getDatabasePath("kharcha_dekh_db")
            if (!dbFile.exists()) return false

            val sharedPrefsFile = File(context.dataDir, "shared_prefs/kharchadekh_prefs.xml")

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // 1. Pack database file
                    if (dbFile.exists()) {
                        zipOut.putNextEntry(ZipEntry("database.db"))
                        dbFile.inputStream().use { inputStream ->
                            inputStream.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                    // 2. Pack user settings preferences file
                    if (sharedPrefsFile.exists()) {
                        zipOut.putNextEntry(ZipEntry("preferences.xml"))
                        sharedPrefsFile.inputStream().use { inputStream ->
                            inputStream.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun restoreDatabase(context: Context, sourceUri: Uri): Boolean {
        return try {
            // Close active database instance to prevent locking/corruption
            val db = AppDatabase.getDatabase(context)
            db.close()

            val dbFile = context.getDatabasePath("kharcha_dekh_db")
            val walFile = context.getDatabasePath("kharcha_dekh_db-wal")
            val shmFile = context.getDatabasePath("kharcha_dekh_db-shm")

            // Delete wal and shm files so that they don't overwrite restored DB state
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            val sharedPrefsFile = File(context.dataDir, "shared_prefs/kharchadekh_prefs.xml")

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name == "database.db") {
                            dbFile.outputStream().use { outputStream ->
                                zipIn.copyTo(outputStream)
                            }
                        } else if (entry.name == "preferences.xml") {
                            // Ensure shared_prefs directory exists
                            sharedPrefsFile.parentFile?.let { parent ->
                                if (!parent.exists()) parent.mkdirs()
                            }
                            sharedPrefsFile.outputStream().use { outputStream ->
                                zipIn.copyTo(outputStream)
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            // Programmatically restart the application to reset Room DB and SharedPreferences memory references
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun backupDatabaseToDefaultFile(context: Context): File? {
        return try {
            val db = AppDatabase.getDatabase(context)
            // Force write-ahead log checkpoint to write all pending transactions to the main db file
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val dbFile = context.getDatabasePath("kharcha_dekh_db")
            if (!dbFile.exists()) return null

            val sharedPrefsFile = File(context.dataDir, "shared_prefs/kharchadekh_prefs.xml")

            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val backupFile = File(backupDir, "kharchadekh_auto_backup.zip")
            backupFile.outputStream().use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // 1. Pack database file
                    if (dbFile.exists()) {
                        zipOut.putNextEntry(ZipEntry("database.db"))
                        dbFile.inputStream().use { inputStream ->
                            inputStream.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                    // 2. Pack user settings preferences file
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
