package com.aricansoft.sahatakip.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aricansoft.sahatakip.data.model.ControlStatus
import com.aricansoft.sahatakip.data.model.DeficiencyPriority
import com.aricansoft.sahatakip.data.model.DeficiencyStatus
import com.aricansoft.sahatakip.data.model.FindingKind
import com.aricansoft.sahatakip.data.model.ProblemRecordStatus
import com.aricansoft.sahatakip.data.model.ProgressStatus
import com.aricansoft.sahatakip.data.model.QualityStatus
import com.aricansoft.sahatakip.data.model.WorkItemKind

@Entity(tableName = "projects")
data class ProjectEntity(@PrimaryKey val id: String, val name: String, val createdAt: Long)

@Entity(
    tableName = "block_types",
    indices = [Index("projectId"), Index(value=["projectId","code"], unique=true)],
    foreignKeys = [ForeignKey(
        entity=ProjectEntity::class,
        parentColumns=["id"],
        childColumns=["projectId"],
        onDelete=ForeignKey.RESTRICT
    )]
)
data class BlockTypeEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val code: String,
    val name: String,
    val tooltip: String? = null
)

@Entity(
    tableName = "blocks",
    indices = [Index("projectId"), Index("blockTypeId"), Index(value=["projectId","code"], unique=true)],
    foreignKeys = [
        ForeignKey(entity=ProjectEntity::class,parentColumns=["id"],childColumns=["projectId"],onDelete=ForeignKey.RESTRICT),
        ForeignKey(entity=BlockTypeEntity::class,parentColumns=["id"],childColumns=["blockTypeId"],onDelete=ForeignKey.RESTRICT)
    ]
)
data class BlockEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val blockTypeId: String,
    val code: String,
    val sequence: Int
)

@Entity(
    tableName = "work_item_definitions",
    indices = [Index("projectId"), Index(value=["projectId","name"], unique=true)],
    foreignKeys = [ForeignKey(
        entity=ProjectEntity::class,
        parentColumns=["id"],
        childColumns=["projectId"],
        onDelete=ForeignKey.RESTRICT
    )]
)
data class WorkItemDefinitionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val code: String? = null,
    val name: String,
    val description: String? = null,
    val tooltip: String? = null,
    val active: Boolean = true,
    @ColumnInfo(defaultValue="'ELECTRICAL'")
    val kind: WorkItemKind = WorkItemKind.ELECTRICAL
)

@Entity(
    tableName = "block_type_work_items",
    primaryKeys=["blockTypeId","workItemDefinitionId"],
    indices=[Index("workItemDefinitionId")],
    foreignKeys = [
        ForeignKey(entity=BlockTypeEntity::class,parentColumns=["id"],childColumns=["blockTypeId"],onDelete=ForeignKey.RESTRICT),
        ForeignKey(entity=WorkItemDefinitionEntity::class,parentColumns=["id"],childColumns=["workItemDefinitionId"],onDelete=ForeignKey.RESTRICT)
    ]
)
data class BlockTypeWorkItemEntity(
    val blockTypeId: String,
    val workItemDefinitionId: String,
    val sortOrder: Int
)

@Entity(
    tableName = "block_work_items",
    indices=[
        Index("blockId"),
        Index("workItemDefinitionId"),
        Index(value=["blockId","workItemDefinitionId"], unique=true)
    ],
    foreignKeys = [
        ForeignKey(entity=BlockEntity::class,parentColumns=["id"],childColumns=["blockId"],onDelete=ForeignKey.RESTRICT),
        ForeignKey(entity=WorkItemDefinitionEntity::class,parentColumns=["id"],childColumns=["workItemDefinitionId"],onDelete=ForeignKey.RESTRICT)
    ]
)
data class BlockWorkItemEntity(
    @PrimaryKey val id: String,
    val blockId: String,
    val workItemDefinitionId: String,
    val progressStatus: ProgressStatus = ProgressStatus.NOT_STARTED,
    val qualityStatus: QualityStatus = QualityStatus.NOT_EVALUATED,
    val controlStatus: ControlStatus = ControlStatus.NOT_CHECKED,
    val isBlocked: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "problem_definitions",
    indices=[Index("projectId"), Index(value=["projectId","code"], unique=true)],
    foreignKeys = [ForeignKey(
        entity=ProjectEntity::class,
        parentColumns=["id"],
        childColumns=["projectId"],
        onDelete=ForeignKey.RESTRICT
    )]
)
data class ProblemDefinitionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val code: String,
    val title: String,
    val description: String? = null,
    val tooltip: String? = null,
    val active: Boolean = true,
    @ColumnInfo(defaultValue="'PROBLEM'")
    val kind: FindingKind = FindingKind.PROBLEM
)

@Entity(
    tableName = "problem_records",
    indices=[Index("blockWorkItemId"), Index("problemDefinitionId")],
    foreignKeys = [
        ForeignKey(entity=BlockWorkItemEntity::class,parentColumns=["id"],childColumns=["blockWorkItemId"],onDelete=ForeignKey.RESTRICT),
        ForeignKey(entity=ProblemDefinitionEntity::class,parentColumns=["id"],childColumns=["problemDefinitionId"],onDelete=ForeignKey.RESTRICT)
    ]
)
data class ProblemRecordEntity(
    @PrimaryKey val id: String,
    val blockWorkItemId: String,
    val problemDefinitionId: String,
    val status: ProblemRecordStatus = ProblemRecordStatus.OPEN,
    val note: String? = null,
    val specificDescription: String? = null,
    val floor: String? = null,
    val unitNumber: String? = null,
    val unitName: String? = null,
    val includeInReport: Boolean = true,
    val createdAt: Long,
    val closedAt: Long? = null
)

@Entity(
    tableName = "deficiency_definitions",
    indices=[Index("projectId"), Index(value=["projectId","title"], unique=true)],
    foreignKeys = [ForeignKey(
        entity=ProjectEntity::class,
        parentColumns=["id"],
        childColumns=["projectId"],
        onDelete=ForeignKey.RESTRICT
    )]
)
data class DeficiencyDefinitionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val description: String? = null,
    val active: Boolean = true
)

@Entity(
    tableName = "deficiencies",
    indices=[Index("blockWorkItemId"), Index("status"), Index("targetDate")],
    foreignKeys = [ForeignKey(
        entity=BlockWorkItemEntity::class,
        parentColumns=["id"],
        childColumns=["blockWorkItemId"],
        onDelete=ForeignKey.RESTRICT
    )]
)
data class DeficiencyEntity(
    @PrimaryKey val id: String,
    val blockWorkItemId: String,
    val title: String,
    val description: String? = null,
    val floor: String? = null,
    val unitNumber: String? = null,
    val unitName: String? = null,
    val responsible: String? = null,
    val targetDate: String? = null,
    val priority: DeficiencyPriority = DeficiencyPriority.NORMAL,
    val status: DeficiencyStatus = DeficiencyStatus.OPEN,
    val includeInReport: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "notes",
    indices=[Index("blockWorkItemId")],
    foreignKeys = [ForeignKey(
        entity=BlockWorkItemEntity::class,
        parentColumns=["id"],
        childColumns=["blockWorkItemId"],
        onDelete=ForeignKey.RESTRICT
    )]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val blockWorkItemId: String,
    val text: String,
    val includeInReport: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "photos",
    indices=[Index("blockWorkItemId"), Index("problemRecordId"), Index("deficiencyId")],
    foreignKeys = [
        ForeignKey(entity=BlockWorkItemEntity::class,parentColumns=["id"],childColumns=["blockWorkItemId"],onDelete=ForeignKey.RESTRICT),
        ForeignKey(entity=ProblemRecordEntity::class,parentColumns=["id"],childColumns=["problemRecordId"],onDelete=ForeignKey.RESTRICT),
        ForeignKey(entity=DeficiencyEntity::class,parentColumns=["id"],childColumns=["deficiencyId"],onDelete=ForeignKey.RESTRICT)
    ]
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    val blockWorkItemId: String,
    val problemRecordId: String? = null,
    val deficiencyId: String? = null,
    val localUri: String,
    val caption: String? = null,
    val includeInReport: Boolean = true,
    val createdAt: Long
)

@Entity(
    tableName = "block_attribute_definitions",
    indices=[Index("projectId"), Index(value=["projectId","key"], unique=true)],
    foreignKeys = [ForeignKey(
        entity=ProjectEntity::class,
        parentColumns=["id"],
        childColumns=["projectId"],
        onDelete=ForeignKey.RESTRICT
    )]
)
data class BlockAttributeDefinitionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val key: String,
    val name: String,
    val tooltip: String? = null
)

@Entity(
    tableName = "block_attribute_values",
    primaryKeys=["blockId","attributeDefinitionId"],
    indices=[Index("attributeDefinitionId")],
    foreignKeys = [
        ForeignKey(entity=BlockEntity::class,parentColumns=["id"],childColumns=["blockId"],onDelete=ForeignKey.RESTRICT),
        ForeignKey(entity=BlockAttributeDefinitionEntity::class,parentColumns=["id"],childColumns=["attributeDefinitionId"],onDelete=ForeignKey.RESTRICT)
    ]
)
data class BlockAttributeValueEntity(
    val blockId: String,
    val attributeDefinitionId: String,
    val value: String,
    val updatedAt: Long
)

@Entity(
    tableName = "audit_events",
    indices=[Index("blockWorkItemId"), Index("createdAt")],
    foreignKeys = [ForeignKey(
        entity=BlockWorkItemEntity::class,
        parentColumns=["id"],
        childColumns=["blockWorkItemId"],
        onDelete=ForeignKey.RESTRICT
    )]
)
data class AuditEventEntity(
    @PrimaryKey val id: String,
    val blockWorkItemId: String,
    val eventType: String,
    val detail: String,
    val createdAt: Long
)
