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
    val progressStatus:com.aricansoft.sahatakip.data.model.ProgressStatus,
    val qualityStatus:com.aricansoft.sahatakip.data.model.QualityStatus,
    val controlStatus:com.aricansoft.sahatakip.data.model.ControlStatus,
    val isBlocked:Boolean,
    val updatedAt:Long
)

data class ProblemRecordRow(
    val id:String,
    val blockWorkItemId:String,
    val problemDefinitionId:String,
    val code:String,
    val title:String,
    val tooltip:String?,
    val status:com.aricansoft.sahatakip.data.model.ProblemRecordStatus,
    val note:String?,
    val includeInReport:Boolean,
    val createdAt:Long,
    val closedAt:Long?
)

data class BlockAttributeRow(val key:String,val name:String,val tooltip:String?,val value:String)
data class PhotoContextRow(val projectName:String,val blockCode:String,val workItemName:String)

@Dao
interface SahaDao {
    @Query("SELECT * FROM projects ORDER BY name")
    fun observeProjects():Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id=:id")
    suspend fun getProject(id:String):ProjectEntity?

    @Query("SELECT * FROM blocks WHERE projectId=:projectId ORDER BY blockTypeId, sequence")
    fun observeBlocks(projectId:String):Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocks WHERE id=:id")
    suspend fun getBlock(id:String):BlockEntity?

    @Query("SELECT * FROM blocks WHERE blockTypeId=:blockTypeId ORDER BY sequence")
    suspend fun getBlocksForType(blockTypeId:String):List<BlockEntity>

    @Query("SELECT * FROM blocks WHERE projectId=:projectId ORDER BY blockTypeId, sequence")
    suspend fun getBlocksForProject(projectId:String):List<BlockEntity>

    @Query("SELECT * FROM block_types WHERE projectId=:projectId ORDER BY code")
    suspend fun getBlockTypesForProject(projectId:String):List<BlockTypeEntity>

    @Query("SELECT * FROM block_types WHERE id=:id")
    suspend fun getBlockType(id:String):BlockTypeEntity?

    @Query("""
        SELECT bwi.id, bwi.blockId, bwi.workItemDefinitionId,
               wid.name, wid.code, wid.tooltip,
               bwi.progressStatus, bwi.qualityStatus, bwi.controlStatus,
               bwi.isBlocked, bwi.updatedAt
        FROM block_work_items bwi
        JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
        WHERE bwi.blockId=:blockId
        ORDER BY wid.name
    """)
    fun observeBlockWorkItems(blockId:String):Flow<List<BlockWorkItemRow>>

    @Query("SELECT * FROM block_work_items WHERE id=:id")
    fun observeBlockWorkItem(id:String):Flow<BlockWorkItemEntity?>

    @Query("SELECT * FROM block_work_items WHERE id=:id")
    suspend fun getBlockWorkItem(id:String):BlockWorkItemEntity?

    @Query("SELECT * FROM work_item_definitions WHERE id=:id")
    suspend fun getWorkItemDefinition(id:String):WorkItemDefinitionEntity?

    @Query("SELECT * FROM work_item_definitions WHERE projectId=:projectId AND active=1 ORDER BY name")
    fun observeWorkItemDefinitions(projectId:String):Flow<List<WorkItemDefinitionEntity>>

    @Query("SELECT * FROM problem_definitions WHERE projectId=:projectId AND active=1 ORDER BY code")
    fun observeProblemDefinitions(projectId:String):Flow<List<ProblemDefinitionEntity>>

    @Query("SELECT * FROM problem_definitions WHERE projectId=:projectId AND code=:code LIMIT 1")
    suspend fun getProblemDefinitionByCode(projectId:String,code:String):ProblemDefinitionEntity?

    @Query("""
        SELECT pr.id, pr.blockWorkItemId, pr.problemDefinitionId,
               pd.code, pd.title, pd.tooltip,
               pr.status, pr.note, pr.includeInReport, pr.createdAt, pr.closedAt
        FROM problem_records pr
        JOIN problem_definitions pd ON pd.id=pr.problemDefinitionId
        WHERE pr.blockWorkItemId=:blockWorkItemId
        ORDER BY pr.createdAt DESC
    """)
    fun observeProblemRecords(blockWorkItemId:String):Flow<List<ProblemRecordRow>>

    @Query("SELECT * FROM problem_records WHERE id=:id")
    suspend fun getProblemRecord(id:String):ProblemRecordEntity?

    @Query("SELECT * FROM notes WHERE blockWorkItemId=:blockWorkItemId ORDER BY createdAt DESC")
    fun observeNotes(blockWorkItemId:String):Flow<List<NoteEntity>>

    @Query("SELECT * FROM photos WHERE blockWorkItemId=:blockWorkItemId ORDER BY createdAt DESC")
    fun observePhotos(blockWorkItemId:String):Flow<List<PhotoEntity>>

    @Query("""
        SELECT bad.key, bad.name, bad.tooltip, bav.value
        FROM block_attribute_values bav
        JOIN block_attribute_definitions bad ON bad.id=bav.attributeDefinitionId
        WHERE bav.blockId=:blockId
        ORDER BY bad.name
    """)
    fun observeBlockAttributes(blockId:String):Flow<List<BlockAttributeRow>>

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

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun projectCount():Int
}
