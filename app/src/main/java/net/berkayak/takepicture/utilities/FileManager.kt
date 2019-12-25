package net.berkayak.takepicture.utilities

import android.Manifest
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FileManager private constructor(val folderName: String?, val fileName: String?){

    data class Builder(
    var folderPath: String? = null,
    var fileName: String? = null){
        fun setFolderPath(folderName: String?) = apply { this.folderPath = folderName }
        fun setFileName(fileName: String?) = apply { this.fileName = fileName }

        fun build(): FileManager{
            return FileManager(folderPath, fileName)
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun save(bytes: ByteArray){
        var folder = File("/storage/emulated/0/$folderName")
        if (!folder.exists())
            folder.mkdir()

        var timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var media = File(folder.absolutePath + File.separator + fileName + timeStamp + ".jpg")

        var fileOutputStream = FileOutputStream(media)
        fileOutputStream.write(bytes)
        fileOutputStream.close()
    }

}