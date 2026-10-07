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
        DeficiencyDefinitionEntity::class,
        DeficiencyEntity::class,
        NoteEntity::class,
        PhotoEntity::class,
        BlockAttributeDefinitionEntity::class,
        BlockAttributeValueEntity::class,
        AuditEventEntity::class
    ],
    version=8,
    exportSchema=true
)
@TypeConverters(Converters::class)
abstract class SahaDatabase:RoomDatabase(){
    abstract fun sahaDao():SahaDao

    companion object {
        val CALLBACK=object:Callback(){
            override fun onOpen(db:SupportSQLiteDatabase){
                super.onOpen(db)
                installIntegrityTriggers(db)
            }
        }

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

        val MIGRATION_5_6=object:Migration(5,6){
            override fun migrate(db:SupportSQLiteDatabase){
                fun rebuild(name:String,createSql:String,columns:String){
                    db.execSQL(createSql)
                    db.execSQL("INSERT INTO ${name}_new ($columns) SELECT $columns FROM $name")
                    db.execSQL("DROP TABLE $name")
                    db.execSQL("ALTER TABLE ${name}_new RENAME TO $name")
                }

                rebuild(
                    "block_types",
                    """CREATE TABLE block_types_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        code TEXT NOT NULL,
                        name TEXT NOT NULL,
                        tooltip TEXT,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "id,projectId,code,name,tooltip"
                )
                rebuild(
                    "work_item_definitions",
                    """CREATE TABLE work_item_definitions_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        code TEXT,
                        name TEXT NOT NULL,
                        description TEXT,
                        tooltip TEXT,
                        active INTEGER NOT NULL,
                        kind TEXT NOT NULL DEFAULT 'ELECTRICAL',
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "id,projectId,code,name,description,tooltip,active,kind"
                )
                rebuild(
                    "problem_definitions",
                    """CREATE TABLE problem_definitions_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        code TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        tooltip TEXT,
                        active INTEGER NOT NULL,
                        kind TEXT NOT NULL DEFAULT 'PROBLEM',
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "id,projectId,code,title,description,tooltip,active,kind"
                )
                rebuild(
                    "block_attribute_definitions",
                    """CREATE TABLE block_attribute_definitions_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        key TEXT NOT NULL,
                        name TEXT NOT NULL,
                        tooltip TEXT,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "id,projectId,key,name,tooltip"
                )
                rebuild(
                    "blocks",
                    """CREATE TABLE blocks_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        blockTypeId TEXT NOT NULL,
                        code TEXT NOT NULL,
                        sequence INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(blockTypeId) REFERENCES block_types(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "id,projectId,blockTypeId,code,sequence"
                )
                rebuild(
                    "block_type_work_items",
                    """CREATE TABLE block_type_work_items_new (
                        blockTypeId TEXT NOT NULL,
                        workItemDefinitionId TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        PRIMARY KEY(blockTypeId,workItemDefinitionId),
                        FOREIGN KEY(blockTypeId) REFERENCES block_types(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(workItemDefinitionId) REFERENCES work_item_definitions(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "blockTypeId,workItemDefinitionId,sortOrder"
                )
                rebuild(
                    "block_work_items",
                    """CREATE TABLE block_work_items_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        blockId TEXT NOT NULL,
                        workItemDefinitionId TEXT NOT NULL,
                        progressStatus TEXT NOT NULL,
                        qualityStatus TEXT NOT NULL,
                        controlStatus TEXT NOT NULL,
                        isBlocked INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(blockId) REFERENCES blocks(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(workItemDefinitionId) REFERENCES work_item_definitions(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "id,blockId,workItemDefinitionId,progressStatus,qualityStatus,controlStatus,isBlocked,createdAt,updatedAt"
                )
                rebuild(
                    "problem_records",
                    """CREATE TABLE problem_records_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        blockWorkItemId TEXT NOT NULL,
                        problemDefinitionId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        note TEXT,
                        specificDescription TEXT,
                        floor TEXT,
                        unitNumber TEXT,
                        unitName TEXT,
                        includeInReport INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        closedAt INTEGER,
                        FOREIGN KEY(blockWorkItemId) REFERENCES block_work_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(problemDefinitionId) REFERENCES problem_definitions(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "id,blockWorkItemId,problemDefinitionId,status,note,specificDescription,floor,unitNumber,unitName,includeInReport,createdAt,closedAt"
                )
                rebuild(
                    "deficiencies",
                    """CREATE TABLE deficiencies_new (
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
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(blockWorkItemId) REFERENCES block_work_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "id,blockWorkItemId,title,description,floor,unitNumber,unitName,responsible,targetDate,priority,status,includeInReport,createdAt,updatedAt"
                )
                rebuild(
                    "notes",
                    """CREATE TABLE notes_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        blockWorkItemId TEXT NOT NULL,
                        text TEXT NOT NULL,
                        includeInReport INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(blockWorkItemId) REFERENCES block_work_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "id,blockWorkItemId,text,includeInReport,createdAt"
                )
                rebuild(
                    "block_attribute_values",
                    """CREATE TABLE block_attribute_values_new (
                        blockId TEXT NOT NULL,
                        attributeDefinitionId TEXT NOT NULL,
                        value TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(blockId,attributeDefinitionId),
                        FOREIGN KEY(blockId) REFERENCES blocks(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(attributeDefinitionId) REFERENCES block_attribute_definitions(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "blockId,attributeDefinitionId,value,updatedAt"
                )
                rebuild(
                    "audit_events",
                    """CREATE TABLE audit_events_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        blockWorkItemId TEXT NOT NULL,
                        eventType TEXT NOT NULL,
                        detail TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(blockWorkItemId) REFERENCES block_work_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "id,blockWorkItemId,eventType,detail,createdAt"
                )
                rebuild(
                    "photos",
                    """CREATE TABLE photos_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        blockWorkItemId TEXT NOT NULL,
                        problemRecordId TEXT,
                        deficiencyId TEXT,
                        localUri TEXT NOT NULL,
                        caption TEXT,
                        includeInReport INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(blockWorkItemId) REFERENCES block_work_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(problemRecordId) REFERENCES problem_records(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(deficiencyId) REFERENCES deficiencies(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""",
                    "id,blockWorkItemId,problemRecordId,deficiencyId,localUri,caption,includeInReport,createdAt"
                )

                val indexStatements=listOf(
                    "CREATE INDEX index_block_types_projectId ON block_types(projectId)",
                    "CREATE UNIQUE INDEX index_block_types_projectId_code ON block_types(projectId,code)",
                    "CREATE INDEX index_blocks_projectId ON blocks(projectId)",
                    "CREATE INDEX index_blocks_blockTypeId ON blocks(blockTypeId)",
                    "CREATE UNIQUE INDEX index_blocks_projectId_code ON blocks(projectId,code)",
                    "CREATE INDEX index_work_item_definitions_projectId ON work_item_definitions(projectId)",
                    "CREATE UNIQUE INDEX index_work_item_definitions_projectId_name ON work_item_definitions(projectId,name)",
                    "CREATE INDEX index_block_type_work_items_workItemDefinitionId ON block_type_work_items(workItemDefinitionId)",
                    "CREATE INDEX index_block_work_items_blockId ON block_work_items(blockId)",
                    "CREATE INDEX index_block_work_items_workItemDefinitionId ON block_work_items(workItemDefinitionId)",
                    "CREATE UNIQUE INDEX index_block_work_items_blockId_workItemDefinitionId ON block_work_items(blockId,workItemDefinitionId)",
                    "CREATE INDEX index_problem_definitions_projectId ON problem_definitions(projectId)",
                    "CREATE UNIQUE INDEX index_problem_definitions_projectId_code ON problem_definitions(projectId,code)",
                    "CREATE INDEX index_problem_records_blockWorkItemId ON problem_records(blockWorkItemId)",
                    "CREATE INDEX index_problem_records_problemDefinitionId ON problem_records(problemDefinitionId)",
                    "CREATE INDEX index_deficiencies_blockWorkItemId ON deficiencies(blockWorkItemId)",
                    "CREATE INDEX index_deficiencies_status ON deficiencies(status)",
                    "CREATE INDEX index_deficiencies_targetDate ON deficiencies(targetDate)",
                    "CREATE INDEX index_notes_blockWorkItemId ON notes(blockWorkItemId)",
                    "CREATE INDEX index_photos_blockWorkItemId ON photos(blockWorkItemId)",
                    "CREATE INDEX index_photos_problemRecordId ON photos(problemRecordId)",
                    "CREATE INDEX index_photos_deficiencyId ON photos(deficiencyId)",
                    "CREATE INDEX index_block_attribute_definitions_projectId ON block_attribute_definitions(projectId)",
                    "CREATE UNIQUE INDEX index_block_attribute_definitions_projectId_key ON block_attribute_definitions(projectId,key)",
                    "CREATE INDEX index_block_attribute_values_attributeDefinitionId ON block_attribute_values(attributeDefinitionId)",
                    "CREATE INDEX index_audit_events_blockWorkItemId ON audit_events(blockWorkItemId)",
                    "CREATE INDEX index_audit_events_createdAt ON audit_events(createdAt)"
                )
                indexStatements.forEach(db::execSQL)
                installIntegrityTriggers(db)
            }
        }

        val MIGRATION_6_7=object:Migration(6,7){
            override fun migrate(db:SupportSQLiteDatabase){
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS deficiency_definitions (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        active INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_deficiency_definitions_projectId ON deficiency_definitions(projectId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_deficiency_definitions_projectId_title ON deficiency_definitions(projectId,title)")
            }
        }

        val MIGRATION_7_8=object:Migration(7,8){
            override fun migrate(db:SupportSQLiteDatabase){
                db.execSQL("""
                    CREATE TABLE deficiencies_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        blockWorkItemId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        floor TEXT,
                        unitNumber TEXT,
                        unitName TEXT,
                        targetDate TEXT,
                        priority TEXT NOT NULL,
                        status TEXT NOT NULL,
                        includeInReport INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(blockWorkItemId) REFERENCES block_work_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO deficiencies_new(
                        id,blockWorkItemId,title,description,floor,unitNumber,unitName,
                        targetDate,priority,status,includeInReport,createdAt,updatedAt
                    )
                    SELECT
                        id,blockWorkItemId,title,description,floor,unitNumber,unitName,
                        targetDate,priority,status,includeInReport,createdAt,updatedAt
                    FROM deficiencies
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE photos_backup AS
                    SELECT id,blockWorkItemId,problemRecordId,deficiencyId,localUri,caption,includeInReport,createdAt
                    FROM photos
                """.trimIndent())
                db.execSQL("DROP TABLE photos")
                db.execSQL("DROP TABLE deficiencies")
                db.execSQL("ALTER TABLE deficiencies_new RENAME TO deficiencies")

                db.execSQL("""
                    CREATE TABLE photos (
                        id TEXT NOT NULL PRIMARY KEY,
                        blockWorkItemId TEXT NOT NULL,
                        problemRecordId TEXT,
                        deficiencyId TEXT,
                        localUri TEXT NOT NULL,
                        caption TEXT,
                        includeInReport INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(blockWorkItemId) REFERENCES block_work_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(problemRecordId) REFERENCES problem_records(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(deficiencyId) REFERENCES deficiencies(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO photos(
                        id,blockWorkItemId,problemRecordId,deficiencyId,localUri,caption,includeInReport,createdAt
                    )
                    SELECT id,blockWorkItemId,problemRecordId,deficiencyId,localUri,caption,includeInReport,createdAt
                    FROM photos_backup
                """.trimIndent())
                db.execSQL("DROP TABLE photos_backup")

                db.execSQL("CREATE INDEX index_deficiencies_blockWorkItemId ON deficiencies(blockWorkItemId)")
                db.execSQL("CREATE INDEX index_deficiencies_status ON deficiencies(status)")
                db.execSQL("CREATE INDEX index_deficiencies_targetDate ON deficiencies(targetDate)")
                db.execSQL("CREATE INDEX index_photos_blockWorkItemId ON photos(blockWorkItemId)")
                db.execSQL("CREATE INDEX index_photos_problemRecordId ON photos(problemRecordId)")
                db.execSQL("CREATE INDEX index_photos_deficiencyId ON photos(deficiencyId)")
                installIntegrityTriggers(db)
            }
        }

        private fun installIntegrityTriggers(db:SupportSQLiteDatabase){
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS photos_validate_insert
                BEFORE INSERT ON photos
                BEGIN
                    SELECT CASE
                        WHEN NEW.problemRecordId IS NOT NULL AND NEW.deficiencyId IS NOT NULL
                        THEN RAISE(ABORT,'photo cannot link both a finding and a deficiency')
                    END;
                    SELECT CASE
                        WHEN NEW.problemRecordId IS NOT NULL AND
                             (SELECT blockWorkItemId FROM problem_records WHERE id=NEW.problemRecordId) != NEW.blockWorkItemId
                        THEN RAISE(ABORT,'photo finding context mismatch')
                    END;
                    SELECT CASE
                        WHEN NEW.deficiencyId IS NOT NULL AND
                             (SELECT blockWorkItemId FROM deficiencies WHERE id=NEW.deficiencyId) != NEW.blockWorkItemId
                        THEN RAISE(ABORT,'photo deficiency context mismatch')
                    END;
                END
            """.trimIndent())
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS photos_validate_update
                BEFORE UPDATE OF blockWorkItemId,problemRecordId,deficiencyId ON photos
                BEGIN
                    SELECT CASE
                        WHEN NEW.problemRecordId IS NOT NULL AND NEW.deficiencyId IS NOT NULL
                        THEN RAISE(ABORT,'photo cannot link both a finding and a deficiency')
                    END;
                    SELECT CASE
                        WHEN NEW.problemRecordId IS NOT NULL AND
                             (SELECT blockWorkItemId FROM problem_records WHERE id=NEW.problemRecordId) != NEW.blockWorkItemId
                        THEN RAISE(ABORT,'photo finding context mismatch')
                    END;
                    SELECT CASE
                        WHEN NEW.deficiencyId IS NOT NULL AND
                             (SELECT blockWorkItemId FROM deficiencies WHERE id=NEW.deficiencyId) != NEW.blockWorkItemId
                        THEN RAISE(ABORT,'photo deficiency context mismatch')
                    END;
                END
            """.trimIndent())
        }
    }
}
