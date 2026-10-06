package com.aricansoft.sahatakip.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version=3,
    exportSchema=true
)
@TypeConverters(Converters::class)
abstract class SahaDatabase:RoomDatabase(){
    abstract fun sahaDao():SahaDao

    companion object {
        val MIGRATION_1_2=object:Migration(1,2){
            override fun migrate(db:SupportSQLiteDatabase){
                db.execSQL("ALTER TABLE work_item_definitions ADD COLUMN kind TEXT NOT NULL DEFAULT 'ELECTRICAL'")
                db.execSQL("ALTER TABLE problem_definitions ADD COLUMN kind TEXT NOT NULL DEFAULT 'PROBLEM'")

                db.execSQL("""
                    UPDATE work_item_definitions
                    SET kind='RELATED_DISCIPLINE'
                    WHERE projectId='project-konya-444'
                      AND name='Mutfak Fayans / Dolap'
                """.trimIndent())
                db.execSQL("""
                    UPDATE problem_definitions
                    SET kind='ADVANTAGE'
                    WHERE projectId='project-konya-444'
                      AND code='L.İ.E.'
                """.trimIndent())

                db.execSQL("""
                    INSERT OR IGNORE INTO problem_definitions
                    (id,projectId,code,title,description,tooltip,active,kind)
                    VALUES(
                        'pd-related-complete',
                        'project-konya-444',
                        'TAM.İNŞ.',
                        'Aleyhimize tamamlanmış inşaat işi',
                        NULL,
                        'Elektrik işini etkileyen başka disiplin imalatı biz müdahale etmeden tamamlanmış.',
                        1,
                        'PROBLEM'
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO problem_records
                    (id,blockWorkItemId,problemDefinitionId,status,note,includeInReport,createdAt,closedAt)
                    SELECT
                        'pr-related-complete-' || bwi.id,
                        bwi.id,
                        'pd-related-complete',
                        'OPEN',
                        NULL,
                        1,
                        bwi.updatedAt,
                        NULL
                    FROM block_work_items bwi
                    JOIN work_item_definitions wid ON wid.id=bwi.workItemDefinitionId
                    WHERE wid.projectId='project-konya-444'
                      AND wid.name='Mutfak Fayans / Dolap'
                      AND bwi.progressStatus='FINISHED'
                """.trimIndent())
            }
        }

        val MIGRATION_2_3=object:Migration(2,3){
            override fun migrate(db:SupportSQLiteDatabase){
                db.execSQL("ALTER TABLE photos ADD COLUMN problemRecordId TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_problemRecordId ON photos(problemRecordId)")
            }
        }
    }
}
