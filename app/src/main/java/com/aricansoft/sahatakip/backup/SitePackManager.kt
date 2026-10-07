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
import java.security.MessageDigest
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
    companion object {
        const val GKTE_MIME="application/vnd.aricansoft.gkte"
        const val GKTE_FORMAT="GKTE"
        const val GKTE_FORMAT_VERSION=1
        private const val MAX_ARCHIVE_ENTRIES=5_000
        private const val MAX_ENTRY_BYTES=100L*1024L*1024L
        private const val MAX_TOTAL_UNCOMPRESSED_BYTES=2L*1024L*1024L*1024L
        private const val MAX_RETAINED_EXPORTS=5
    }
    suspend fun exportGkte(projectId:String):Uri{
        val root=dumpProject(projectId)
        val projectMeta=root.getJSONObject("meta")
        val project=projectMeta.getString("projectName")
        val photos=root.getJSONObject("tables").getJSONArray("photos")

        val photoSources=linkedMapOf<String,String>()
        for(i in 0 until photos.length()){
            val row=photos.getJSONObject(i)
            val id=row.getString("id")
            val localUri=row.optString("localUri")
            require(localUri.isNotBlank()){
                "Fotoğraf kaydı dosya bağı içermiyor: $id. GKTE eksik kanıtla oluşturulmadı."
            }
            photoSources[id]=localUri
            row.put("localUri","")
        }

        val staging=File(context.cacheDir,"gkte-export-"+UUID.randomUUID()).apply{mkdirs()}
        val dir=File(context.filesDir,"backups").apply{mkdirs()}
        val stamp=DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .format(Instant.now().atZone(ZoneId.systemDefault()))
        val output=File(dir,safe(project)+"_"+stamp+".gkte")

        try{
            val assets=JSONArray()
            photoSources.forEach{(id,uriString)->
                val target=File(staging,id+".jpg")
                val digest=MessageDigest.getInstance("SHA-256")
                var size=0L
                val input=context.contentResolver.openInputStream(Uri.parse(uriString))
                    ?: error("Fotoğraf okunamadı: $id")
                input.use{source->
                    FileOutputStream(target).buffered().use{out->
                        val buffer=ByteArray(DEFAULT_BUFFER_SIZE)
                        while(true){
                            val read=source.read(buffer)
                            if(read<0) break
                            size+=read
                            require(size<=MAX_ENTRY_BYTES){
                                "Fotoğraf GKTE sınırını aşıyor: $id"
                            }
                            digest.update(buffer,0,read)
                            out.write(buffer,0,read)
                        }
                    }
                }
                require(size>0){"Fotoğraf dosyası boş: $id"}
                assets.put(
                    JSONObject()
                        .put("path","photos/$id.jpg")
                        .put("size",size)
                        .put("sha256",digest.digest().toHex())
                )
            }

            val dataBytes=root.toString().toByteArray(Charsets.UTF_8)
            val manifest=JSONObject()
                .put("format",GKTE_FORMAT)
                .put("formatVersion",GKTE_FORMAT_VERSION)
                .put("minReaderVersion",1)
                .put("databaseVersion",root.optInt("databaseVersion"))
                .put("fileExtension",".gkte")
                .put("mimeType",GKTE_MIME)
                .put("encoding","UTF-8")
                .put("payload","data.json")
                .put("payloadSha256",sha256(dataBytes))
                .put("assetsRoot","photos/")
                .put("assets",assets)
                .put("project",JSONObject()
                    .put("id",projectMeta.getString("projectId"))
                    .put("name",project)
                )
                .put("exportedAt",projectMeta.getLong("exportedAt"))
                .put("producer",JSONObject()
                    .put("application","Saha Takip")
                    .put("platform","android")
                )

            ZipOutputStream(FileOutputStream(output).buffered()).use{zip->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(dataBytes)
                zip.closeEntry()

                for(i in 0 until assets.length()){
                    val asset=assets.getJSONObject(i)
                    val path=asset.getString("path")
                    val id=path.substringAfter("photos/").substringBeforeLast(".jpg")
                    zip.putNextEntry(ZipEntry(path))
                    File(staging,id+".jpg").inputStream().buffered().use{it.copyTo(zip)}
                    zip.closeEntry()
                }
            }

            pruneFiles(dir,".gkte",MAX_RETAINED_EXPORTS)
            return FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID+".fileprovider",
                output
            )
        }catch(error:Throwable){
            output.delete()
            throw error
        }finally{
            staging.deleteRecursively()
        }
    }

    @Deprecated("Use exportGkte")
    suspend fun exportProject(projectId:String):Uri=exportGkte(projectId)

    suspend fun importProject(packageUri:Uri):String{
        val tempRoot=File(context.cacheDir,"gkte-"+UUID.randomUUID()).apply{mkdirs()}
        try{
            extract(packageUri,tempRoot)

            val manifestFile=File(tempRoot,"manifest.json")
            val strictPackage=manifestFile.isFile
            if(strictPackage){
                val manifest=JSONObject(manifestFile.readText(Charsets.UTF_8))
                require(manifest.optString("format")==GKTE_FORMAT){
                    "Bu dosya GKTE proje paketi değil."
                }
                require(manifest.optInt("formatVersion")==GKTE_FORMAT_VERSION){
                    "Desteklenmeyen GKTE sürümü: "+manifest.optInt("formatVersion")
                }
                require(manifest.optString("payload","data.json")=="data.json"){
                    "GKTE payload tanımı desteklenmiyor."
                }
                validateManifestIntegrity(manifest,tempRoot)
            }

            val dataFile=File(tempRoot,"data.json")
            require(dataFile.isFile){"Geçerli bir .gkte proje dosyası değil: data.json bulunamadı."}
            val root=JSONObject(dataFile.readText(Charsets.UTF_8))
            require(root.optInt("formatVersion")==1){
                "Desteklenmeyen veri formatı sürümü."
            }
            val tables=root.getJSONObject("tables")
            normalizeKindsForImport(tables)
            validateImportGraph(tables,strictPackage)
            if(!tables.has("deficiencies")){
                tables.put("deficiencies",JSONArray())
            }

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
                deficiencies=newIdMap(tables,"deficiencies","def"),
                notes=newIdMap(tables,"notes","note"),
                photos=newIdMap(tables,"photos","photo"),
                attributes=newIdMap(tables,"block_attribute_definitions","bad"),
                audits=newIdMap(tables,"audit_events","audit")
            )

            remap(tables,maps)
            val restoredDir=restorePhotos(tempRoot,tables,maps.photos,newProject,strictPackage)
            try{
                insertTables(tables)
            }catch(error:Throwable){
                restoredDir.deleteRecursively()
                throw error
            }
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
            "deficiencies" to """
                SELECT d.* FROM deficiencies d
                JOIN block_work_items bwi ON bwi.id=d.blockWorkItemId
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
            .put("databaseVersion",6)
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
        val seen=mutableSetOf<String>()
        var entryCount=0
        var totalBytes=0L
        context.contentResolver.openInputStream(uri)?.use{raw->
            ZipInputStream(raw.buffered()).use{zip->
                var entry=zip.nextEntry
                while(entry!=null){
                    entryCount++
                    require(entryCount<=MAX_ARCHIVE_ENTRIES){"GKTE çok fazla arşiv girdisi içeriyor."}
                    require(entry.name.isNotBlank() && seen.add(entry.name)){"Geçersiz veya yinelenen arşiv girdisi."}
                    val out=File(target,entry.name).canonicalFile
                    require(out.path.startsWith(rootPath)){"Geçersiz arşiv yolu."}
                    if(entry.isDirectory){
                        out.mkdirs()
                    }else{
                        out.parentFile?.mkdirs()
                        var entryBytes=0L
                        FileOutputStream(out).buffered().use{output->
                            val buffer=ByteArray(DEFAULT_BUFFER_SIZE)
                            while(true){
                                val read=zip.read(buffer)
                                if(read<0) break
                                entryBytes+=read
                                totalBytes+=read
                                require(entryBytes<=MAX_ENTRY_BYTES){"GKTE içindeki tek dosya boyut sınırını aşıyor."}
                                require(totalBytes<=MAX_TOTAL_UNCOMPRESSED_BYTES){"GKTE açılmış toplam boyut sınırını aşıyor."}
                                output.write(buffer,0,read)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry=zip.nextEntry
                }
            }
        } ?: error("Yedek dosyası açılamadı.")
    }

    private fun validateManifestIntegrity(manifest:JSONObject,tempRoot:File){
        val data=File(tempRoot,"data.json")
        require(data.isFile){"GKTE payload dosyası bulunamadı."}
        manifest.optString("payloadSha256").takeIf{it.isNotBlank()}?.let{expected->
            require(sha256(data.readBytes()).equals(expected,ignoreCase=true)){
                "GKTE data.json bütünlük doğrulaması başarısız."
            }
        }

        val assets=manifest.optJSONArray("assets") ?: return
        val seen=mutableSetOf<String>()
        for(i in 0 until assets.length()){
            val asset=assets.getJSONObject(i)
            val path=asset.getString("path")
            require(path.startsWith("photos/") && path.endsWith(".jpg") && seen.add(path)){
                "GKTE asset tanımı geçersiz."
            }
            val file=File(tempRoot,path).canonicalFile
            require(file.path.startsWith(tempRoot.canonicalPath+File.separator) && file.isFile){
                "GKTE asset dosyası eksik: $path"
            }
            val expectedSize=asset.getLong("size")
            require(file.length()==expectedSize){"GKTE asset boyutu uyuşmuyor: $path"}
            require(sha256(file).equals(asset.getString("sha256"),ignoreCase=true)){
                "GKTE asset bütünlük doğrulaması başarısız: $path"
            }
        }
    }

    private fun validateImportGraph(tables:JSONObject,strictAssets:Boolean){
        val required=listOf(
            "projects","block_types","blocks","work_item_definitions","block_type_work_items",
            "block_work_items","problem_definitions","problem_records","deficiencies","notes",
            "photos","block_attribute_definitions","block_attribute_values","audit_events"
        )
        required.forEach{require(tables.has(it)){"GKTE tablosu eksik: $it"}}

        fun ids(table:String):Set<String>{
            val array=tables.getJSONArray(table)
            val result=linkedSetOf<String>()
            for(i in 0 until array.length()){
                val id=array.getJSONObject(i).getString("id")
                require(result.add(id)){"GKTE içinde yinelenen kimlik: $table / $id"}
            }
            return result
        }
        val projectIds=ids("projects")
        require(projectIds.size==1){"GKTE içinde tam bir proje kaydı bekleniyor."}
        val blockTypeIds=ids("block_types")
        val blockIds=ids("blocks")
        val workIds=ids("work_item_definitions")
        val bwiIds=ids("block_work_items")
        val problemDefIds=ids("problem_definitions")
        val problemRecordIds=ids("problem_records")
        val deficiencyIds=ids("deficiencies")
        ids("notes")
        ids("photos")
        val attributeIds=ids("block_attribute_definitions")
        ids("audit_events")

        fun requireRef(row:JSONObject,key:String,valid:Set<String>,table:String){
            require(row.has(key) && !row.isNull(key) && row.getString(key) in valid){
                "GKTE ilişki hatası: $table.$key"
            }
        }
        forEachRow(tables,"block_types"){requireRef(it,"projectId",projectIds,"block_types")}
        forEachRow(tables,"blocks"){
            requireRef(it,"projectId",projectIds,"blocks")
            requireRef(it,"blockTypeId",blockTypeIds,"blocks")
        }
        forEachRow(tables,"work_item_definitions"){requireRef(it,"projectId",projectIds,"work_item_definitions")}
        forEachRow(tables,"block_type_work_items"){
            requireRef(it,"blockTypeId",blockTypeIds,"block_type_work_items")
            requireRef(it,"workItemDefinitionId",workIds,"block_type_work_items")
        }
        val bwiOwner=mutableMapOf<String,String>()
        forEachRow(tables,"block_work_items"){
            requireRef(it,"blockId",blockIds,"block_work_items")
            requireRef(it,"workItemDefinitionId",workIds,"block_work_items")
            bwiOwner[it.getString("id")]=it.getString("blockId")
        }
        forEachRow(tables,"problem_definitions"){requireRef(it,"projectId",projectIds,"problem_definitions")}
        val problemOwner=mutableMapOf<String,String>()
        forEachRow(tables,"problem_records"){
            requireRef(it,"blockWorkItemId",bwiIds,"problem_records")
            requireRef(it,"problemDefinitionId",problemDefIds,"problem_records")
            problemOwner[it.getString("id")]=it.getString("blockWorkItemId")
        }
        val deficiencyOwner=mutableMapOf<String,String>()
        forEachRow(tables,"deficiencies"){
            requireRef(it,"blockWorkItemId",bwiIds,"deficiencies")
            deficiencyOwner[it.getString("id")]=it.getString("blockWorkItemId")
        }
        forEachRow(tables,"notes"){requireRef(it,"blockWorkItemId",bwiIds,"notes")}
        forEachRow(tables,"block_attribute_definitions"){requireRef(it,"projectId",projectIds,"block_attribute_definitions")}
        forEachRow(tables,"block_attribute_values"){
            requireRef(it,"blockId",blockIds,"block_attribute_values")
            requireRef(it,"attributeDefinitionId",attributeIds,"block_attribute_values")
        }
        forEachRow(tables,"audit_events"){requireRef(it,"blockWorkItemId",bwiIds,"audit_events")}
        forEachRow(tables,"photos"){row->
            val bwi=row.getString("blockWorkItemId")
            require(bwi in bwiIds){"GKTE fotoğraf imalat ilişkisi geçersiz."}
            val problem=row.optString("problemRecordId").takeIf{it.isNotBlank()}
            val deficiency=row.optString("deficiencyId").takeIf{it.isNotBlank()}
            require(problem==null || deficiency==null){"GKTE fotoğrafı iki kanıt kaydına birden bağlı."}
            if(problem!=null){
                require(problem in problemRecordIds && problemOwner[problem]==bwi){"GKTE fotoğraf problem bağlamı uyuşmuyor."}
            }
            if(deficiency!=null){
                require(deficiency in deficiencyIds && deficiencyOwner[deficiency]==bwi){"GKTE fotoğraf eksik bağlamı uyuşmuyor."}
            }
            if(strictAssets){
                require(row.optString("localUri").isBlank()){"GKTE fotoğraf URI alanı platform bağımsız olmalı."}
            }
        }
    }

    private fun forEachRow(tables:JSONObject,table:String,action:(JSONObject)->Unit){
        val array=tables.getJSONArray(table)
        for(i in 0 until array.length()) action(array.getJSONObject(i))
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

        replaceIds(tables.getJSONArray("deficiencies"),"id",m.deficiencies)
        replaceIds(tables.getJSONArray("deficiencies"),"blockWorkItemId",m.blockWorkItems)

        replaceIds(tables.getJSONArray("notes"),"id",m.notes)
        replaceIds(tables.getJSONArray("notes"),"blockWorkItemId",m.blockWorkItems)

        replaceIds(tables.getJSONArray("photos"),"id",m.photos)
        replaceIds(tables.getJSONArray("photos"),"blockWorkItemId",m.blockWorkItems)
        replaceIds(tables.getJSONArray("photos"),"problemRecordId",m.problemRecords)
        replaceIds(tables.getJSONArray("photos"),"deficiencyId",m.deficiencies)

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
        newProjectId:String,
        strictAssets:Boolean
    ):File{
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
                require(!strictAssets){"GKTE fotoğraf asset'i eksik: $oldId"}
                row.put("localUri","")
            }
        }
        return targetDir
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
            "deficiencies",
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

    private fun sha256(bytes:ByteArray):String=
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun sha256(file:File):String{
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use{input->
            val buffer=ByteArray(DEFAULT_BUFFER_SIZE)
            while(true){
                val read=input.read(buffer)
                if(read<0) break
                digest.update(buffer,0,read)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex():String=joinToString(""){"%02x".format(it)}

    private fun pruneFiles(dir:File,extension:String,keep:Int){
        dir.listFiles()
            ?.filter{it.isFile && it.name.endsWith(extension,ignoreCase=true)}
            ?.sortedByDescending{it.lastModified()}
            ?.drop(keep)
            ?.forEach{it.delete()}
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
        val deficiencies:Map<String,String>,
        val notes:Map<String,String>,
        val photos:Map<String,String>,
        val attributes:Map<String,String>,
        val audits:Map<String,String>
    )
}
