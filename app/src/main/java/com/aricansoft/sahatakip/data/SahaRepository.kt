package com.aricansoft.sahatakip.data

import com.aricansoft.sahatakip.data.db.*
import com.aricansoft.sahatakip.data.model.*
import com.aricansoft.sahatakip.report.ProjectReportSnapshot
import java.util.Locale
import java.util.UUID

class SahaRepository(private val dao:SahaDao){
    fun observeProjects()=dao.observeProjects()
    fun observeProjectQuickStatuses()=dao.observeProjectQuickStatuses()
    fun observeProjectWorkItemQuickStatuses()=dao.observeProjectWorkItemQuickStatuses()
    fun observeBlocks(projectId:String)=dao.observeBlocks(projectId)
    fun observeBlockTypes(projectId:String)=dao.observeBlockTypes(projectId)
    fun observeBlockWorkItems(blockId:String)=dao.observeBlockWorkItems(blockId)
    fun observeBlockWorkItem(id:String)=dao.observeBlockWorkItem(id)
    fun observeWorkItemDefinitions(projectId:String)=dao.observeWorkItemDefinitions(projectId)
    fun observeProblemDefinitions(projectId:String)=dao.observeProblemDefinitions(projectId)
    fun observeDeficiencyDefinitions(projectId:String)=dao.observeDeficiencyDefinitions(projectId)
    fun observeProblemRecords(blockWorkItemId:String)=dao.observeProblemRecords(blockWorkItemId)
    fun observeProjectFindings(projectId:String)=dao.observeProjectFindings(projectId)
    fun observeDeficiencies(blockWorkItemId:String)=dao.observeDeficiencies(blockWorkItemId)
    fun observeProjectDeficiencies(projectId:String)=dao.observeProjectDeficiencies(projectId)
    fun observeNotes(blockWorkItemId:String)=dao.observeNotes(blockWorkItemId)
    fun observePhotos(blockWorkItemId:String)=dao.observePhotos(blockWorkItemId)
    fun observeFindingPhotos(problemRecordId:String)=dao.observeFindingPhotos(problemRecordId)
    fun observeDeficiencyPhotos(deficiencyId:String)=dao.observeDeficiencyPhotos(deficiencyId)
    fun observeBlockAttributes(blockId:String)=dao.observeBlockAttributes(blockId)
    fun observeBlockAttributeDefinitions(projectId:String)=dao.observeBlockAttributeDefinitions(projectId)
    fun observeProjectMatrixRows(projectId:String)=dao.observeProjectMatrixRows(projectId)

    suspend fun getBlock(id:String)=dao.getBlock(id)
    suspend fun getBlockType(id:String)=dao.getBlockType(id)
    suspend fun getWorkItemDefinition(id:String)=dao.getWorkItemDefinition(id)
    suspend fun getProjectIdForBlockWorkItem(id:String)=dao.getProjectIdForBlockWorkItem(id)
    suspend fun getPhotoContext(id:String)=dao.getPhotoContext(id)

    suspend fun createProject(name:String):ProjectEntity{
        val clean=name.trim()
        require(clean.isNotBlank()){"Proje adı boş olamaz."}
        val item=ProjectEntity(
            id="project-"+UUID.randomUUID(),
            name=clean,
            createdAt=System.currentTimeMillis()
        )
        dao.insertProjects(listOf(item))
        return item
    }

    suspend fun createBlockType(
        projectId:String,
        code:String,
        name:String?,
        tooltip:String?
    ):BlockTypeEntity{
        val normalized=code.trim().uppercase(Locale.forLanguageTag("tr-TR"))
        require(normalized.isNotBlank()){"Blok tipi kodu boş olamaz."}
        dao.getBlockTypeByCode(projectId,normalized)?.let{
            error(normalized+" blok tipi zaten tanımlı.")
        }
        val item=BlockTypeEntity(
            id="bt-"+UUID.randomUUID(),
            projectId=projectId,
            code=normalized,
            name=name?.trim()?.ifBlank{null} ?: normalized+" Tip",
            tooltip=tooltip?.trim()?.ifBlank{null}
        )
        dao.insertBlockTypes(listOf(item))
        return item
    }

    suspend fun createBlock(projectId:String,blockTypeId:String,sequence:Int):BlockEntity{
        require(sequence>0){"Blok numarası 0'dan büyük olmalı."}
        val type=dao.getBlockType(blockTypeId) ?: error("Blok tipi bulunamadı.")
        require(type.projectId==projectId){"Blok tipi bu projeye ait değil."}
        val code=type.code+"-"+sequence
        dao.getBlockByCode(projectId,code)?.let{error(code+" zaten mevcut.")}

        val now=System.currentTimeMillis()
        val block=BlockEntity(
            id="block-"+UUID.randomUUID(),
            projectId=projectId,
            blockTypeId=blockTypeId,
            code=code,
            sequence=sequence
        )
        dao.insertBlocks(listOf(block))

        val templateIds=dao.getTemplateWorkItemIds(blockTypeId)
        if(templateIds.isNotEmpty()){
            dao.insertBlockWorkItems(templateIds.map{definitionId->
                BlockWorkItemEntity(
                    id="bwi-"+UUID.randomUUID(),
                    blockId=block.id,
                    workItemDefinitionId=definitionId,
                    createdAt=now,
                    updatedAt=now
                )
            })
        }
        return block
    }

    suspend fun setBlockAttributeValue(
        blockId:String,
        attributeDefinitionId:String,
        value:String
    ){
        dao.insertBlockAttributeValues(listOf(
            BlockAttributeValueEntity(
                blockId=blockId,
                attributeDefinitionId=attributeDefinitionId,
                value=value.trim(),
                updatedAt=System.currentTimeMillis()
            )
        ))
    }

    suspend fun createBlockAttributeAndSet(
        blockId:String,
        projectId:String,
        name:String,
        tooltip:String?,
        value:String
    ):BlockAttributeDefinitionEntity{
        val cleanName=name.trim()
        require(cleanName.isNotBlank()){"Parametre adı boş olamaz."}
        val definition=BlockAttributeDefinitionEntity(
            id="bad-"+UUID.randomUUID(),
            projectId=projectId,
            key="custom_"+UUID.randomUUID().toString().replace("-",""),
            name=cleanName,
            tooltip=tooltip?.trim()?.ifBlank{null}
        )
        dao.insertBlockAttributeDefinitions(listOf(definition))
        setBlockAttributeValue(blockId,definition.id,value)
        return definition
    }

    suspend fun getProjectReportSnapshot(projectId:String):ProjectReportSnapshot?{
        val project=dao.getProject(projectId) ?: return null
        return ProjectReportSnapshot(
            projectId=project.id,
            projectName=project.name,
            generatedAt=System.currentTimeMillis(),
            workItems=dao.getReportWorkItems(projectId),
            problems=dao.getReportProblems(projectId),
            deficiencies=dao.getReportDeficiencies(projectId),
            notes=dao.getReportNotes(projectId),
            photos=dao.getReportPhotos(projectId)
        )
    }

    suspend fun setProgress(id:String,value:ProgressStatus)=mutateWorkItem(id,"İlerleme: "+value.label){copy(progressStatus=value)}
    suspend fun setQuality(id:String,value:QualityStatus)=mutateWorkItem(id,"Kalite: "+value.label){copy(qualityStatus=value)}
    suspend fun setControl(id:String,value:ControlStatus)=mutateWorkItem(id,"Kontrol: "+value.label){copy(controlStatus=value)}
    suspend fun setBlocked(id:String,value:Boolean)=mutateWorkItem(id,if(value)"Bloke edildi" else "Bloke kaldırıldı"){copy(isBlocked=value)}

    suspend fun attachWorkItem(
        sourceBlockId:String,
        definitionId:String,
        scope:WorkItemScope=WorkItemScope.THIS_BLOCK
    ){
        val sourceBlock=dao.getBlock(sourceBlockId) ?: return
        val targetBlocks=when(scope){
            WorkItemScope.THIS_BLOCK -> listOf(sourceBlock)
            WorkItemScope.BLOCK_TYPE -> dao.getBlocksForType(sourceBlock.blockTypeId)
            WorkItemScope.PROJECT -> dao.getBlocksForProject(sourceBlock.projectId)
        }
        val now=System.currentTimeMillis()
        dao.insertBlockWorkItems(targetBlocks.map{block->
            BlockWorkItemEntity(
                id="bwi-"+UUID.randomUUID(),
                blockId=block.id,
                workItemDefinitionId=definitionId,
                createdAt=now,
                updatedAt=now
            )
        })

        val templateTypes=when(scope){
            WorkItemScope.THIS_BLOCK -> emptyList()
            WorkItemScope.BLOCK_TYPE -> listOf(sourceBlock.blockTypeId)
            WorkItemScope.PROJECT -> dao.getBlockTypesForProject(sourceBlock.projectId).map{it.id}
        }
        if(templateTypes.isNotEmpty()){
            dao.insertBlockTypeWorkItems(templateTypes.map{
                BlockTypeWorkItemEntity(
                    blockTypeId=it,
                    workItemDefinitionId=definitionId,
                    sortOrder=9_999
                )
            })
        }
    }

    suspend fun createWorkItemAndAttach(
        blockId:String,
        projectId:String,
        name:String,
        tooltip:String?,
        scope:WorkItemScope,
        kind:WorkItemKind=WorkItemKind.ELECTRICAL
    ):WorkItemDefinitionEntity{
        val cleanName=name.trim()
        require(cleanName.isNotBlank()){"İmalat adı boş olamaz."}
        val normalized=normalizeWorkItemName(cleanName)
        val existing=dao.getWorkItemDefinitionsForProject(projectId)
            .firstOrNull{normalizeWorkItemName(it.name)==normalized}
        if(existing!=null){
            attachWorkItem(blockId,existing.id,scope)
            return existing
        }
        val item=WorkItemDefinitionEntity(
            id="wi-"+UUID.randomUUID(),
            projectId=projectId,
            name=cleanName,
            tooltip=tooltip?.trim()?.ifBlank{null},
            kind=kind
        )
        dao.insertWorkItemDefinitions(listOf(item))
        attachWorkItem(blockId,item.id,scope)
        return item
    }

    suspend fun addNote(blockWorkItemId:String,text:String,includeInReport:Boolean){
        val now=System.currentTimeMillis()
        dao.insertNote(NoteEntity(UUID.randomUUID().toString(),blockWorkItemId,text.trim(),includeInReport,now))
        audit(blockWorkItemId,AuditEventType.NOTE_ADDED,"Not eklendi",now)
    }

    suspend fun createProblemDefinition(
        projectId:String,
        code:String,
        title:String,
        tooltip:String?=null,
        kind:FindingKind=FindingKind.PROBLEM
    ):ProblemDefinitionEntity{
        val normalized=code.trim().uppercase(Locale.forLanguageTag("tr-TR"))
        require(normalized.isNotBlank()){"Kod boş olamaz."}
        val cleanTitle=title.trim()
        require(cleanTitle.isNotBlank()){"Tanım boş olamaz."}
        dao.getProblemDefinitionByCode(projectId,normalized)?.let{existing->
            if(existing.kind==kind){
                error(normalized+" kodu zaten tanımlı.")
            }
            error(normalized+" kodu zaten "+existing.kind.label.lowercase(Locale.forLanguageTag("tr-TR"))+" olarak kullanılıyor.")
        }
        return ProblemDefinitionEntity(
            id="pd-"+UUID.randomUUID(),
            projectId=projectId,
            code=normalized,
            title=cleanTitle,
            tooltip=tooltip?.trim()?.ifBlank{null},
            kind=kind
        ).also{dao.insertProblemDefinition(it)}
    }

    suspend fun createDeficiencyDefinition(
        projectId:String,
        title:String,
        description:String?=null
    ):DeficiencyDefinitionEntity{
        val cleanTitle=title.trim()
        require(cleanTitle.isNotBlank()){"Eksik tanımı boş olamaz."}
        val normalized=normalizeWorkItemName(cleanTitle)
        dao.getDeficiencyDefinitionsForProject(projectId)
            .firstOrNull{normalizeWorkItemName(it.title)==normalized}
            ?.let{error("Bu eksik tanımı zaten mevcut.")}
        return DeficiencyDefinitionEntity(
            id="dd-"+UUID.randomUUID(),
            projectId=projectId,
            title=cleanTitle,
            description=description?.trim()?.ifBlank{null}
        ).also{dao.insertDeficiencyDefinition(it)}
    }

    suspend fun attachProblem(
        blockWorkItemId:String,
        problemDefinitionId:String,
        note:String?=null,
        specificDescription:String?=null,
        floor:String?=null,
        unitNumber:String?=null,
        unitName:String?=null,
        includeInReport:Boolean=true
    ){
        val definition=dao.getProblemDefinition(problemDefinitionId)
            ?: error("Problem/avantaj tanımı bulunamadı.")
        val now=System.currentTimeMillis()
        dao.insertProblemRecord(ProblemRecordEntity(
            id=UUID.randomUUID().toString(),
            blockWorkItemId=blockWorkItemId,
            problemDefinitionId=problemDefinitionId,
            note=note?.trim()?.ifBlank{null},
            specificDescription=specificDescription?.trim()?.ifBlank{null},
            floor=floor?.trim()?.ifBlank{null},
            unitNumber=unitNumber?.trim()?.ifBlank{null},
            unitName=unitName?.trim()?.ifBlank{null},
            includeInReport=includeInReport,
            createdAt=now
        ))
        val event=if(definition.kind==FindingKind.ADVANTAGE){
            AuditEventType.ADVANTAGE_OPENED
        }else{
            AuditEventType.PROBLEM_OPENED
        }
        audit(
            blockWorkItemId,
            event,
            if(definition.kind==FindingKind.ADVANTAGE)"Avantaj eklendi" else "Problem eklendi",
            now
        )
    }

    suspend fun createProblemAndAttach(
        blockWorkItemId:String,
        projectId:String,
        code:String,
        title:String,
        tooltip:String?=null,
        specificDescription:String?=null,
        floor:String?=null,
        unitNumber:String?=null,
        unitName:String?=null,
        includeInReport:Boolean=true,
        kind:FindingKind=FindingKind.PROBLEM
    ){
        val normalized=code.trim().uppercase(Locale.forLanguageTag("tr-TR"))
        require(normalized.isNotBlank()){"Kod boş olamaz."}
        require(title.trim().isNotBlank()){"Tanım boş olamaz."}
        val existing=dao.getProblemDefinitionByCode(projectId,normalized)
        if(existing!=null && existing.kind!=kind){
            error(normalized+" kodu zaten "+existing.kind.label.lowercase(Locale.forLanguageTag("tr-TR"))+" olarak tanımlı.")
        }
        val def=existing ?: ProblemDefinitionEntity(
            id=UUID.randomUUID().toString(),
            projectId=projectId,
            code=normalized,
            title=title.trim(),
            tooltip=tooltip?.trim()?.ifBlank{null},
            kind=kind
        ).also{dao.insertProblemDefinition(it)}
        attachProblem(
            blockWorkItemId=blockWorkItemId,
            problemDefinitionId=def.id,
            specificDescription=specificDescription,
            floor=floor,
            unitNumber=unitNumber,
            unitName=unitName,
            includeInReport=includeInReport
        )
    }

    suspend fun createDeficiency(
        blockWorkItemId:String,
        title:String,
        description:String?=null,
        floor:String?=null,
        unitNumber:String?=null,
        unitName:String?=null,
        targetDate:String?=null,
        priority:DeficiencyPriority=DeficiencyPriority.NORMAL,
        includeInReport:Boolean=true
    ):DeficiencyEntity{
        val cleanTitle=title.trim()
        require(cleanTitle.isNotBlank()){"Eksik tanımı boş olamaz."}
        val cleanTarget=validateTargetDate(targetDate)
        val now=System.currentTimeMillis()
        val item=DeficiencyEntity(
            id="def-"+UUID.randomUUID(),
            blockWorkItemId=blockWorkItemId,
            title=cleanTitle,
            description=description?.trim()?.ifBlank{null},
            floor=floor?.trim()?.ifBlank{null},
            unitNumber=unitNumber?.trim()?.ifBlank{null},
            unitName=unitName?.trim()?.ifBlank{null},
            targetDate=cleanTarget,
            priority=priority,
            status=DeficiencyStatus.OPEN,
            includeInReport=includeInReport,
            createdAt=now,
            updatedAt=now
        )
        dao.insertDeficiency(item)
        audit(blockWorkItemId,AuditEventType.DEFICIENCY_CREATED,"Eksik açıldı: "+cleanTitle,now)
        return item
    }

    suspend fun updateDeficiency(
        id:String,
        title:String,
        description:String?=null,
        floor:String?=null,
        unitNumber:String?=null,
        unitName:String?=null,
        targetDate:String?=null,
        priority:DeficiencyPriority=DeficiencyPriority.NORMAL,
        includeInReport:Boolean=true
    ){
        val current=dao.getDeficiency(id) ?: return
        val cleanTitle=title.trim()
        require(cleanTitle.isNotBlank()){"Eksik tanımı boş olamaz."}
        val cleanTarget=validateTargetDate(targetDate)
        val now=System.currentTimeMillis()
        dao.updateDeficiency(
            current.copy(
                title=cleanTitle,
                description=description?.trim()?.ifBlank{null},
                floor=floor?.trim()?.ifBlank{null},
                unitNumber=unitNumber?.trim()?.ifBlank{null},
                unitName=unitName?.trim()?.ifBlank{null},
                targetDate=cleanTarget,
                priority=priority,
                includeInReport=includeInReport,
                updatedAt=now
            )
        )
        audit(current.blockWorkItemId,AuditEventType.DEFICIENCY_UPDATED,"Eksik güncellendi: "+cleanTitle,now)
    }

    suspend fun setDeficiencyStatus(id:String,status:DeficiencyStatus){
        val current=dao.getDeficiency(id) ?: return
        if(current.status==status) return
        val now=System.currentTimeMillis()
        dao.updateDeficiency(current.copy(status=status,updatedAt=now))
        audit(
            current.blockWorkItemId,
            AuditEventType.DEFICIENCY_STATUS_CHANGED,
            "Eksik durumu: "+status.label+" — "+current.title,
            now
        )
    }

    suspend fun setDeficiencyReportInclusion(id:String,include:Boolean)=
        dao.setDeficiencyReportInclusion(id,include)

    suspend fun closeProblem(recordId:String){
        val record=dao.getProblemRecord(recordId) ?: return
        if(record.status==ProblemRecordStatus.CLOSED) return
        val definition=dao.getProblemDefinition(record.problemDefinitionId)
        val now=System.currentTimeMillis()
        dao.updateProblemRecord(record.copy(status=ProblemRecordStatus.CLOSED,closedAt=now))
        val isAdvantage=definition?.kind==FindingKind.ADVANTAGE
        audit(
            record.blockWorkItemId,
            if(isAdvantage)AuditEventType.ADVANTAGE_CLOSED else AuditEventType.PROBLEM_CLOSED,
            if(isAdvantage)"Avantaj kapatıldı" else "Problem kapatıldı",
            now
        )
    }

    suspend fun addPhoto(
        blockWorkItemId:String,
        uri:String,
        includeInReport:Boolean=true,
        problemRecordId:String?=null,
        deficiencyId:String?=null
    ){
        require(problemRecordId==null || deficiencyId==null){
            "Bir fotoğraf aynı anda hem problem/avantaj hem eksik kaydına bağlanamaz."
        }
        problemRecordId?.let{id->
            val record=dao.getProblemRecord(id) ?: error("Problem/avantaj kaydı bulunamadı.")
            require(record.blockWorkItemId==blockWorkItemId){"Fotoğraf problem/avantaj bağlamı uyuşmuyor."}
        }
        deficiencyId?.let{id->
            val record=dao.getDeficiency(id) ?: error("Eksik kaydı bulunamadı.")
            require(record.blockWorkItemId==blockWorkItemId){"Fotoğraf eksik bağlamı uyuşmuyor."}
        }
        val now=System.currentTimeMillis()
        dao.insertPhoto(
            PhotoEntity(
                id=UUID.randomUUID().toString(),
                blockWorkItemId=blockWorkItemId,
                problemRecordId=problemRecordId,
                deficiencyId=deficiencyId,
                localUri=uri,
                includeInReport=includeInReport,
                createdAt=now
            )
        )
        audit(
            blockWorkItemId,
            AuditEventType.PHOTO_ADDED,
            when{
                deficiencyId!=null -> "Eksik kanıt fotoğrafı eklendi"
                problemRecordId!=null -> "Problem/avantaj kanıt fotoğrafı eklendi"
                else -> "İmalat fotoğrafı eklendi"
            },
            now
        )
    }

    suspend fun setProblemReportInclusion(id:String,include:Boolean)=
        dao.setProblemReportInclusion(id,include)

    suspend fun setNoteReportInclusion(id:String,include:Boolean)=
        dao.setNoteReportInclusion(id,include)

    suspend fun setPhotoReportInclusion(id:String,include:Boolean)=
        dao.setPhotoReportInclusion(id,include)

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
