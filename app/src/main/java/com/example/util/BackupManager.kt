package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.AppDatabase
import java.io.File

object BackupManager {

    fun backupDatabase(context: Context, destinationUri: Uri): Boolean {
        return try {
            val db = AppDatabase.getDatabase(context)
            // Force write-ahead log checkpoint to write all pending transactions to the main db file
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val dbFile = context.getDatabasePath("kharcha_dekh_db")
            if (!dbFile.exists()) return false

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                dbFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
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

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                dbFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Programmatically restart the application to reset Room DB memory references
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

            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val backupFile = File(backupDir, "kharchadekh_auto_backup.db")
            backupFile.outputStream().use { outputStream ->
                dbFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
