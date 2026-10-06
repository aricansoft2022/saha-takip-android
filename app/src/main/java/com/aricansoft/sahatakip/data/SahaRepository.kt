package com.aricansoft.sahatakip.data

import com.aricansoft.sahatakip.data.db.*
import com.aricansoft.sahatakip.data.model.*
import java.util.Locale
import java.util.UUID

class SahaRepository(private val dao:SahaDao){
    fun observeProjects()=dao.observeProjects()
    fun observeBlocks(projectId:String)=dao.observeBlocks(projectId)
    fun observeBlockWorkItems(blockId:String)=dao.observeBlockWorkItems(blockId)
    fun observeBlockWorkItem(id:String)=dao.observeBlockWorkItem(id)
    fun observeWorkItemDefinitions(projectId:String)=dao.observeWorkItemDefinitions(projectId)
    fun observeProblemDefinitions(projectId:String)=dao.observeProblemDefinitions(projectId)
    fun observeProblemRecords(blockWorkItemId:String)=dao.observeProblemRecords(blockWorkItemId)
    fun observeNotes(blockWorkItemId:String)=dao.observeNotes(blockWorkItemId)
    fun observePhotos(blockWorkItemId:String)=dao.observePhotos(blockWorkItemId)
    fun observeBlockAttributes(blockId:String)=dao.observeBlockAttributes(blockId)

    suspend fun getBlock(id:String)=dao.getBlock(id)
    suspend fun getBlockType(id:String)=dao.getBlockType(id)
    suspend fun getWorkItemDefinition(id:String)=dao.getWorkItemDefinition(id)
    suspend fun getProjectIdForBlockWorkItem(id:String)=dao.getProjectIdForBlockWorkItem(id)
    suspend fun getPhotoContext(id:String)=dao.getPhotoContext(id)

    suspend fun setProgress(id:String,value:ProgressStatus)=mutateWorkItem(id,"İlerleme: "+value.label){copy(progressStatus=value)}
    suspend fun setQuality(id:String,value:QualityStatus)=mutateWorkItem(id,"Kalite: "+value.label){copy(qualityStatus=value)}
    suspend fun setControl(id:String,value:ControlStatus)=mutateWorkItem(id,"Kontrol: "+value.label){copy(controlStatus=value)}
    suspend fun setBlocked(id:String,value:Boolean)=mutateWorkItem(id,if(value)"Bloke edildi" else "Bloke kaldırıldı"){copy(isBlocked=value)}

    suspend fun attachWorkItem(blockId:String,definitionId:String){
        val now=System.currentTimeMillis()
        dao.insertBlockWorkItems(listOf(BlockWorkItemEntity(
            id="bwi-"+UUID.randomUUID(),
            blockId=blockId,
            workItemDefinitionId=definitionId,
            createdAt=now,
            updatedAt=now
        )))
    }

    suspend fun createWorkItemAndAttach(blockId:String,projectId:String,name:String,tooltip:String?):WorkItemDefinitionEntity{
        val item=WorkItemDefinitionEntity(
            id="wi-"+UUID.randomUUID(),
            projectId=projectId,
            name=name.trim(),
            tooltip=tooltip?.trim()?.ifBlank{null}
        )
        dao.insertWorkItemDefinitions(listOf(item))
        attachWorkItem(blockId,item.id)
        return item
    }

    suspend fun addNote(blockWorkItemId:String,text:String,includeInReport:Boolean){
        val now=System.currentTimeMillis()
        dao.insertNote(NoteEntity(UUID.randomUUID().toString(),blockWorkItemId,text.trim(),includeInReport,now))
        audit(blockWorkItemId,AuditEventType.NOTE_ADDED,"Not eklendi",now)
    }

    suspend fun attachProblem(blockWorkItemId:String,problemDefinitionId:String,note:String?=null,includeInReport:Boolean=true){
        val now=System.currentTimeMillis()
        dao.insertProblemRecord(ProblemRecordEntity(
            id=UUID.randomUUID().toString(),
            blockWorkItemId=blockWorkItemId,
            problemDefinitionId=problemDefinitionId,
            note=note?.trim()?.ifBlank{null},
            includeInReport=includeInReport,
            createdAt=now
        ))
        audit(blockWorkItemId,AuditEventType.PROBLEM_OPENED,"Problem eklendi",now)
    }

    suspend fun createProblemAndAttach(
        blockWorkItemId:String,
        projectId:String,
        code:String,
        title:String,
        tooltip:String?=null,
        includeInReport:Boolean=true
    ){
        val normalized=code.trim().uppercase(Locale.forLanguageTag("tr-TR"))
        val existing=dao.getProblemDefinitionByCode(projectId,normalized)
        val def=existing ?: ProblemDefinitionEntity(
            id=UUID.randomUUID().toString(),
            projectId=projectId,
            code=normalized,
            title=title.trim(),
            tooltip=tooltip?.trim()?.ifBlank{null}
        ).also{dao.insertProblemDefinition(it)}
        attachProblem(blockWorkItemId,def.id,includeInReport=includeInReport)
    }

    suspend fun closeProblem(recordId:String){
        val record=dao.getProblemRecord(recordId) ?: return
        if(record.status==ProblemRecordStatus.CLOSED) return
        val now=System.currentTimeMillis()
        dao.updateProblemRecord(record.copy(status=ProblemRecordStatus.CLOSED,closedAt=now))
        audit(record.blockWorkItemId,AuditEventType.PROBLEM_CLOSED,"Problem kapatıldı",now)
    }

    suspend fun addPhoto(blockWorkItemId:String,uri:String,includeInReport:Boolean=true){
        val now=System.currentTimeMillis()
        dao.insertPhoto(PhotoEntity(UUID.randomUUID().toString(),blockWorkItemId,uri,includeInReport=includeInReport,createdAt=now))
        audit(blockWorkItemId,AuditEventType.PHOTO_ADDED,"Fotoğraf eklendi",now)
    }

    private suspend fun mutateWorkItem(
        id:String,
        detail:String,
        transform:BlockWorkItemEntity.()->BlockWorkItemEntity
    ){
        val current=dao.getBlockWorkItem(id) ?: return
        val now=System.currentTimeMillis()
        dao.updateBlockWorkItem(current.transform().copy(updatedAt=now))
        audit(id,AuditEventType.STATUS_CHANGED,detail,now)
    }

    private suspend fun audit(blockWorkItemId:String,type:AuditEventType,detail:String,at:Long){
        dao.insertAuditEvent(AuditEventEntity(UUID.randomUUID().toString(),blockWorkItemId,type.name,detail,at))
    }
}
