package com.aricansoft.sahatakip.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities=[
        ProjectEntity::class,
        BlockTypeEntity::class,
        BlockEntity::class,
        WorkItemDefinitionEntity::class,
        BlockTypeWorkItemEntity::class,
        BlockWorkItemEntity::class,
        ProblemDefinitionEntity::class,
        ProblemRecordEntity::class,
        NoteEntity::class,
        PhotoEntity::class,
        BlockAttributeDefinitionEntity::class,
        BlockAttributeValueEntity::class,
        AuditEventEntity::class
    ],
    version=1,
    exportSchema=true
)
@TypeConverters(Converters::class)
abstract class SahaDatabase:RoomDatabase(){
    abstract fun sahaDao():SahaDao
}
