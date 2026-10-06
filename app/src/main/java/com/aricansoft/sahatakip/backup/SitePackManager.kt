package com.aricansoft.sahatakip.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Environment
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.aricansoft.sahatakip.BuildConfig
import com.aricansoft.sahatakip.data.db.SahaDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SitePackManager(
    private val context:Context,
    private val database:SahaDatabase
){
    suspend fun exportProject(projectId:String):Uri{
        val root=dumpProject(projectId)
        val project=root.getJSONObject("meta").getString("projectName")
        val photos=root.getJSONObject("tables").getJSONArray("photos")

        val dir=File(context.filesDir,"backups").apply{mkdirs()}
        val stamp=DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .format(Instant.now().atZone(ZoneId.systemDefault()))
        val output=File(dir,safe(project)+"_"+stamp+".sitepack")

        ZipOutputStream(FileOutputStream(output).buffered()).use{zip->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(root.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            for(i in 0 until photos.length()){
                val row=photos.getJSONObject(i)
                val id=row.getString("id")
                val uri=row.optString("localUri")
                if(uri.isBlank()) continue
                runCatching{
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use{input->
                        zip.putNextEntry(ZipEntry("photos/"+id+".jpg"))
                        input.copyTo(zip)
                        zip.closeEntry()
                    }
                }
            }
        }

        return FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID+".fileprovider",
            output
        )
    }

    suspend fun importProject(sitePackUri:Uri):String{
        val tempRoot=File(context.cacheDir,"sitepack-"+UUID.randomUUID()).apply{mkdirs()}
        try{
            extract(sitePackUri,tempRoot)
            val dataFile=File(tempRoot,"data.json")
            require(dataFile.isFile){"Geçerli bir .sitepack değil: data.json bulunamadı."}
            val root=JSONObject(dataFile.readText(Charsets.UTF_8))
            require(root.optInt("formatVersion")==1){"Desteklenmeyen .sitepack sürümü."}
            val tables=root.getJSONObject("tables")
            normalizeKindsForImport(tables)

            val oldProject=singleId(tables,"projects")
            val newProject="project-"+UUID.randomUUID()
            val maps=IdMaps(
                projects=mapOf(oldProject to newProject),
                blockTypes=newIdMap(tables,"block_types","bt"),
                blocks=newIdMap(tables,"blocks","block"),
                workItems=newIdMap(tables,"work_item_definitions","wi"),
                blockWorkItems=newIdMap(tables,"block_work_items","bwi"),
                problemDefinitions=newIdMap(tables,"problem_definitions","pd"),
                problemRecords=newIdMap(tables,"problem_records","pr"),
                notes=newIdMap(tables,"notes","note"),
                photos=newIdMap(tables,"photos","photo"),
                attributes=newIdMap(tables,"block_attribute_definitions","bad"),
                audits=newIdMap(tables,"audit_events","audit")
            )

            remap(tables,maps)
            restorePhotos(tempRoot,tables,maps.photos,newProject)
            insertTables(tables)
            database.invalidationTracker.refreshAsync()
            return newProject
        }finally{
            tempRoot.deleteRecursively()
        }
    }

    private suspend fun dumpProject(projectId:String):JSONObject=database.withTransaction{
        val project=database.sahaDao().getProject(projectId) ?: error("Proje bulunamadı.")
        val db=database.openHelper.readableDatabase
        val tables=JSONObject()

        val queries=linkedMapOf(
            "projects" to "SELECT * FROM projects WHERE id=?",
            "block_types" to "SELECT * FROM block_types WHERE projectId=?",
            "blocks" to "SELECT * FROM blocks WHERE projectId=?",
            "work_item_definitions" to "SELECT * FROM work_item_definitions WHERE projectId=?",
            "block_type_work_items" to """
                SELECT btw.* FROM block_type_work_items btw
                JOIN block_types bt ON bt.id=btw.blockTypeId
                WHERE bt.projectId=?
            """.trimIndent(),
            "block_work_items" to """
                SELECT bwi.* FROM block_work_items bwi
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=?
            """.trimIndent(),
            "problem_definitions" to "SELECT * FROM problem_definitions WHERE projectId=?",
            "problem_records" to """
                SELECT pr.* FROM problem_records pr
                JOIN block_work_items bwi ON bwi.id=pr.blockWorkItemId
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=?
            """.trimIndent(),
            "notes" to """
                SELECT n.* FROM notes n
                JOIN block_work_items bwi ON bwi.id=n.blockWorkItemId
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=?
            """.trimIndent(),
            "photos" to """
                SELECT ph.* FROM photos ph
                JOIN block_work_items bwi ON bwi.id=ph.blockWorkItemId
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=?
            """.trimIndent(),
            "block_attribute_definitions" to "SELECT * FROM block_attribute_definitions WHERE projectId=?",
            "block_attribute_values" to """
                SELECT bav.* FROM block_attribute_values bav
                JOIN blocks b ON b.id=bav.blockId
                WHERE b.projectId=?
            """.trimIndent(),
            "audit_events" to """
                SELECT ae.* FROM audit_events ae
                JOIN block_work_items bwi ON bwi.id=ae.blockWorkItemId
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=?
            """.trimIndent()
        )

        queries.forEach{entry->
            db.query(SimpleSQLiteQuery(entry.value,arrayOf(projectId))).use{cursor->
                tables.put(entry.key,cursorToJson(cursor))
            }
        }

        JSONObject()
            .put("formatVersion",1)
            .put("databaseVersion",2)
            .put("meta",JSONObject()
                .put("projectId",project.id)
                .put("projectName",project.name)
                .put("exportedAt",System.currentTimeMillis())
            )
            .put("tables",tables)
    }

    private fun cursorToJson(cursor:Cursor):JSONArray{
        val result=JSONArray()
        while(cursor.moveToNext()){
            val row=JSONObject()
            for(index in 0 until cursor.columnCount){
                val name=cursor.getColumnName(index)
                when(cursor.getType(index)){
                    Cursor.FIELD_TYPE_NULL -> row.put(name,JSONObject.NULL)
                    Cursor.FIELD_TYPE_INTEGER -> row.put(name,cursor.getLong(index))
                    Cursor.FIELD_TYPE_FLOAT -> row.put(name,cursor.getDouble(index))
                    Cursor.FIELD_TYPE_STRING -> row.put(name,cursor.getString(index))
                    Cursor.FIELD_TYPE_BLOB -> row.put(
                        name,
                        Base64.encodeToString(cursor.getBlob(index),Base64.NO_WRAP)
                    )
                }
            }
            result.put(row)
        }
        return result
    }

    private fun extract(uri:Uri,target:File){
        val rootPath=target.canonicalPath+File.separator
        context.contentResolver.openInputStream(uri)?.use{raw->
            ZipInputStream(raw.buffered()).use{zip->
                var entry=zip.nextEntry
                while(entry!=null){
                    val out=File(target,entry.name).canonicalFile
                    require(out.path.startsWith(rootPath)){"Geçersiz arşiv yolu."}
                    if(entry.isDirectory){
                        out.mkdirs()
                    }else{
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use{zip.copyTo(it)}
                    }
                    zip.closeEntry()
                    entry=zip.nextEntry
                }
            }
        } ?: error("Yedek dosyası açılamadı.")
    }

    private fun normalizeKindsForImport(tables:JSONObject){
        val relatedDefinitionIds=mutableSetOf<String>()
        val workItems=tables.getJSONArray("work_item_definitions")
        var legacyProjectId:String?=null
        for(i in 0 until workItems.length()){
            val row=workItems.getJSONObject(i)
            if(!row.has("kind") || row.isNull("kind")) row.put("kind","ELECTRICAL")
            if(row.optString("name")=="Mutfak Fayans / Dolap"){
                row.put("kind","RELATED_DISCIPLINE")
                relatedDefinitionIds += row.getString("id")
                legacyProjectId=row.optString("projectId").ifBlank{legacyProjectId}
            }
        }

        val definitions=tables.getJSONArray("problem_definitions")
        var completedDefinitionId:String?=null
        for(i in 0 until definitions.length()){
            val row=definitions.getJSONObject(i)
            if(!row.has("kind") || row.isNull("kind")) row.put("kind","PROBLEM")
            if(row.optString("code")=="L.İ.E."){
                row.put("kind","ADVANTAGE")
            }
            if(row.optString("code")=="TAM.İNŞ."){
                row.put("kind","PROBLEM")
                completedDefinitionId=row.getString("id")
            }
        }

        if(relatedDefinitionIds.isNotEmpty() && completedDefinitionId==null){
            val projectId=legacyProjectId ?: singleId(tables,"projects")
            completedDefinitionId="pd-import-related-complete"
            definitions.put(
                JSONObject()
                    .put("id",completedDefinitionId)
                    .put("projectId",projectId)
                    .put("code","TAM.İNŞ.")
                    .put("title","Aleyhimize tamamlanmış inşaat işi")
                    .put("description",JSONObject.NULL)
                    .put("tooltip","Elektrik işini etkileyen başka disiplin imalatı biz müdahale etmeden tamamlanmış.")
                    .put("active",1)
                    .put("kind","PROBLEM")
            )
        }

        val completedId=completedDefinitionId
        if(completedId!=null && relatedDefinitionIds.isNotEmpty()){
            val records=tables.getJSONArray("problem_records")
            val existing=mutableSetOf<Pair<String,String>>()
            for(i in 0 until records.length()){
                val row=records.getJSONObject(i)
                existing += row.optString("blockWorkItemId") to row.optString("problemDefinitionId")
            }

            val blockWorkItems=tables.getJSONArray("block_work_items")
            for(i in 0 until blockWorkItems.length()){
                val row=blockWorkItems.getJSONObject(i)
                if(
                    row.optString("workItemDefinitionId") in relatedDefinitionIds &&
                    row.optString("progressStatus")=="FINISHED"
                ){
                    val bwiId=row.getString("id")
                    if((bwiId to completedId) !in existing){
                        records.put(
                            JSONObject()
                                .put("id","pr-import-related-complete-"+bwiId)
                                .put("blockWorkItemId",bwiId)
                                .put("problemDefinitionId",completedId)
                                .put("status","OPEN")
                                .put("note",JSONObject.NULL)
                                .put("includeInReport",1)
                                .put("createdAt",row.optLong("updatedAt",System.currentTimeMillis()))
                                .put("closedAt",JSONObject.NULL)
                        )
                    }
                }
            }
        }
    }

    private fun singleId(tables:JSONObject,table:String):String{
        val array=tables.getJSONArray(table)
        require(array.length()==1){"Yedekte tek proje kaydı bekleniyordu."}
        return array.getJSONObject(0).getString("id")
    }

    private fun newIdMap(tables:JSONObject,table:String,prefix:String):Map<String,String>{
        val array=tables.getJSONArray(table)
        val result=linkedMapOf<String,String>()
        for(i in 0 until array.length()){
            val old=array.getJSONObject(i).getString("id")
            result[old]=prefix+"-"+UUID.randomUUID()
        }
        return result
    }

    private fun remap(tables:JSONObject,m:IdMaps){
        replaceIds(tables.getJSONArray("projects"),"id",m.projects)

        replaceIds(tables.getJSONArray("block_types"),"id",m.blockTypes)
        replaceIds(tables.getJSONArray("block_types"),"projectId",m.projects)

        replaceIds(tables.getJSONArray("blocks"),"id",m.blocks)
        replaceIds(tables.getJSONArray("blocks"),"projectId",m.projects)
        replaceIds(tables.getJSONArray("blocks"),"blockTypeId",m.blockTypes)

        replaceIds(tables.getJSONArray("work_item_definitions"),"id",m.workItems)
        replaceIds(tables.getJSONArray("work_item_definitions"),"projectId",m.projects)

        replaceIds(tables.getJSONArray("block_type_work_items"),"blockTypeId",m.blockTypes)
        replaceIds(tables.getJSONArray("block_type_work_items"),"workItemDefinitionId",m.workItems)

        replaceIds(tables.getJSONArray("block_work_items"),"id",m.blockWorkItems)
        replaceIds(tables.getJSONArray("block_work_items"),"blockId",m.blocks)
        replaceIds(tables.getJSONArray("block_work_items"),"workItemDefinitionId",m.workItems)

        replaceIds(tables.getJSONArray("problem_definitions"),"id",m.problemDefinitions)
        replaceIds(tables.getJSONArray("problem_definitions"),"projectId",m.projects)

        replaceIds(tables.getJSONArray("problem_records"),"id",m.problemRecords)
        replaceIds(tables.getJSONArray("problem_records"),"blockWorkItemId",m.blockWorkItems)
        replaceIds(tables.getJSONArray("problem_records"),"problemDefinitionId",m.problemDefinitions)

        replaceIds(tables.getJSONArray("notes"),"id",m.notes)
        replaceIds(tables.getJSONArray("notes"),"blockWorkItemId",m.blockWorkItems)

        replaceIds(tables.getJSONArray("photos"),"id",m.photos)
        replaceIds(tables.getJSONArray("photos"),"blockWorkItemId",m.blockWorkItems)

        replaceIds(tables.getJSONArray("block_attribute_definitions"),"id",m.attributes)
        replaceIds(tables.getJSONArray("block_attribute_definitions"),"projectId",m.projects)

        replaceIds(tables.getJSONArray("block_attribute_values"),"blockId",m.blocks)
        replaceIds(tables.getJSONArray("block_attribute_values"),"attributeDefinitionId",m.attributes)

        replaceIds(tables.getJSONArray("audit_events"),"id",m.audits)
        replaceIds(tables.getJSONArray("audit_events"),"blockWorkItemId",m.blockWorkItems)
    }

    private fun replaceIds(array:JSONArray,key:String,map:Map<String,String>){
        for(i in 0 until array.length()){
            val row=array.getJSONObject(i)
            if(!row.has(key) || row.isNull(key)) continue
            val old=row.getString(key)
            map[old]?.let{row.put(key,it)}
        }
    }

    private fun restorePhotos(
        tempRoot:File,
        tables:JSONObject,
        photoMap:Map<String,String>,
        newProjectId:String
    ){
        val array=tables.getJSONArray("photos")
        val byNewId=mutableMapOf<String,JSONObject>()
        for(i in 0 until array.length()){
            val row=array.getJSONObject(i)
            byNewId[row.getString("id")]=row
        }

        val pictureRoot=requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        val targetDir=File(pictureRoot,"Restored/"+newProjectId).apply{mkdirs()}

        photoMap.forEach{entry->
            val oldId=entry.key
            val newId=entry.value
            val source=File(tempRoot,"photos/"+oldId+".jpg")
            val row=byNewId[newId] ?: return@forEach
            if(source.isFile){
                val target=File(targetDir,newId+".jpg")
                source.copyTo(target,overwrite=true)
                val uri=FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID+".fileprovider",
                    target
                )
                row.put("localUri",uri.toString())
            }else{
                row.put("localUri","")
            }
        }
    }

    private suspend fun insertTables(tables:JSONObject)=database.withTransaction{
        val db=database.openHelper.writableDatabase
        val order=listOf(
            "projects",
            "block_types",
            "blocks",
            "work_item_definitions",
            "block_type_work_items",
            "block_work_items",
            "problem_definitions",
            "problem_records",
            "notes",
            "photos",
            "block_attribute_definitions",
            "block_attribute_values",
            "audit_events"
        )
        order.forEach{table->
            val array=tables.getJSONArray(table)
            for(i in 0 until array.length()){
                val values=jsonToContentValues(array.getJSONObject(i))
                val result=db.insert(table,SQLiteDatabase.CONFLICT_ABORT,values)
                check(result!=-1L){"Yedek içe aktarılırken $table tablosunda hata oluştu."}
            }
        }
    }

    private fun jsonToContentValues(row:JSONObject):ContentValues{
        val values=ContentValues()
        val keys=row.keys()
        while(keys.hasNext()){
            val key=keys.next()
            val value=row.get(key)
            when(value){
                JSONObject.NULL -> values.putNull(key)
                is Int -> values.put(key,value)
                is Long -> values.put(key,value)
                is Double -> values.put(key,value)
                is Boolean -> values.put(key,if(value)1 else 0)
                else -> values.put(key,value.toString())
            }
        }
        return values
    }

    private fun safe(value:String)=value
        .trim()
        .replace(Regex("""[^\p{L}\p{N}._-]+"""),"_")
        .take(80)
        .ifBlank{"saha-yedek"}

    private data class IdMaps(
        val projects:Map<String,String>,
        val blockTypes:Map<String,String>,
        val blocks:Map<String,String>,
        val workItems:Map<String,String>,
        val blockWorkItems:Map<String,String>,
        val problemDefinitions:Map<String,String>,
        val problemRecords:Map<String,String>,
        val notes:Map<String,String>,
        val photos:Map<String,String>,
        val attributes:Map<String,String>,
        val audits:Map<String,String>
    )
}
