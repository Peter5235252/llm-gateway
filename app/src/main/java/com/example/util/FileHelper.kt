package com.example.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileHelper(private val context: Context) {

    suspend fun saveFile(treeUriStr: String, fileName: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val treeUri = Uri.parse(treeUriStr)
            val pickedDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
            
            // Delete if exists
            val existingFile = pickedDir.findFile(fileName)
            existingFile?.delete()
            
            val newFile = pickedDir.createFile("text/plain", fileName) ?: return@withContext false
            
            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun readFile(treeUriStr: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val treeUri = Uri.parse(treeUriStr)
            val pickedDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext null
            val file = pickedDir.findFile(fileName) ?: return@withContext null
            
            val stringBuilder = java.lang.StringBuilder()
            context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
            stringBuilder.toString().trim()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
