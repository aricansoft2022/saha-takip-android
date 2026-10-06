package com.aricansoft.sahatakip.report

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.aricansoft.sahatakip.BuildConfig
import com.aricansoft.sahatakip.data.model.ProblemRecordStatus
import com.aricansoft.sahatakip.data.model.QualityStatus
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxExporter {
    fun exportProject(context:Context,snapshot:ProjectReportSnapshot):Uri{
        val reportDir=File(context.filesDir,"reports").apply{mkdirs()}
        val file=File(
            reportDir,
            safeFileName(snapshot.projectName)+"_"+formatForFile(snapshot.generatedAt)+".xlsx"
        )

        val itemById=snapshot.workItems.associateBy{it.blockWorkItemId}
        val blockCodes=snapshot.workItems.map{it.blockCode}.distinct()
        val workNames=snapshot.workItems.map{it.workItemName}.distinct().sorted()
        val matrix=snapshot.workItems.associateBy{it.workItemName to it.blockCode}

        val matrixRows=mutableListOf<List<String>>()
        matrixRows.add(listOf(snapshot.projectName))
        matrixRows.add(listOf("Oluşturulma",formatDate(snapshot.generatedAt)))
        matrixRows.add(emptyList())
        matrixRows.add(listOf("İmalat")+blockCodes)
        workNames.forEach{workName->
            matrixRows.add(listOf(workName)+blockCodes.map{block->
                matrix[workName to block]?.let{statusText(it)} ?: ""
            })
        }

        val problemRows=mutableListOf<List<String>>()
        problemRows.add(listOf("Blok","İmalat","Kod","Tanım","Durum","Açılış","Kapanış","Not"))
        snapshot.problems.forEach{problem->
            val item=itemById[problem.blockWorkItemId]
            problemRows.add(listOf(
                item?.blockCode.orEmpty(),
                item?.workItemName.orEmpty(),
                problem.code,
                problem.title,
                if(problem.status==ProblemRecordStatus.OPEN)"Açık" else "Kapalı",
                formatDate(problem.createdAt),
                problem.closedAt?.let{formatDate(it)} ?: "",
                problem.note.orEmpty()
            ))
        }

        val noteRows=mutableListOf<List<String>>()
        noteRows.add(listOf("Blok","İmalat","Tarih","Not"))
        snapshot.notes.forEach{note->
            val item=itemById[note.blockWorkItemId]
            noteRows.add(listOf(
                item?.blockCode.orEmpty(),
                item?.workItemName.orEmpty(),
                formatDate(note.createdAt),
                note.text
            ))
        }

        val photoRows=mutableListOf<List<String>>()
        photoRows.add(listOf("Blok","İmalat","Tarih","Açıklama"))
        snapshot.photos.forEach{photo->
            val item=itemById[photo.blockWorkItemId]
            photoRows.add(listOf(
                item?.blockCode.orEmpty(),
                item?.workItemName.orEmpty(),
                formatDate(photo.createdAt),
                photo.caption.orEmpty()
            ))
        }

        ZipOutputStream(FileOutputStream(file).buffered()).use{zip->
            putText(zip,"[Content_Types].xml",contentTypes())
            putText(zip,"_rels/.rels",rootRels())
            putText(zip,"xl/workbook.xml",workbook())
            putText(zip,"xl/_rels/workbook.xml.rels",workbookRels())
            putText(zip,"xl/worksheets/sheet1.xml",worksheet(matrixRows))
            putText(zip,"xl/worksheets/sheet2.xml",worksheet(problemRows))
            putText(zip,"xl/worksheets/sheet3.xml",worksheet(noteRows))
            putText(zip,"xl/worksheets/sheet4.xml",worksheet(photoRows))
        }

        return FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID+".fileprovider",
            file
        )
    }

    private fun statusText(item:com.aricansoft.sahatakip.data.db.ReportWorkItemRow):String{
        val parts=mutableListOf(item.progressStatus.label)
        if(item.qualityStatus!=QualityStatus.NOT_EVALUATED) parts += item.qualityStatus.label
        if(item.controlStatus.label!="Kontrol edilmedi") parts += item.controlStatus.label
        if(item.isBlocked) parts += "Bloke"
        if(item.openProblemCount>0) parts += "Açık problem: "+item.openProblemCount
        return parts.joinToString(" · ")
    }

    private fun worksheet(rows:List<List<String>>):String{
        val out=StringBuilder()
        out.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        out.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed{rowIndex,row->
            val number=rowIndex+1
            out.append("<row r=\"").append(number).append("\">")
            row.forEachIndexed{columnIndex,value->
                val ref=columnName(columnIndex+1)+number
                out.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                out.append(escapeXml(value))
                out.append("</t></is></c>")
            }
            out.append("</row>")
        }
        out.append("</sheetData></worksheet>")
        return out.toString()
    }

    private fun contentTypes()="""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private fun rootRels()="""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbook()="""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="İmalat Matrisi" sheetId="1" r:id="rId1"/>
    <sheet name="Problemler" sheetId="2" r:id="rId2"/>
    <sheet name="Notlar" sheetId="3" r:id="rId3"/>
    <sheet name="Fotoğraflar" sheetId="4" r:id="rId4"/>
  </sheets>
</workbook>"""

    private fun workbookRels()="""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
</Relationships>"""

    private fun putText(zip:ZipOutputStream,path:String,text:String){
        zip.putNextEntry(ZipEntry(path))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun columnName(index:Int):String{
        var n=index
        val result=StringBuilder()
        while(n>0){
            n--
            result.append(('A'.code+(n%26)).toChar())
            n/=26
        }
        return result.reverse().toString()
    }

    private fun escapeXml(value:String):String{
        val clean=value.filter{ch->
            ch=='\t' || ch=='\n' || ch=='\r' || ch.code in 0x20..0xD7FF || ch.code in 0xE000..0xFFFD
        }
        return clean
            .replace("&","&amp;")
            .replace("<","&lt;")
            .replace(">","&gt;")
            .replace("\"","&quot;")
            .replace("'","&apos;")
    }

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
}
