package com.aricansoft.sahatakip.data

import androidx.room.withTransaction
import com.aricansoft.sahatakip.data.db.*
import com.aricansoft.sahatakip.data.model.*
import java.util.Locale

object LegacyKonyaSeed {
    const val PROJECT_ID="project-konya-444"

    private data class LegacyState(val finished:Boolean=false,val problemCode:String?=null)

    suspend fun seedIfEmpty(database:SahaDatabase){
        val dao=database.sahaDao()
        if(dao.projectCount()!=0) return

        val now=System.currentTimeMillis()
        val project=ProjectEntity(PROJECT_ID,"TOKİ Ardıçlı 444 Konut",now)
        val blockTypes=listOf(
            BlockTypeEntity("bt-gk",PROJECT_ID,"GK","GK Tip","Genel konut blok tipi"),
            BlockTypeEntity("bt-gb",PROJECT_ID,"GB","GB Tip","GB blok tipi"),
            BlockTypeEntity("bt-a",PROJECT_ID,"A","A Tip","A blok tipi")
        )
        val blocks=buildList{
            (1..17).forEach{add(BlockEntity("block-gk-"+it,PROJECT_ID,"bt-gk","GK-"+it,it))}
            (1..6).forEach{add(BlockEntity("block-gb-"+it,PROJECT_ID,"bt-gb","GB-"+it,it))}
            (1..2).forEach{add(BlockEntity("block-a-"+it,PROJECT_ID,"bt-a","A-"+it,it))}
        }

        val gkNames=listOf(
            "K.A. Kolon Kablosu",
            "Z.A. Kolon Kablosu",
            "ADP Pano Yerleştirme",
            "ADP Pano K.A. Kablo Bağlantıları",
            "Ortak Mahal Kablo",
            "Ortak Mahal Aydınlatma Armatürleri",
            "Ortak Mahal Exit Armatürleri",
            "Daire Pano",
            "Daire Kablolama",
            "Daire Anahtar Priz",
            "Daire Tijler",
            "Yangın-Diafon Kablo",
            "Daire Lambalar",
            "Kablo Tavaları",
            "Mutfak Fayans / Dolap"
        )
        val otherNames=gkNames.filterNot{it=="Yangın-Diafon Kablo" || it=="Daire Lambalar"}
        val allNames=(gkNames+otherNames).distinct()
        val definitions=allNames.map{
            WorkItemDefinitionEntity(
                id="wi-"+slug(it),
                projectId=PROJECT_ID,
                name=it,
                tooltip=when(it){
                    "Daire Pano"->"Daire elektrik panosunun imalat ve kontrol kaydı."
                    "Kablo Tavaları"->"Blok içindeki kablo tava imalatının saha durumu."
                    else->null
                }
            )
        }
        val byName=definitions.associateBy{it.name}
        val typeLinks=buildList{
            gkNames.forEachIndexed{i,n->add(BlockTypeWorkItemEntity("bt-gk",byName.getValue(n).id,i))}
            otherNames.forEachIndexed{i,n->
                add(BlockTypeWorkItemEntity("bt-gb",byName.getValue(n).id,i))
                add(BlockTypeWorkItemEntity("bt-a",byName.getValue(n).id,i))
            }
        }

        val problemDefs=listOf(
            ProblemDefinitionEntity("pd-lie",PROJECT_ID,"L.İ.E.","Lehimize inşaat eksiği",tooltip="Elektrik imalatını etkileyen inşaat eksiği."),
            ProblemDefinitionEntity("pd-e1",PROJECT_ID,"E-1","Kolon sigortası ve KAKR yok"),
            ProblemDefinitionEntity("pd-e2",PROJECT_ID,"E-2","Sigorta kutuları boş"),
            ProblemDefinitionEntity("pd-e3",PROJECT_ID,"E-3","Sigorta kutusu hiç yok"),
            ProblemDefinitionEntity("pd-iy",PROJECT_ID,"İ.Y.","İnceleme yapılamadı"),
            ProblemDefinitionEntity("pd-ikacma",PROJECT_ID,"İ.K.AÇMA.","İşlik kapısı açılmamalı"),
            ProblemDefinitionEntity("pd-islh",PROJECT_ID,"İŞL.H.","İşlik priz yanlış yerde"),
            ProblemDefinitionEntity("pd-bh",PROJECT_ID,"B.H.","Bodrum kat daire panosu hatası")
        )
        val problemByCode=problemDefs.associateBy{it.code}

        fun seq(prefix:String,range:IntRange)=range.map{prefix+"-"+it}.toSet()
        val states=mutableMapOf<Pair<String,String>,LegacyState>()
        fun finish(work:String,blockCodes:Set<String>){blockCodes.forEach{states[it to work]=LegacyState(finished=true)}}
        fun issue(work:String,block:String,code:String,finished:Boolean=false){states[block to work]=LegacyState(finished,code)}

        finish("K.A. Kolon Kablosu",seq("GK",1..8))
        finish("Z.A. Kolon Kablosu",seq("GK",1..4))
        finish("ADP Pano Yerleştirme",seq("GK",1..17)+seq("GB",1..6))
        finish("Ortak Mahal Kablo",seq("GK",1..15)+seq("GB",1..6))
        finish("Ortak Mahal Aydınlatma Armatürleri",seq("GK",1..12)+seq("GB",1..5))
        finish("Ortak Mahal Exit Armatürleri",setOf("GK-1"))
        finish("Daire Pano",seq("GK",1..12))
        finish("Daire Kablolama",seq("GK",1..12)+seq("GB",1..6)+setOf("A-1","A-2"))
        finish("Daire Anahtar Priz",seq("GK",1..6))
        finish("Daire Tijler",seq("GK",1..4))
        finish("Yangın-Diafon Kablo",seq("GK",1..12))
        finish("Daire Lambalar",seq("GK",1..4))
        finish("Kablo Tavaları",seq("GK",1..17)+seq("GB",1..6)+setOf("A-1","A-2"))
        finish("Mutfak Fayans / Dolap",seq("GK",1..11)+seq("GB",1..3))

        issue("Daire Pano","GK-9","E-1",true)
        issue("Daire Pano","GK-10","E-2",true)
        issue("Daire Pano","GK-11","E-2",true)
        issue("Daire Pano","GK-13","E-3")
        issue("Daire Pano","GK-14","E-3")
        issue("Daire Pano","GK-15","E-2")
        issue("Daire Pano","GK-16","E-2")
        issue("Daire Pano","GK-17","İ.Y.")
        issue("Daire Pano","GB-1","B.H.")
        issue("Daire Pano","GB-2","B.H.")
        issue("Daire Pano","GB-3","B.H.")

        issue("Daire Anahtar Priz","GB-1","İŞL.H.",true)
        issue("Daire Anahtar Priz","GB-2","İ.K.AÇMA.",true)
        issue("Daire Anahtar Priz","GB-3","İŞL.H.",true)
        issue("Daire Anahtar Priz","GB-4","İŞL.H.",true)
        issue("Daire Anahtar Priz","GB-5","İŞL.H.",true)
        issue("Daire Anahtar Priz","GB-6","İŞL.H.",true)

        (12..17).forEach{issue("Mutfak Fayans / Dolap","GK-"+it,"L.İ.E.")}
        (4..6).forEach{issue("Mutfak Fayans / Dolap","GB-"+it,"L.İ.E.")}
        issue("Mutfak Fayans / Dolap","A-1","L.İ.E.")
        issue("Mutfak Fayans / Dolap","A-2","L.İ.E.")

        val workItems=buildList{
            blocks.forEach{block->
                val names=if(block.blockTypeId=="bt-gk") gkNames else otherNames
                names.forEach{name->
                    val def=byName.getValue(name)
                    val state=states[block.code to name] ?: LegacyState()
                    add(BlockWorkItemEntity(
                        id=bwiId(block.code,name),
                        blockId=block.id,
                        workItemDefinitionId=def.id,
                        progressStatus=if(state.finished) ProgressStatus.FINISHED else ProgressStatus.NOT_STARTED,
                        // The legacy spreadsheet stores checkmarks and problem codes, not
                        // independent quality/control/blocking axes. Preserve the source:
                        // never invent those statuses from a problem code during migration.
                        qualityStatus=QualityStatus.NOT_EVALUATED,
                        controlStatus=ControlStatus.NOT_CHECKED,
                        isBlocked=false,
                        createdAt=now,
                        updatedAt=now
                    ))
                }
            }
        }

        val problemRecords=states.mapNotNull{entry->
            val key=entry.key
            val state=entry.value
            val code=state.problemCode ?: return@mapNotNull null
            val def=problemByCode[code] ?: return@mapNotNull null
            ProblemRecordEntity(
                id="pr-"+slug(key.first)+"-"+slug(key.second)+"-"+slug(code),
                blockWorkItemId=bwiId(key.first,key.second),
                problemDefinitionId=def.id,
                status=ProblemRecordStatus.OPEN,
                includeInReport=true,
                createdAt=now
            )
        }

        val attributeDef=BlockAttributeDefinitionEntity(
            id="bad-horizontal-tray",
            projectId=PROJECT_ID,
            key="horizontal_tray_exists",
            name="Yatay tava var mı?",
            tooltip="Blokta yatay kablo tavasının mevcut olup olmadığını gösteren bağımsız blok parametresi."
        )
        val attrValues=buildList{
            (1..17).forEach{
                add(BlockAttributeValueEntity("block-gk-"+it,attributeDef.id,if(it<=12)"Evet" else "Hayır",now))
            }
            (1..6).forEach{
                add(BlockAttributeValueEntity("block-gb-"+it,attributeDef.id,if(it<=2)"Evet" else "Hayır",now))
            }
        }

        database.withTransaction{
            dao.insertProjects(listOf(project))
            dao.insertBlockTypes(blockTypes)
            dao.insertBlocks(blocks)
            dao.insertWorkItemDefinitions(definitions)
            dao.insertBlockTypeWorkItems(typeLinks)
            dao.insertProblemDefinitions(problemDefs)
            dao.insertBlockAttributeDefinitions(listOf(attributeDef))
            dao.insertBlockAttributeValues(attrValues)
            dao.insertBlockWorkItems(workItems)
            dao.insertProblemRecords(problemRecords)
        }
    }

    fun bwiId(blockCode:String,workName:String)="bwi-"+slug(blockCode)+"-"+slug(workName)

    private fun slug(value:String)=value
        .lowercase(Locale.forLanguageTag("tr-TR"))
        .replace("ı","i").replace("ğ","g").replace("ü","u").replace("ş","s").replace("ö","o").replace("ç","c")
        .replace(Regex("[^a-z0-9]+"),"-")
        .trim('-')
}
