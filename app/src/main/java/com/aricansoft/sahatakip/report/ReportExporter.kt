package com.aricansoft.sahatakip.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.aricansoft.sahatakip.BuildConfig
import com.aricansoft.sahatakip.data.model.ProblemRecordStatus
import com.aricansoft.sahatakip.data.model.QualityStatus
import com.aricansoft.sahatakip.data.model.ProgressStatus
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

object ReportExporter {
    fun exportProject(context:Context,snapshot:ProjectReportSnapshot):Uri{
        val reportDir=File(context.filesDir,"reports").apply{mkdirs()}
        val stamp=formatForFile(snapshot.generatedAt)
        val file=File(reportDir,safeFileName(snapshot.projectName)+"_"+stamp+".pdf")
        val document=PdfDocument()
        val writer=PdfWriter(document,snapshot.projectName)

        try{
            val finished=snapshot.workItems.count{it.progressStatus==ProgressStatus.FINISHED}
            val defective=snapshot.workItems.count{
                it.qualityStatus==QualityStatus.DEFECTIVE || it.qualityStatus==QualityStatus.CRITICAL_DEFECT
            }
            val blocked=snapshot.workItems.count{it.isBlocked}
            val openProblems=snapshot.problems.count{it.status==ProblemRecordStatus.OPEN}

            writer.title("SAHA TAKİP RAPORU")
            writer.paragraph("Proje: "+snapshot.projectName)
            writer.paragraph("Oluşturulma: "+formatDate(snapshot.generatedAt))
            writer.spacer(6f)
            writer.heading("Genel özet")
            writer.paragraph(
                "Toplam imalat kaydı: "+snapshot.workItems.size+
                    "   •   Bitti: "+finished+
                    "   •   Kusurlu/ağır kusurlu: "+defective+
                    "   •   Bloke: "+blocked+
                    "   •   Açık rapor problemi: "+openProblems
            )
            writer.spacer(8f)

            val problems=snapshot.problems.groupBy{it.blockWorkItemId}
            val notes=snapshot.notes.groupBy{it.blockWorkItemId}
            val photos=snapshot.photos.groupBy{it.blockWorkItemId}

            snapshot.workItems.groupBy{it.blockCode}.forEach{entry->
                val blockCode=entry.key
                val items=entry.value
                writer.heading(blockCode)
                items.forEach{item->
                    val status=buildList{
                        add(item.progressStatus.label)
                        if(item.qualityStatus!=QualityStatus.NOT_EVALUATED) add(item.qualityStatus.label)
                        add(item.controlStatus.label)
                        if(item.isBlocked) add("Bloke")
                    }.joinToString(" · ")

                    writer.subheading(item.workItemName)
                    writer.paragraph(status,indent=10f)

                    problems[item.blockWorkItemId].orEmpty().forEach{problem->
                        val state=if(problem.status==ProblemRecordStatus.OPEN)"Açık" else "Kapalı"
                        writer.bullet(
                            "Problem "+problem.code+" — "+problem.title+" ["+state+"]"+
                                (problem.note?.let{" — "+it} ?: "")+
                                " ("+formatDate(problem.createdAt)+")"
                        )
                    }

                    notes[item.blockWorkItemId].orEmpty().forEach{note->
                        writer.bullet("Not — "+note.text+" ("+formatDate(note.createdAt)+")")
                    }

                    photos[item.blockWorkItemId].orEmpty().forEach{photo->
                        writer.paragraph(
                            "Fotoğraf — "+formatDate(photo.createdAt)+
                                (photo.caption?.let{" — "+it} ?: ""),
                            indent=10f
                        )
                        val bitmap=decodeBitmap(context,Uri.parse(photo.localUri))
                        if(bitmap!=null){
                            writer.image(bitmap)
                            bitmap.recycle()
                        }else{
                            writer.paragraph("[Fotoğraf dosyası okunamadı]",indent=10f)
                        }
                    }
                    writer.spacer(4f)
                }
                writer.spacer(6f)
            }

            writer.finish()
            FileOutputStream(file).use{document.writeTo(it)}
        }finally{
            document.close()
        }

        return FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID+".fileprovider",
            file
        )
    }

    private fun decodeBitmap(context:Context,uri:Uri):Bitmap?=runCatching{
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
            val source=ImageDecoder.createSource(context.contentResolver,uri)
            ImageDecoder.decodeBitmap(source){decoder,info,_->
                val width=info.size.width.coerceAtLeast(1)
                val sample=(width/1600).coerceAtLeast(1)
                decoder.setTargetSampleSize(sample)
            }
        }else{
            context.contentResolver.openInputStream(uri).use{BitmapFactory.decodeStream(it)}
        }
    }.getOrNull()

    private fun formatDate(epochMillis:Long)=Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

    private fun formatForFile(epochMillis:Long)=Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

    private fun safeFileName(value:String)=value
        .lowercase(Locale.forLanguageTag("tr-TR"))
        .replace("ı","i").replace("ğ","g").replace("ü","u").replace("ş","s").replace("ö","o").replace("ç","c")
        .replace(Regex("""[^a-z0-9._-]+"""),"_")
        .trim('_')
        .ifBlank{"saha-raporu"}

    private class PdfWriter(
        private val document:PdfDocument,
        private val projectName:String
    ){
        private val pageWidth=595
        private val pageHeight=842
        private val margin=36f
        private val contentWidth=pageWidth-(margin*2)
        private val bottom=pageHeight-margin
        private var pageNumber=0
        private var page:PdfDocument.Page?=null
        private var y=margin

        private val body=Paint(Paint.ANTI_ALIAS_FLAG).apply{
            color=Color.BLACK
            textSize=10.5f
            typeface=Typeface.create("sans-serif",Typeface.NORMAL)
        }
        private val small=Paint(body).apply{textSize=8.5f;color=Color.DKGRAY}
        private val h1=Paint(body).apply{textSize=19f;typeface=Typeface.DEFAULT_BOLD}
        private val h2=Paint(body).apply{textSize=14f;typeface=Typeface.DEFAULT_BOLD}
        private val h3=Paint(body).apply{textSize=11.5f;typeface=Typeface.DEFAULT_BOLD}

        fun title(text:String){
            ensurePage()
            drawWrapped(text,h1,0f,8f)
        }

        fun heading(text:String){
            ensure(26f)
            drawWrapped(text,h2,0f,5f)
        }

        fun subheading(text:String){
            ensure(20f)
            drawWrapped(text,h3,4f,2f)
        }

        fun paragraph(text:String,indent:Float=0f){
            drawWrapped(text,body,indent,2f)
        }

        fun bullet(text:String){
            val prefix="• "
            val prefixWidth=body.measureText(prefix)
            val lines=wrap(text,body,contentWidth-18f-prefixWidth)
            lines.forEachIndexed{index,line->
                ensure(lineHeight(body))
                page!!.canvas.drawText(if(index==0)prefix else "",margin+10f,y,body)
                page!!.canvas.drawText(line,margin+10f+prefixWidth,y,body)
                y+=lineHeight(body)
            }
            y+=1f
        }

        fun spacer(height:Float){
            ensure(height)
            y+=height
        }

        fun image(bitmap:Bitmap){
            val maxWidth=contentWidth-20f
            val maxHeight=250f
            val scale=min(min(maxWidth/bitmap.width,maxHeight/bitmap.height),1f)
            val width=bitmap.width*scale
            val height=bitmap.height*scale
            ensure(height+8f)
            val left=margin+10f
            page!!.canvas.drawBitmap(bitmap,null,RectF(left,y,left+width,y+height),null)
            y+=height+8f
        }

        fun finish(){
            page?.let{
                drawFooter(it)
                document.finishPage(it)
                page=null
            }
        }

        private fun drawWrapped(text:String,paint:Paint,indent:Float,after:Float){
            val lines=wrap(text,paint,contentWidth-indent)
            lines.forEach{line->
                ensure(lineHeight(paint))
                page!!.canvas.drawText(line,margin+indent,y,paint)
                y+=lineHeight(paint)
            }
            y+=after
        }

        private fun wrap(text:String,paint:Paint,maxWidth:Float):List<String>{
            if(text.isBlank()) return listOf("")
            val result=mutableListOf<String>()
            text.lines().forEach{paragraph->
                if(paragraph.isBlank()){
                    result.add("")
                }else{
                    var current=""
                    paragraph.split(Regex("\\s+")).forEach{word->
                        val candidate=if(current.isEmpty()) word else current+" "+word
                        if(paint.measureText(candidate)<=maxWidth){
                            current=candidate
                        }else{
                            if(current.isNotEmpty()) result.add(current)
                            current=word
                        }
                    }
                    if(current.isNotEmpty()) result.add(current)
                }
            }
            return result
        }

        private fun lineHeight(paint:Paint)=paint.textSize*1.35f

        private fun ensure(required:Float){
            ensurePage()
            if(y+required>bottom){
                page?.let{
                    drawFooter(it)
                    document.finishPage(it)
                }
                page=null
                ensurePage()
            }
        }

        private fun ensurePage(){
            if(page!=null) return
            pageNumber++
            val info=PdfDocument.PageInfo.Builder(pageWidth,pageHeight,pageNumber).create()
            page=document.startPage(info)
            y=margin
            page!!.canvas.drawText(projectName,margin,y,small)
            page!!.canvas.drawLine(margin,y+5f,pageWidth-margin,y+5f,small)
            y+=20f
        }

        private fun drawFooter(target:PdfDocument.Page){
            val text="Sayfa "+pageNumber
            target.canvas.drawText(text,pageWidth-margin-small.measureText(text),pageHeight-18f,small)
        }
    }
}
