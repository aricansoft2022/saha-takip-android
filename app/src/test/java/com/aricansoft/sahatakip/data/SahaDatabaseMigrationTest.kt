package com.aricansoft.sahatakip.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.aricansoft.sahatakip.data.db.SahaDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class SahaDatabaseMigrationTest {
    @Test
    fun migration5To6PreservesDataAndEnforcesRelationships(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        val dbName="migration-5-6-"+UUID.randomUUID()+".db"
        var helper:SupportSQLiteOpenHelper?=null
        try{
            helper=FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(object:SupportSQLiteOpenHelper.Callback(5){
                        override fun onCreate(db:SupportSQLiteDatabase){
                            createV5Schema(db)
                            seedV5Data(db)
                        }

                        override fun onUpgrade(
                            db:SupportSQLiteDatabase,
                            oldVersion:Int,
                            newVersion:Int
                        )=Unit
                    })
                    .build()
            )

            val db=helper.writableDatabase
            db.setForeignKeyConstraintsEnabled(true)

            SahaDatabase.MIGRATION_5_6.migrate(db)

            assertEquals("Test Projesi",scalarString(db,"SELECT name FROM projects WHERE id='p1'"))
            assertEquals("Daire Pano",scalarString(db,"SELECT name FROM work_item_definitions WHERE id='wi1'"))
            assertEquals("Round-trip notu",scalarString(db,"SELECT text FROM notes WHERE id='n1'"))
            assertEquals("content://photo/1",scalarString(db,"SELECT localUri FROM photos WHERE id='ph1'"))
            assertEquals(1L,scalarLong(db,"SELECT COUNT(*) FROM deficiencies WHERE id='d1'"))

            db.query("PRAGMA foreign_key_check").use{cursor->
                assertFalse("Migration sonrasında foreign-key ihlali olmamalı.",cursor.moveToFirst())
            }

            db.query("PRAGMA foreign_key_list(notes)").use{cursor->
                assertTrue("notes.blockWorkItemId foreign key'i oluşmalı.",cursor.moveToFirst())
            }

            assertThrows(Exception::class.java){
                db.execSQL(
                    """
                    INSERT INTO notes(id,blockWorkItemId,text,includeInReport,createdAt)
                    VALUES('orphan-note','missing-bwi','yetim',1,1)
                    """.trimIndent()
                )
            }

            assertThrows(Exception::class.java){
                db.execSQL(
                    """
                    INSERT INTO photos(
                        id,blockWorkItemId,problemRecordId,deficiencyId,
                        localUri,caption,includeInReport,createdAt
                    ) VALUES(
                        'invalid-photo','bwi1','pr1','d1',
                        'content://invalid',NULL,1,2
                    )
                    """.trimIndent()
                )
            }
        }finally{
            helper?.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun migration6To7CreatesDeficiencyDefinitionCatalog(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        val dbName="migration-6-7-"+UUID.randomUUID()+".db"
        var helper:SupportSQLiteOpenHelper?=null
        try{
            helper=FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(object:SupportSQLiteOpenHelper.Callback(6){
                        override fun onCreate(db:SupportSQLiteDatabase){
                            db.execSQL(
                                "CREATE TABLE projects(id TEXT NOT NULL PRIMARY KEY,name TEXT NOT NULL,createdAt INTEGER NOT NULL)"
                            )
                            db.execSQL("INSERT INTO projects VALUES('p1','Test Projesi',1)")
                        }

                        override fun onUpgrade(
                            db:SupportSQLiteDatabase,
                            oldVersion:Int,
                            newVersion:Int
                        )=Unit
                    })
                    .build()
            )

            val db=helper.writableDatabase
            db.setForeignKeyConstraintsEnabled(true)
            SahaDatabase.MIGRATION_6_7.migrate(db)

            db.execSQL(
                """
                INSERT INTO deficiency_definitions(id,projectId,title,description,active)
                VALUES('dd1','p1','Kablo etiketi eksik','Etiket tamamlanacak',1)
                """.trimIndent()
            )
            assertEquals(
                "Kablo etiketi eksik",
                scalarString(db,"SELECT title FROM deficiency_definitions WHERE id='dd1'")
            )

            assertThrows(Exception::class.java){
                db.execSQL(
                    """
                    INSERT INTO deficiency_definitions(id,projectId,title,description,active)
                    VALUES('dd2','p1','Kablo etiketi eksik',NULL,1)
                    """.trimIndent()
                )
            }
            assertThrows(Exception::class.java){
                db.execSQL(
                    """
                    INSERT INTO deficiency_definitions(id,projectId,title,description,active)
                    VALUES('dd3','missing','Yetim tanım',NULL,1)
                    """.trimIndent()
                )
            }
        }finally{
            helper?.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun migration7To8RemovesResponsibilityAndPreservesDeficiencyEvidence(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        val dbName="migration-7-8-"+UUID.randomUUID()+".db"
        var helper:SupportSQLiteOpenHelper?=null
        try{
            helper=FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(object:SupportSQLiteOpenHelper.Callback(7){
                        override fun onCreate(db:SupportSQLiteDatabase){
                            db.execSQL("CREATE TABLE block_work_items(id TEXT NOT NULL PRIMARY KEY)")
                            db.execSQL("CREATE TABLE problem_records(id TEXT NOT NULL PRIMARY KEY)")
                            db.execSQL("""
                                CREATE TABLE deficiencies(
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
                            db.execSQL("""
                                CREATE TABLE photos(
                                    id TEXT NOT NULL PRIMARY KEY,
                                    blockWorkItemId TEXT NOT NULL,
                                    problemRecordId TEXT,
                                    deficiencyId TEXT,
                                    localUri TEXT NOT NULL,
                                    caption TEXT,
                                    includeInReport INTEGER NOT NULL,
                                    createdAt INTEGER NOT NULL
                                )
                            """.trimIndent())
                            db.execSQL("INSERT INTO block_work_items VALUES('bwi1')")
                            db.execSQL("""
                                INSERT INTO deficiencies VALUES(
                                    'd1','bwi1','Test eksiği','Açıklama','1','2','Daire 2',
                                    'Eski ekip','2026-10-20','HIGH','OPEN',1,1,2
                                )
                            """.trimIndent())
                            db.execSQL("""
                                INSERT INTO photos VALUES(
                                    'ph1','bwi1',NULL,'d1','content://photo/deficiency',NULL,1,3
                                )
                            """.trimIndent())
                        }

                        override fun onUpgrade(
                            db:SupportSQLiteDatabase,
                            oldVersion:Int,
                            newVersion:Int
                        )=Unit
                    })
                    .build()
            )

            val db=helper.writableDatabase
            db.setForeignKeyConstraintsEnabled(true)
            SahaDatabase.MIGRATION_7_8.migrate(db)

            val columns=mutableListOf<String>()
            db.query("PRAGMA table_info(deficiencies)").use{cursor->
                val nameIndex=cursor.getColumnIndexOrThrow("name")
                while(cursor.moveToNext()) columns+=cursor.getString(nameIndex)
            }
            assertFalse(columns.contains("responsible"))
            assertEquals("Test eksiği",scalarString(db,"SELECT title FROM deficiencies WHERE id='d1'"))
            assertEquals("d1",scalarString(db,"SELECT deficiencyId FROM photos WHERE id='ph1'"))
            assertEquals("content://photo/deficiency",scalarString(db,"SELECT localUri FROM photos WHERE id='ph1'"))
        }finally{
            helper?.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun scalarString(db:SupportSQLiteDatabase,sql:String):String=
        db.query(sql).use{cursor->
            check(cursor.moveToFirst()){"Beklenen satır bulunamadı: $sql"}
            cursor.getString(0)
        }

    private fun scalarLong(db:SupportSQLiteDatabase,sql:String):Long=
        db.query(sql).use{cursor->
            check(cursor.moveToFirst()){"Beklenen satır bulunamadı: $sql"}
            cursor.getLong(0)
        }

    private fun createV5Schema(db:SupportSQLiteDatabase){
        db.execSQL("CREATE TABLE projects(id TEXT NOT NULL PRIMARY KEY,name TEXT NOT NULL,createdAt INTEGER NOT NULL)")
        db.execSQL("""
            CREATE TABLE block_types(
                id TEXT NOT NULL PRIMARY KEY,
                projectId TEXT NOT NULL,
                code TEXT NOT NULL,
                name TEXT NOT NULL,
                tooltip TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE blocks(
                id TEXT NOT NULL PRIMARY KEY,
                projectId TEXT NOT NULL,
                blockTypeId TEXT NOT NULL,
                code TEXT NOT NULL,
                sequence INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE work_item_definitions(
                id TEXT NOT NULL PRIMARY KEY,
                projectId TEXT NOT NULL,
                code TEXT,
                name TEXT NOT NULL,
                description TEXT,
                tooltip TEXT,
                active INTEGER NOT NULL,
                kind TEXT NOT NULL DEFAULT 'ELECTRICAL'
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE block_type_work_items(
                blockTypeId TEXT NOT NULL,
                workItemDefinitionId TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                PRIMARY KEY(blockTypeId,workItemDefinitionId)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE block_work_items(
                id TEXT NOT NULL PRIMARY KEY,
                blockId TEXT NOT NULL,
                workItemDefinitionId TEXT NOT NULL,
                progressStatus TEXT NOT NULL,
                qualityStatus TEXT NOT NULL,
                controlStatus TEXT NOT NULL,
                isBlocked INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE problem_definitions(
                id TEXT NOT NULL PRIMARY KEY,
                projectId TEXT NOT NULL,
                code TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                tooltip TEXT,
                active INTEGER NOT NULL,
                kind TEXT NOT NULL DEFAULT 'PROBLEM'
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE problem_records(
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
                closedAt INTEGER
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE deficiencies(
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
        db.execSQL("""
            CREATE TABLE notes(
                id TEXT NOT NULL PRIMARY KEY,
                blockWorkItemId TEXT NOT NULL,
                text TEXT NOT NULL,
                includeInReport INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE photos(
                id TEXT NOT NULL PRIMARY KEY,
                blockWorkItemId TEXT NOT NULL,
                problemRecordId TEXT,
                deficiencyId TEXT,
                localUri TEXT NOT NULL,
                caption TEXT,
                includeInReport INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE block_attribute_definitions(
                id TEXT NOT NULL PRIMARY KEY,
                projectId TEXT NOT NULL,
                key TEXT NOT NULL,
                name TEXT NOT NULL,
                tooltip TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE block_attribute_values(
                blockId TEXT NOT NULL,
                attributeDefinitionId TEXT NOT NULL,
                value TEXT NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(blockId,attributeDefinitionId)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE audit_events(
                id TEXT NOT NULL PRIMARY KEY,
                blockWorkItemId TEXT NOT NULL,
                eventType TEXT NOT NULL,
                detail TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }

    private fun seedV5Data(db:SupportSQLiteDatabase){
        db.execSQL("INSERT INTO projects VALUES('p1','Test Projesi',1)")
        db.execSQL("INSERT INTO block_types VALUES('bt1','p1','GK','GK Tip',NULL)")
        db.execSQL("INSERT INTO blocks VALUES('b1','p1','bt1','GK-1',1)")
        db.execSQL("""
            INSERT INTO work_item_definitions
            VALUES('wi1','p1',NULL,'Daire Pano',NULL,NULL,1,'ELECTRICAL')
        """.trimIndent())
        db.execSQL("INSERT INTO block_type_work_items VALUES('bt1','wi1',1)")
        db.execSQL("""
            INSERT INTO block_work_items
            VALUES('bwi1','b1','wi1','FINISHED','OK','CHECKED',0,1,2)
        """.trimIndent())
        db.execSQL("""
            INSERT INTO problem_definitions
            VALUES('pd1','p1','P-1','Test problemi',NULL,NULL,1,'PROBLEM')
        """.trimIndent())
        db.execSQL("""
            INSERT INTO problem_records
            VALUES('pr1','bwi1','pd1','OPEN','not',NULL,'1','1','Daire 1',1,1,NULL)
        """.trimIndent())
        db.execSQL("""
            INSERT INTO deficiencies
            VALUES(
                'd1','bwi1','Test eksiği',NULL,'1','1','Daire 1',
                'Ekip','2026-10-15','NORMAL','OPEN',1,1,2
            )
        """.trimIndent())
        db.execSQL("INSERT INTO notes VALUES('n1','bwi1','Round-trip notu',1,1)")
        db.execSQL("""
            INSERT INTO photos
            VALUES('ph1','bwi1','pr1',NULL,'content://photo/1',NULL,1,1)
        """.trimIndent())
        db.execSQL("""
            INSERT INTO block_attribute_definitions
            VALUES('bad1','p1','custom_test','Test parametre',NULL)
        """.trimIndent())
        db.execSQL("INSERT INTO block_attribute_values VALUES('b1','bad1','Değer',1)")
        db.execSQL("INSERT INTO audit_events VALUES('a1','bwi1','NOTE_ADDED','Not eklendi',1)")
    }
}
