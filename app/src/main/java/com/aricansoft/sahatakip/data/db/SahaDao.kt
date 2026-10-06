package com.aricansoft.sahatakip.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class BlockWorkItemRow(
    val id:String,
    val blockId:String,
    val workItemDefinitionId:String,
    val name:String,
    val code:String?,
    val tooltip:String?,
    val kind:com.aricansoft.sahatakip.data.model.WorkItemKind,
    val progressStatus:com.aricansoft.sahatakip.data.model.ProgressStatus,
    val qualityStatus:com.aricansoft.sahatakip.data.model.QualityStatus,
    val controlStatus:com.aricansoft.sahatakip.data.model.ControlStatus,
    val isBlocked:Boolean,
    val openProblemCount:Int,
    val openAdvantageCount:Int,
    val updatedAt:Long
)

data class ProblemRecordRow(
    val id:String,
    val blockWorkItemId:String,
    val problemDefinitionId:String,
    val code:String,
    val title:String,
    val tooltip:String?,
    val kind:com.aricansoft.sahatakip.data.model.FindingKind,
    val status:com.aricansoft.sahatakip.data.model.ProblemRecordStatus,
    val note:String?,
    val includeInReport:Boolean,
    val createdAt:Long,
    val closedAt:Long?
)

data class BlockAttributeRow(
    val attributeDefinitionId:String,
    val key:String,
    val name:String,
    val tooltip:String?,
    val value:String
)
data class PhotoContextRow(val projectName:String,val blockCode:String,val workItemName:String)

data class ProjectQuickStatusRow(
    val projectId:String,
    val totalWorkItemCount:Int,
    val inProgressCount:Int,
    val finishedCount:Int,
    val defectiveCount:Int,
    val blockedCount:Int,
    val openProblemCount:Int,
    val openAdvantageCount:Int
)

data class ReportWorkItemRow(
    val blockWorkItemId:String,
    val blockCode:String,
    val workItemName:String,
    val workItemKind:com.aricansoft.sahatakip.data.model.WorkItemKind,
    val progressStatus:com.aricansoft.sahatakip.data.model.ProgressStatus,
    val qualityStatus:com.aricansoft.sahatakip.data.model.QualityStatus,
    val controlStatus:com.aricansoft.sahatakip.data.model.ControlStatus,
    val isBlocked:Boolean,
    val openProblemCount:Int,
    val openAdvantageCount:Int
)

data class ReportProblemRow(
    val blockWorkItemId:String,
    val code:String,
    val title:String,
    val kind:com.aricansoft.sahatakip.data.model.FindingKind,
    val note:String?,
    val status:com.aricansoft.sahatakip.data.model.ProblemRecordStatus,
    val createdAt:Long,
    val closedAt:Long?
)

data class ReportNoteRow(
    val blockWorkItemId:String,
    val text:String,
    val createdAt:Long
)

data class ReportPhotoRow(
    val blockWorkItemId:String,
    val localUri:String,
    val caption:String?,
    val createdAt:Long
)

@Dao
interface SahaDao {
    @Query("SELECT * FROM projects ORDER BY name")
    fun observeProjects():Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id=:id")
    suspend fun getProject(id:String):ProjectEntity?

    @Query("""
        SELECT
            p.id AS projectId,
            (
                SELECT COUNT(*)
                FROM block_work_items bwi
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=p.id
            ) AS totalWorkItemCount,
            (
                SELECT COUNT(*)
                FROM block_work_items bwi
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=p.id AND bwi.progressStatus='IN_PROGRESS'
            ) AS inProgressCount,
            (
                SELECT COUNT(*)
                FROM block_work_items bwi
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=p.id AND bwi.progressStatus='FINISHED'
            ) AS finishedCount,
            (
                SELECT COUNT(*)
                FROM block_work_items bwi
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=p.id
                  AND bwi.qualityStatus IN ('DEFECTIVE','CRITICAL_DEFECT')
            ) AS defectiveCount,
            (
                SELECT COUNT(*)
                FROM block_work_items bwi
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=p.id AND bwi.isBlocked=1
            ) AS blockedCount,
            (
                SELECT COUNT(*)
                FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                JOIN block_work_items bwi ON bwi.id=pr.blockWorkItemId
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=p.id
                  AND pr.status='OPEN'
                  AND pd.kind='PROBLEM'
            ) AS openProblemCount,
            (
                SELECT COUNT(*)
                FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                JOIN block_work_items bwi ON bwi.id=pr.blockWorkItemId
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=p.id
                  AND pr.status='OPEN'
                  AND pd.kind='ADVANTAGE'
            ) AS openAdvantageCount
        FROM projects p
        ORDER BY p.name
    """)
    fun observeProjectQuickStatuses():Flow<List<ProjectQuickStatusRow>>

    @Query("SELECT * FROM blocks WHERE projectId=:projectId ORDER BY blockTypeId, sequence")
    fun observeBlocks(projectId:String):Flow<List<BlockEntity>>

    @Query("SELECT * FROM block_types WHERE projectId=:projectId ORDER BY code")
    fun observeBlockTypes(projectId:String):Flow<List<BlockTypeEntity>>

    @Query("SELECT * FROM blocks WHERE id=:id")
    suspend fun getBlock(id:String):BlockEntity?

    @Query("SELECT * FROM blocks WHERE projectId=:projectId AND code=:code LIMIT 1")
    suspend fun getBlockByCode(projectId:String,code:String):BlockEntity?

    @Query("SELECT * FROM blocks WHERE blockTypeId=:blockTypeId ORDER BY sequence")
    suspend fun getBlocksForType(blockTypeId:String):List<BlockEntity>

    @Query("SELECT * FROM blocks WHERE projectId=:projectId ORDER BY blockTypeId, sequence")
    suspend fun getBlocksForProject(projectId:String):List<BlockEntity>

    @Query("SELECT * FROM block_types WHERE projectId=:projectId ORDER BY code")
    suspend fun getBlockTypesForProject(projectId:String):List<BlockTypeEntity>

    @Query("SELECT * FROM block_types WHERE projectId=:projectId AND code=:code LIMIT 1")
    suspend fun getBlockTypeByCode(projectId:String,code:String):BlockTypeEntity?

    @Query("SELECT * FROM block_types WHERE id=:id")
    suspend fun getBlockType(id:String):BlockTypeEntity?

    @Query("""
        SELECT bwi.id, bwi.blockId, bwi.workItemDefinitionId,
               wid.name, wid.code, wid.tooltip, wid.kind,
               bwi.progressStatus, bwi.qualityStatus, bwi.controlStatus,
               bwi.isBlocked,
               (SELECT COUNT(*) FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id AND pr.status='OPEN' AND pd.kind='PROBLEM') AS openProblemCount,
               (SELECT COUNT(*) FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id AND pr.status='OPEN' AND pd.kind='ADVANTAGE') AS openAdvantageCount,
               bwi.updatedAt
        FROM block_work_items bwi
        JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
        WHERE bwi.blockId=:blockId
        ORDER BY CASE wid.kind WHEN 'ELECTRICAL' THEN 0 ELSE 1 END, wid.name
    """)
    fun observeBlockWorkItems(blockId:String):Flow<List<BlockWorkItemRow>>

    @Query("SELECT * FROM block_work_items WHERE id=:id")
    fun observeBlockWorkItem(id:String):Flow<BlockWorkItemEntity?>

    @Query("SELECT * FROM block_work_items WHERE id=:id")
    suspend fun getBlockWorkItem(id:String):BlockWorkItemEntity?

    @Query("SELECT workItemDefinitionId FROM block_type_work_items WHERE blockTypeId=:blockTypeId ORDER BY sortOrder")
    suspend fun getTemplateWorkItemIds(blockTypeId:String):List<String>

    @Query("SELECT * FROM work_item_definitions WHERE id=:id")
    suspend fun getWorkItemDefinition(id:String):WorkItemDefinitionEntity?

    @Query("""
        SELECT * FROM work_item_definitions
        WHERE projectId=:projectId AND active=1
        ORDER BY CASE kind WHEN 'ELECTRICAL' THEN 0 ELSE 1 END, name
    """)
    fun observeWorkItemDefinitions(projectId:String):Flow<List<WorkItemDefinitionEntity>>

    @Query("""
        SELECT * FROM problem_definitions
        WHERE projectId=:projectId AND active=1
        ORDER BY CASE kind WHEN 'PROBLEM' THEN 0 ELSE 1 END, code
    """)
    fun observeProblemDefinitions(projectId:String):Flow<List<ProblemDefinitionEntity>>

    @Query("SELECT * FROM problem_definitions WHERE projectId=:projectId AND code=:code LIMIT 1")
    suspend fun getProblemDefinitionByCode(projectId:String,code:String):ProblemDefinitionEntity?

    @Query("SELECT * FROM problem_definitions WHERE id=:id")
    suspend fun getProblemDefinition(id:String):ProblemDefinitionEntity?

    @Query("""
        SELECT pr.id, pr.blockWorkItemId, pr.problemDefinitionId,
               pd.code, pd.title, pd.tooltip, pd.kind,
               pr.status, pr.note, pr.includeInReport, pr.createdAt, pr.closedAt
        FROM problem_records pr
        JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
        WHERE pr.blockWorkItemId=:blockWorkItemId
        ORDER BY CASE pd.kind WHEN 'PROBLEM' THEN 0 ELSE 1 END, pr.createdAt DESC
    """)
    fun observeProblemRecords(blockWorkItemId:String):Flow<List<ProblemRecordRow>>

    @Query("SELECT * FROM problem_records WHERE id=:id")
    suspend fun getProblemRecord(id:String):ProblemRecordEntity?

    @Query("SELECT * FROM notes WHERE blockWorkItemId=:blockWorkItemId ORDER BY createdAt DESC")
    fun observeNotes(blockWorkItemId:String):Flow<List<NoteEntity>>

    @Query("SELECT * FROM photos WHERE blockWorkItemId=:blockWorkItemId ORDER BY createdAt DESC")
    fun observePhotos(blockWorkItemId:String):Flow<List<PhotoEntity>>

    @Query("""
        SELECT bad.id AS attributeDefinitionId, bad.key, bad.name, bad.tooltip, bav.value
        FROM block_attribute_values bav
        JOIN block_attribute_definitions bad ON bad.id=bav.attributeDefinitionId
        WHERE bav.blockId=:blockId
        ORDER BY bad.name
    """)
    fun observeBlockAttributes(blockId:String):Flow<List<BlockAttributeRow>>

    @Query("SELECT * FROM block_attribute_definitions WHERE projectId=:projectId ORDER BY name")
    fun observeBlockAttributeDefinitions(projectId:String):Flow<List<BlockAttributeDefinitionEntity>>

    @Query("""
        SELECT b.projectId
        FROM block_work_items bwi
        JOIN blocks b ON b.id=bwi.blockId
        WHERE bwi.id=:blockWorkItemId
    """)
    suspend fun getProjectIdForBlockWorkItem(blockWorkItemId:String):String?

    @Query("""
        SELECT p.name AS projectName, b.code AS blockCode, wid.name AS workItemName
        FROM block_work_items bwi
        JOIN blocks b ON b.id=bwi.blockId
        JOIN projects p ON p.id=b.projectId
        JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
        WHERE bwi.id=:blockWorkItemId
    """)
    suspend fun getPhotoContext(blockWorkItemId:String):PhotoContextRow?

    @Query("""
        SELECT bwi.id AS blockWorkItemId, b.code AS blockCode, wid.name AS workItemName,
               wid.kind AS workItemKind,
               bwi.progressStatus, bwi.qualityStatus, bwi.controlStatus, bwi.isBlocked,
               (SELECT COUNT(*) FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id AND pr.status='OPEN' AND pd.kind='PROBLEM') AS openProblemCount,
               (SELECT COUNT(*) FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id AND pr.status='OPEN' AND pd.kind='ADVANTAGE') AS openAdvantageCount
        FROM block_work_items bwi
        JOIN blocks b ON b.id=bwi.blockId
        JOIN block_types bt ON bt.id=b.blockTypeId
        JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
        WHERE b.projectId=:projectId
        ORDER BY bt.code, b.sequence, CASE wid.kind WHEN 'ELECTRICAL' THEN 0 ELSE 1 END, wid.name
    """)
    suspend fun getReportWorkItems(projectId:String):List<ReportWorkItemRow>

    @Query("""
        SELECT bwi.id AS blockWorkItemId, b.code AS blockCode, wid.name AS workItemName,
               wid.kind AS workItemKind,
               bwi.progressStatus, bwi.qualityStatus, bwi.controlStatus, bwi.isBlocked,
               (SELECT COUNT(*) FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id AND pr.status='OPEN' AND pd.kind='PROBLEM') AS openProblemCount,
               (SELECT COUNT(*) FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id AND pr.status='OPEN' AND pd.kind='ADVANTAGE') AS openAdvantageCount
        FROM block_work_items bwi
        JOIN blocks b ON b.id=bwi.blockId
        JOIN block_types bt ON bt.id=b.blockTypeId
        JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
        WHERE b.projectId=:projectId
        ORDER BY bt.code, b.sequence, CASE wid.kind WHEN 'ELECTRICAL' THEN 0 ELSE 1 END, wid.name
    """)
    fun observeProjectMatrixRows(projectId:String):Flow<List<ReportWorkItemRow>>

    @Query("""
        SELECT pr.blockWorkItemId, pd.code, pd.title, pd.kind, pr.note, pr.status, pr.createdAt, pr.closedAt
        FROM problem_records pr
        JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
        JOIN block_work_items bwi ON bwi.id=pr.blockWorkItemId
        JOIN blocks b ON b.id=bwi.blockId
        WHERE b.projectId=:projectId AND pr.includeInReport=1
        ORDER BY pr.createdAt
    """)
    suspend fun getReportProblems(projectId:String):List<ReportProblemRow>

    @Query("""
        SELECT n.blockWorkItemId, n.text, n.createdAt
        FROM notes n
        JOIN block_work_items bwi ON bwi.id=n.blockWorkItemId
        JOIN blocks b ON b.id=bwi.blockId
        WHERE b.projectId=:projectId AND n.includeInReport=1
        ORDER BY n.createdAt
    """)
    suspend fun getReportNotes(projectId:String):List<ReportNoteRow>

    @Query("""
        SELECT ph.blockWorkItemId, ph.localUri, ph.caption, ph.createdAt
        FROM photos ph
        JOIN block_work_items bwi ON bwi.id=ph.blockWorkItemId
        JOIN blocks b ON b.id=bwi.blockId
        WHERE b.projectId=:projectId AND ph.includeInReport=1
        ORDER BY ph.createdAt
    """)
    suspend fun getReportPhotos(projectId:String):List<ReportPhotoRow>

    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertProjects(items:List<ProjectEntity>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertBlockTypes(items:List<BlockTypeEntity>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertBlocks(items:List<BlockEntity>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertWorkItemDefinitions(items:List<WorkItemDefinitionEntity>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertBlockTypeWorkItems(items:List<BlockTypeWorkItemEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertBlockWorkItems(items:List<BlockWorkItemEntity>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertProblemDefinitions(items:List<ProblemDefinitionEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertProblemRecords(items:List<ProblemRecordEntity>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertBlockAttributeDefinitions(items:List<BlockAttributeDefinitionEntity>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertBlockAttributeValues(items:List<BlockAttributeValueEntity>)

    @Insert suspend fun insertProblemDefinition(item:ProblemDefinitionEntity)
    @Insert suspend fun insertProblemRecord(item:ProblemRecordEntity)
    @Insert suspend fun insertNote(item:NoteEntity)
    @Insert suspend fun insertPhoto(item:PhotoEntity)
    @Insert suspend fun insertAuditEvent(item:AuditEventEntity)

    @Update suspend fun updateBlockWorkItem(item:BlockWorkItemEntity)
    @Update suspend fun updateProblemRecord(item:ProblemRecordEntity)

    @Query("UPDATE problem_records SET includeInReport=:include WHERE id=:id")
    suspend fun setProblemReportInclusion(id:String,include:Boolean)

    @Query("UPDATE notes SET includeInReport=:include WHERE id=:id")
    suspend fun setNoteReportInclusion(id:String,include:Boolean)

    @Query("UPDATE photos SET includeInReport=:include WHERE id=:id")
    suspend fun setPhotoReportInclusion(id:String,include:Boolean)

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun projectCount():Int
}
