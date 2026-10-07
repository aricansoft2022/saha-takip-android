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
        DeficiencyEntity::class,
        NoteEntity::class,
        PhotoEntity::class,
        BlockAttributeDefinitionEntity::class,
        BlockAttributeValueEntity::class,
        AuditEventEntity::class
    ],
    version=5,
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
                db.execSQL("ALTER TABLE photos ADD COLUMN problemRecordId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_problemRecordId ON photos(problemRecordId)")
            }
        }

        val MIGRATION_3_4=object:Migration(3,4){
            override fun migrate(db:SupportSQLiteDatabase){
                db.execSQL("ALTER TABLE problem_records ADD COLUMN specificDescription TEXT")
                db.execSQL("ALTER TABLE problem_records ADD COLUMN floor TEXT")
                db.execSQL("ALTER TABLE problem_records ADD COLUMN unitNumber TEXT")
                db.execSQL("ALTER TABLE problem_records ADD COLUMN unitName TEXT")
            }
        }

        val MIGRATION_4_5=object:Migration(4,5){
            override fun migrate(db:SupportSQLiteDatabase){
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS deficiencies (
                        id TEXT NOT NULL PRIMARY KEY,
                        blockWorkItemId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        floor TEXT,
                        unitNumber TEXT,
                        unitName TEXT,
                        responsible TEXT,
                        targetDate TEXT,
                        priority TEXT NOT NULL,
                        status TEXT NOT NULL,
                        includeInReport INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_deficiencies_blockWorkItemId ON deficiencies(blockWorkItemId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_deficiencies_status ON deficiencies(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_deficiencies_targetDate ON deficiencies(targetDate)")

                db.execSQL("ALTER TABLE photos ADD COLUMN deficiencyId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_deficiencyId ON photos(deficiencyId)")
            }
        }
    }
}
