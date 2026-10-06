package com.aricansoft.sahatakip.photo

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.aricansoft.sahatakip.BuildConfig
import com.aricansoft.sahatakip.data.db.PhotoContextRow
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class PendingPhoto(val uri:Uri,val file:File)

object PhotoStore {
    fun create(context:Context,photoContext:PhotoContextRow):PendingPhoto{
        val root=requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        val dir=File(
            root,
            listOf(
                safe(photoContext.projectName),
                safe(photoContext.blockCode),
                safe(photoContext.workItemName)
            ).joinToString(File.separator)
        )
        check(dir.exists() || dir.mkdirs()){"Fotoğraf klasörü oluşturulamadı."}
        val stamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))
        val file=File(dir,"IMG_"+stamp+".jpg")
        val uri=FileProvider.getUriForFile(context,BuildConfig.APPLICATION_ID+".fileprovider",file)
        return PendingPhoto(uri,file)
    }

    private fun safe(value:String)=value
        .trim()
        .replace(Regex("[^\p{L}\p{N}._-]+"),"_")
        .take(80)
}
