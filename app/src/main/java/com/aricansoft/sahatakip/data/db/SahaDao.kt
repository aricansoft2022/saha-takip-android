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
    val openDeficiencyCount:Int,
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
    val specificDescription:String?,
    val floor:String?,
    val unitNumber:String?,
    val unitName:String?,
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
    val openAdvantageCount:Int,
    val openDeficiencyCount:Int
)

data class ProjectWorkItemQuickStatusRow(
    val projectId:String,
    val workItemName:String,
    val workItemKind:com.aricansoft.sahatakip.data.model.WorkItemKind,
    val totalWorkItemCount:Int,
    val inProgressCount:Int,
    val finishedCount:Int,
    val defectiveCount:Int,
    val blockedCount:Int,
    val openProblemCount:Int,
    val openAdvantageCount:Int,
    val openDeficiencyCount:Int
)

data class ReportWorkItemRow(
    val blockWorkItemId:String,
    val blockCode:String,
    val workItemDefinitionId:String,
    val workItemName:String,
    val workItemKind:com.aricansoft.sahatakip.data.model.WorkItemKind,
    val progressStatus:com.aricansoft.sahatakip.data.model.ProgressStatus,
    val qualityStatus:com.aricansoft.sahatakip.data.model.QualityStatus,
    val controlStatus:com.aricansoft.sahatakip.data.model.ControlStatus,
    val isBlocked:Boolean,
    val openProblemCount:Int,
    val openAdvantageCount:Int,
    val openDeficiencyCount:Int
)

data class ReportProblemRow(
    val problemRecordId:String,
    val blockWorkItemId:String,
    val code:String,
    val title:String,
    val kind:com.aricansoft.sahatakip.data.model.FindingKind,
    val note:String?,
    val specificDescription:String?,
    val floor:String?,
    val unitNumber:String?,
    val unitName:String?,
    val status:com.aricansoft.sahatakip.data.model.ProblemRecordStatus,
    val createdAt:Long,
    val closedAt:Long?
)

data class ProjectFindingRow(
    val problemRecordId:String,
    val blockWorkItemId:String,
    val blockCode:String,
    val workItemDefinitionId:String,
    val workItemName:String,
    val problemDefinitionId:String,
    val code:String,
    val title:String,
    val kind:com.aricansoft.sahatakip.data.model.FindingKind,
    val status:com.aricansoft.sahatakip.data.model.ProblemRecordStatus
)

data class ProjectDeficiencyRow(
    val deficiencyId:String,
    val blockWorkItemId:String,
    val blockCode:String,
    val workItemDefinitionId:String,
    val workItemName:String,
    val title:String,
    val description:String?,
    val floor:String?,
    val unitNumber:String?,
    val unitName:String?,
    val targetDate:String?,
    val priority:com.aricansoft.sahatakip.data.model.DeficiencyPriority,
    val status:com.aricansoft.sahatakip.data.model.DeficiencyStatus,
    val includeInReport:Boolean,
    val createdAt:Long,
    val updatedAt:Long
)

data class ReportDeficiencyRow(
    val deficiencyId:String,
    val blockWorkItemId:String,
    val blockCode:String,
    val workItemName:String,
    val title:String,
    val description:String?,
    val floor:String?,
    val unitNumber:String?,
    val unitName:String?,
    val targetDate:String?,
    val priority:com.aricansoft.sahatakip.data.model.DeficiencyPriority,
    val status:com.aricansoft.sahatakip.data.model.DeficiencyStatus,
    val createdAt:Long,
    val updatedAt:Long
)

data class ReportNoteRow(
    val blockWorkItemId:String,
    val text:String,
    val createdAt:Long
)

data class ReportPhotoRow(
    val blockWorkItemId:String,
    val problemRecordId:String?,
    val deficiencyId:String?,
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
            ) AS openAdvantageCount,
            (
                SELECT COUNT(*)
                FROM deficiencies d
                JOIN block_work_items bwi ON bwi.id=d.blockWorkItemId
                JOIN blocks b ON b.id=bwi.blockId
                WHERE b.projectId=p.id
                  AND d.status!='VERIFIED'
            ) AS openDeficiencyCount
        FROM projects p
        ORDER BY p.name
    """)
    fun observeProjectQuickStatuses():Flow<List<ProjectQuickStatusRow>>

    @Query("""
        SELECT
            b.projectId AS projectId,
            wid.name AS workItemName,
            wid.kind AS workItemKind,
            COUNT(*) AS totalWorkItemCount,
            SUM(CASE WHEN bwi.progressStatus='IN_PROGRESS' THEN 1 ELSE 0 END) AS inProgressCount,
            SUM(CASE WHEN bwi.progressStatus='FINISHED' THEN 1 ELSE 0 END) AS finishedCount,
            SUM(CASE WHEN bwi.qualityStatus IN ('DEFECTIVE','CRITICAL_DEFECT') THEN 1 ELSE 0 END) AS defectiveCount,
            SUM(CASE WHEN bwi.isBlocked=1 THEN 1 ELSE 0 END) AS blockedCount,
            SUM((
                SELECT COUNT(*)
                FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id
                  AND pr.status='OPEN'
                  AND pd.kind='PROBLEM'
            )) AS openProblemCount,
            SUM((
                SELECT COUNT(*)
                FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id
                  AND pr.status='OPEN'
                  AND pd.kind='ADVANTAGE'
            )) AS openAdvantageCount,
            SUM((
                SELECT COUNT(*)
                FROM deficiencies d
                WHERE d.blockWorkItemId=bwi.id
                  AND d.status!='VERIFIED'
            )) AS openDeficiencyCount
        FROM block_work_items bwi
        JOIN blocks b ON b.id=bwi.blockId
        JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
        GROUP BY b.projectId, wid.name, wid.kind
        ORDER BY CASE wid.kind WHEN 'ELECTRICAL' THEN 0 ELSE 1 END, wid.name
    """)
    fun observeProjectWorkItemQuickStatuses():Flow<List<ProjectWorkItemQuickStatusRow>>

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
               (SELECT COUNT(*) FROM deficiencies d
                WHERE d.blockWorkItemId=bwi.id AND d.status!='VERIFIED') AS openDeficiencyCount,
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

    @Query("SELECT * FROM work_item_definitions WHERE projectId=:projectId")
    suspend fun getWorkItemDefinitionsForProject(projectId:String):List<WorkItemDefinitionEntity>

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
               pr.status, pr.note, pr.specificDescription, pr.floor, pr.unitNumber, pr.unitName,
               pr.includeInReport, pr.createdAt, pr.closedAt
        FROM problem_records pr
        JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
        WHERE pr.blockWorkItemId=:blockWorkItemId
        ORDER BY CASE pd.kind WHEN 'PROBLEM' THEN 0 ELSE 1 END, pr.createdAt DESC
    """)
    fun observeProblemRecords(blockWorkItemId:String):Flow<List<ProblemRecordRow>>

    @Query("""
        SELECT
            pr.id AS problemRecordId,
            pr.blockWorkItemId,
            b.code AS blockCode,
            bwi.workItemDefinitionId AS workItemDefinitionId,
            wid.name AS workItemName,
            pr.problemDefinitionId,
            pd.code,
            pd.title,
            pd.kind,
            pr.status
        FROM problem_records pr
        JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
        JOIN block_work_items bwi ON bwi.id=pr.blockWorkItemId
        JOIN blocks b ON b.id=bwi.blockId
        JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
        WHERE b.projectId=:projectId
        ORDER BY CASE pd.kind WHEN 'PROBLEM' THEN 0 ELSE 1 END, pd.code, b.code
    """)
    fun observeProjectFindings(projectId:String):Flow<List<ProjectFindingRow>>

    @Query("""
        SELECT * FROM deficiency_definitions
        WHERE projectId=:projectId AND active=1
        ORDER BY title
    """)
    fun observeDeficiencyDefinitions(projectId:String):Flow<List<DeficiencyDefinitionEntity>>

    @Query("SELECT * FROM deficiency_definitions WHERE projectId=:projectId")
    suspend fun getDeficiencyDefinitionsForProject(projectId:String):List<DeficiencyDefinitionEntity>

    @Query("""
        SELECT * FROM deficiencies
        WHERE blockWorkItemId=:blockWorkItemId
        ORDER BY
            CASE status
                WHEN 'OPEN' THEN 0
                WHEN 'IN_PROGRESS' THEN 1
                WHEN 'FIXED' THEN 2
                ELSE 3
            END,
            CASE priority
                WHEN 'CRITICAL' THEN 0
                WHEN 'HIGH' THEN 1
                ELSE 2
            END,
            updatedAt DESC
    """)
    fun observeDeficiencies(blockWorkItemId:String):Flow<List<DeficiencyEntity>>

    @Query("SELECT * FROM deficiencies WHERE id=:id")
    suspend fun getDeficiency(id:String):DeficiencyEntity?

    @Query("""
        SELECT
            d.id AS deficiencyId,
            d.blockWorkItemId,
            b.code AS blockCode,
            bwi.workItemDefinitionId AS workItemDefinitionId,
            wid.name AS workItemName,
            d.title,
            d.description,
            d.floor,
            d.unitNumber,
            d.unitName,
            d.targetDate,
            d.priority,
            d.status,
            d.includeInReport,
            d.createdAt,
            d.updatedAt
        FROM deficiencies d
        JOIN block_work_items bwi ON bwi.id=d.blockWorkItemId
        JOIN blocks b ON b.id=bwi.blockId
        JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
        WHERE b.projectId=:projectId
        ORDER BY
            CASE d.status
                WHEN 'OPEN' THEN 0
                WHEN 'IN_PROGRESS' THEN 1
                WHEN 'FIXED' THEN 2
                ELSE 3
            END,
            CASE d.priority
                WHEN 'CRITICAL' THEN 0
                WHEN 'HIGH' THEN 1
                ELSE 2
            END,
            b.code,
            wid.name,
            d.updatedAt DESC
    """)
    fun observeProjectDeficiencies(projectId:String):Flow<List<ProjectDeficiencyRow>>

    @Query("SELECT * FROM problem_records WHERE id=:id")
    suspend fun getProblemRecord(id:String):ProblemRecordEntity?

    @Query("SELECT * FROM notes WHERE blockWorkItemId=:blockWorkItemId ORDER BY createdAt DESC")
    fun observeNotes(blockWorkItemId:String):Flow<List<NoteEntity>>

    @Query("SELECT * FROM photos WHERE blockWorkItemId=:blockWorkItemId AND problemRecordId IS NULL AND deficiencyId IS NULL ORDER BY createdAt DESC")
    fun observePhotos(blockWorkItemId:String):Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE problemRecordId=:problemRecordId ORDER BY createdAt DESC")
    fun observeFindingPhotos(problemRecordId:String):Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE deficiencyId=:deficiencyId ORDER BY createdAt DESC")
    fun observeDeficiencyPhotos(deficiencyId:String):Flow<List<PhotoEntity>>

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
        SELECT bwi.id AS blockWorkItemId, b.code AS blockCode,
               wid.id AS workItemDefinitionId, wid.name AS workItemName,
               wid.kind AS workItemKind,
               bwi.progressStatus, bwi.qualityStatus, bwi.controlStatus, bwi.isBlocked,
               (SELECT COUNT(*) FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id AND pr.status='OPEN' AND pd.kind='PROBLEM') AS openProblemCount,
               (SELECT COUNT(*) FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id AND pr.status='OPEN' AND pd.kind='ADVANTAGE') AS openAdvantageCount,
               (SELECT COUNT(*) FROM deficiencies d
                WHERE d.blockWorkItemId=bwi.id AND d.status!='VERIFIED') AS openDeficiencyCount
        FROM block_work_items bwi
        JOIN blocks b ON b.id=bwi.blockId
        JOIN block_types bt ON bt.id=b.blockTypeId
        JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
        WHERE b.projectId=:projectId
        ORDER BY bt.code, b.sequence, CASE wid.kind WHEN 'ELECTRICAL' THEN 0 ELSE 1 END, wid.name
    """)
    suspend fun getReportWorkItems(projectId:String):List<ReportWorkItemRow>

    @Query("""
        SELECT bwi.id AS blockWorkItemId, b.code AS blockCode,
               wid.id AS workItemDefinitionId, wid.name AS workItemName,
               wid.kind AS workItemKind,
               bwi.progressStatus, bwi.qualityStatus, bwi.controlStatus, bwi.isBlocked,
               (SELECT COUNT(*) FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id AND pr.status='OPEN' AND pd.kind='PROBLEM') AS openProblemCount,
               (SELECT COUNT(*) FROM problem_records pr
                JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
                WHERE pr.blockWorkItemId=bwi.id AND pr.status='OPEN' AND pd.kind='ADVANTAGE') AS openAdvantageCount,
               (SELECT COUNT(*) FROM deficiencies d
                WHERE d.blockWorkItemId=bwi.id AND d.status!='VERIFIED') AS openDeficiencyCount
        FROM block_work_items bwi
        JOIN blocks b ON b.id=bwi.blockId
        JOIN block_types bt ON bt.id=b.blockTypeId
        JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
        WHERE b.projectId=:projectId
        ORDER BY bt.code, b.sequence, CASE wid.kind WHEN 'ELECTRICAL' THEN 0 ELSE 1 END, wid.name
    """)
    fun observeProjectMatrixRows(projectId:String):Flow<List<ReportWorkItemRow>>

    @Query("""
        SELECT pr.id AS problemRecordId, pr.blockWorkItemId, pd.code, pd.title, pd.kind,
               pr.note, pr.specificDescription, pr.floor, pr.unitNumber, pr.unitName,
               pr.status, pr.createdAt, pr.closedAt
        FROM problem_records pr
        JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
        JOIN block_work_items bwi ON bwi.id=pr.blockWorkItemId
        JOIN blocks b ON b.id=bwi.blockId
        WHERE b.projectId=:projectId AND pr.includeInReport=1
        ORDER BY pr.createdAt
    """)
    suspend fun getReportProblems(projectId:String):List<ReportProblemRow>

    @Query("""
        SELECT
            d.id AS deficiencyId,
            d.blockWorkItemId,
            b.code AS blockCode,
            wid.name AS workItemName,
            d.title,
            d.description,
            d.floor,
            d.unitNumber,
            d.unitName,
            d.targetDate,
            d.priority,
            d.status,
            d.createdAt,
            d.updatedAt
        FROM deficiencies d
        JOIN block_work_items bwi ON bwi.id=d.blockWorkItemId
        JOIN blocks b ON b.id=bwi.blockId
        JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
        WHERE b.projectId=:projectId AND d.includeInReport=1
        ORDER BY b.code, wid.name, d.createdAt
    """)
    suspend fun getReportDeficiencies(projectId:String):List<ReportDeficiencyRow>

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
        SELECT ph.blockWorkItemId, ph.problemRecordId, ph.deficiencyId, ph.localUri, ph.caption, ph.createdAt
        FROM photos ph
        JOIN block_work_items bwi ON bwi.id=ph.blockWorkItemId
        JOIN blocks b ON b.id=bwi.blockId
        WHERE b.projectId=:projectId
          AND ph.includeInReport=1
          AND (
              ph.problemRecordId IS NULL OR
              EXISTS(
                  SELECT 1
                  FROM problem_records pr
                  WHERE pr.id=ph.problemRecordId AND pr.includeInReport=1
              )
          )
          AND (
              ph.deficiencyId IS NULL OR
              EXISTS(
                  SELECT 1
                  FROM deficiencies d
                  WHERE d.id=ph.deficiencyId AND d.includeInReport=1
              )
          )
        ORDER BY ph.createdAt
    """)
    suspend fun getReportPhotos(projectId:String):List<ReportPhotoRow>

    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertProjects(items:List<ProjectEntity>)
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertBlockTypes(items:List<BlockTypeEntity>)
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertBlocks(items:List<BlockEntity>)
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertWorkItemDefinitions(items:List<WorkItemDefinitionEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertBlockTypeWorkItems(items:List<BlockTypeWorkItemEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertBlockWorkItems(items:List<BlockWorkItemEntity>)
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertProblemDefinitions(items:List<ProblemDefinitionEntity>)
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertDeficiencyDefinitions(items:List<DeficiencyDefinitionEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertProblemRecords(items:List<ProblemRecordEntity>)
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertBlockAttributeDefinitions(items:List<BlockAttributeDefinitionEntity>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertBlockAttributeValues(items:List<BlockAttributeValueEntity>)

    @Insert suspend fun insertProblemDefinition(item:ProblemDefinitionEntity)
    @Insert suspend fun insertDeficiencyDefinition(item:DeficiencyDefinitionEntity)
    @Insert suspend fun insertProblemRecord(item:ProblemRecordEntity)
    @Insert suspend fun insertDeficiency(item:DeficiencyEntity)
    @Insert suspend fun insertNote(item:NoteEntity)
    @Insert suspend fun insertPhoto(item:PhotoEntity)
    @Insert suspend fun insertAuditEvent(item:AuditEventEntity)

    @Update suspend fun updateBlockWorkItem(item:BlockWorkItemEntity)
    @Update suspend fun updateProblemRecord(item:ProblemRecordEntity)
    @Update suspend fun updateDeficiency(item:DeficiencyEntity)

    @Query("UPDATE problem_records SET includeInReport=:include WHERE id=:id")
    suspend fun setProblemReportInclusion(id:String,include:Boolean)

    @Query("UPDATE deficiencies SET includeInReport=:include WHERE id=:id")
    suspend fun setDeficiencyReportInclusion(id:String,include:Boolean)

    @Query("UPDATE notes SET includeInReport=:include WHERE id=:id")
    suspend fun setNoteReportInclusion(id:String,include:Boolean)

    @Query("UPDATE photos SET includeInReport=:include WHERE id=:id")
    suspend fun setPhotoReportInclusion(id:String,include:Boolean)

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun projectCount():Int
}
