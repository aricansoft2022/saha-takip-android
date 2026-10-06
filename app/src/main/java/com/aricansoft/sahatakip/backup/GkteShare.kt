package com.aricansoft.sahatakip.backup

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

object GkteShare {
    private val whatsappPackages=listOf(
        "com.whatsapp",
        "com.whatsapp.w4b"
    )

    fun shareToWhatsApp(context:Context,uri:Uri){
        val base=Intent(Intent.ACTION_SEND).apply{
            type=SitePackManager.GKTE_MIME
            putExtra(Intent.EXTRA_STREAM,uri)
            clipData=ClipData.newUri(context.contentResolver,"GKTE proje dosyası",uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val installed=whatsappPackages.firstOrNull{packageName->
            runCatching{
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName,0)
            }.isSuccess
        }

        if(installed!=null){
            val whatsappIntent=Intent(base).apply{
                setPackage(installed)
                // WhatsApp bilinmeyen vendor MIME türlerini her sürümde aynı
                // yorumlamıyor. Dosya adı .gkte kaldığı için octet-stream güvenlidir.
                type="application/octet-stream"
            }
            context.grantUriPermission(
                installed,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            runCatching{
                context.startActivity(whatsappIntent)
            }.onSuccess{
                return
            }
        }

        context.startActivity(
            Intent.createChooser(base,"GKTE proje dosyasını paylaş")
        )
    }
}
