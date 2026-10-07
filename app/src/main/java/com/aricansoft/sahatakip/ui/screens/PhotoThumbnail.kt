package com.aricansoft.sahatakip.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
internal fun LocalPhotoThumbnail(uriString:String){
    val context=LocalContext.current
    val bitmap by produceState<Bitmap?>(null,uriString){
        value=withContext(Dispatchers.IO){
            decodeThumbnail(context,Uri.parse(uriString),256)
        }
    }
    DisposableEffect(bitmap){
        onDispose{
            bitmap?.takeIf{!it.isRecycled}?.recycle()
        }
    }
    Surface(
        modifier=Modifier.size(84.dp),
        shape=MaterialTheme.shapes.small,
        tonalElevation=2.dp
    ){
        val bmp=bitmap
        if(bmp!=null && !bmp.isRecycled){
            Image(
                bitmap=bmp.asImageBitmap(),
                contentDescription="Saha fotoğrafı",
                modifier=Modifier.fillMaxSize(),
                contentScale=ContentScale.Crop
            )
        }else{
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                Icon(Icons.Outlined.CameraAlt,contentDescription=null)
            }
        }
    }
}

private fun decodeThumbnail(context:Context,uri:Uri,maxPx:Int):Bitmap?=runCatching{
    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
        val source=ImageDecoder.createSource(context.contentResolver,uri)
        ImageDecoder.decodeBitmap(source){decoder,info,_->
            val width=info.size.width.coerceAtLeast(1)
            val height=info.size.height.coerceAtLeast(1)
            val longest=max(width,height)
            if(longest>maxPx){
                val scale=maxPx.toFloat()/longest.toFloat()
                decoder.setTargetSize(
                    (width*scale).toInt().coerceAtLeast(1),
                    (height*scale).toInt().coerceAtLeast(1)
                )
            }
        }
    }else{
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
        val longest=max(bounds.outWidth,bounds.outHeight).coerceAtLeast(1)
        var sample=1
        while(longest/sample>maxPx*2) sample*=2
        val options=BitmapFactory.Options().apply{inSampleSize=sample.coerceAtLeast(1)}
        context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,options)}
    }
}.getOrNull()
