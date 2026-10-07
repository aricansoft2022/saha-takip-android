package com.aricansoft.sahatakip.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aricansoft.sahatakip.backup.SitePackManager
import com.aricansoft.sahatakip.data.db.BlockWorkItemEntity
import com.aricansoft.sahatakip.data.db.PhotoEntity
import com.aricansoft.sahatakip.data.db.SahaDatabase
import com.aricansoft.sahatakip.data.model.ProgressStatus
import com.aricansoft.sahatakip.data.model.WorkItemScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class SahaRepositoryIntegrityTest {
    private lateinit var database:SahaDatabase
    private lateinit var repository:SahaRepository

    @Before
    fun setup(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        database=Room.inMemoryDatabaseBuilder(context,SahaDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(SahaDatabase.CALLBACK)
            .build()
        repository=SahaRepository(database.sahaDao())
    }

    @After
    fun tearDown(){
        database.close()
    }

    @Test
    fun duplicateWorkItemNameReusesExistingDefinitionWithoutReplacingIt()=runBlocking{
        val project=repository.createProject("Test")
        val type=repository.createBlockType(project.id,"TEST",null,null)
        val block=repository.createBlock(project.id,type.id,1)

        val first=repository.createWorkItemAndAttach(
            block.id,project.id,"DAİRE PANO",null,WorkItemScope.THIS_BLOCK
        )
        val second=repository.createWorkItemAndAttach(
            block.id,project.id,"  daire pano  ",null,WorkItemScope.THIS_BLOCK
        )

        assertEquals(first.id,second.id)
        assertEquals(1,database.sahaDao().getWorkItemDefinitionsForProject(project.id).size)
        val blockItems=database.sahaDao().observeBlockWorkItems(block.id).first()
        assertEquals(1,blockItems.size)
        assertEquals(first.id,blockItems.single().workItemDefinitionId)
    }

    @Test
    fun foreignKeyRejectsOrphanBlockWorkItem()=runBlocking{
        assertThrows(Exception::class.java){
            runBlocking{
                database.sahaDao().insertBlockWorkItems(
                    listOf(
                        BlockWorkItemEntity(
                            id="bwi-orphan",
                            blockId="missing-block",
                            workItemDefinitionId="missing-definition",
                            createdAt=1L,
                            updatedAt=1L
                        )
                    )
                )
            }
        }
    }

    @Test
    fun repositoryRejectsPhotoLinkedToFindingFromAnotherWorkItem()=runBlocking{
        val project=repository.createProject("Test")
        val type=repository.createBlockType(project.id,"TEST",null,null)
        val block1=repository.createBlock(project.id,type.id,1)
        val block2=repository.createBlock(project.id,type.id,2)
        val definition=repository.createWorkItemAndAttach(
            block1.id,project.id,"Daire Pano",null,WorkItemScope.BLOCK_TYPE
        )
        val bwi1=database.sahaDao().observeBlockWorkItems(block1.id).first()
            .single{it.workItemDefinitionId==definition.id}
        val bwi2=database.sahaDao().observeBlockWorkItems(block2.id).first()
            .single{it.workItemDefinitionId==definition.id}

        repository.createProblemAndAttach(
            blockWorkItemId=bwi1.id,
            projectId=project.id,
            code="E-TEST",
            title="Test problemi"
        )
        val finding=database.sahaDao().observeProblemRecords(bwi1.id).first().single()

        assertThrows(IllegalArgumentException::class.java){
            runBlocking{
                repository.addPhoto(
                    blockWorkItemId=bwi2.id,
                    uri="content://invalid-but-not-opened/photo.jpg",
                    problemRecordId=finding.id
                )
            }
        }
    }

    @Test
    fun databaseRejectsPhotoLinkedToFindingAndDeficiencyTogether()=runBlocking{
        val project=repository.createProject("Test")
        val type=repository.createBlockType(project.id,"TEST",null,null)
        val block=repository.createBlock(project.id,type.id,1)
        val definition=repository.createWorkItemAndAttach(
            block.id,project.id,"Daire Pano",null,WorkItemScope.THIS_BLOCK
        )
        val bwi=database.sahaDao().observeBlockWorkItems(block.id).first()
            .single{it.workItemDefinitionId==definition.id}

        repository.createProblemAndAttach(
            blockWorkItemId=bwi.id,
            projectId=project.id,
            code="E-TEST",
            title="Test problemi"
        )
        val finding=database.sahaDao().observeProblemRecords(bwi.id).first().single()
        val deficiency=repository.createDeficiency(bwi.id,"Test eksiği")

        assertThrows(Exception::class.java){
            runBlocking{
                database.sahaDao().insertPhoto(
                    PhotoEntity(
                        id="photo-"+UUID.randomUUID(),
                        blockWorkItemId=bwi.id,
                        problemRecordId=finding.id,
                        deficiencyId=deficiency.id,
                        localUri="content://invalid-but-not-opened/photo.jpg",
                        createdAt=System.currentTimeMillis()
                    )
                )
            }
        }
    }

    @Test
    fun gkteRoundTripCreatesIndependentProjectAndPreservesCoreData()=runBlocking{
        val context=ApplicationProvider.getApplicationContext<Context>()
        val project=repository.createProject("Round Trip")
        val type=repository.createBlockType(project.id,"RT",null,null)
        val block=repository.createBlock(project.id,type.id,1)
        val definition=repository.createWorkItemAndAttach(
            block.id,project.id,"Daire Pano",null,WorkItemScope.THIS_BLOCK
        )
        val bwi=database.sahaDao().observeBlockWorkItems(block.id).first()
            .single{it.workItemDefinitionId==definition.id}
        repository.setProgress(bwi.id,ProgressStatus.FINISHED)
        repository.addNote(bwi.id,"Round-trip notu",true)

        val manager=SitePackManager(context,database)
        val packageUri=manager.exportGkte(project.id)
        val importedProjectId=manager.importProject(packageUri)

        assertNotEquals(project.id,importedProjectId)
        val imported=repository.getProjectReportSnapshot(importedProjectId)
        requireNotNull(imported)
        assertEquals("Round Trip",imported.projectName)
        assertEquals(1,imported.workItems.size)
        assertEquals("Daire Pano",imported.workItems.single().workItemName)
        assertEquals(ProgressStatus.FINISHED,imported.workItems.single().progressStatus)
        assertEquals(listOf("Round-trip notu"),imported.notes.map{it.text})
    }
}
