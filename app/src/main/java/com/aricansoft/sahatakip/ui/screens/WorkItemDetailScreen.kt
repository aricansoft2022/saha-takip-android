package com.aricansoft.sahatakip.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.data.db.DeficiencyEntity
import com.aricansoft.sahatakip.data.db.ProblemDefinitionEntity
import com.aricansoft.sahatakip.data.db.ProblemRecordRow
import com.aricansoft.sahatakip.data.model.*
import com.aricansoft.sahatakip.photo.PendingPhoto
import com.aricansoft.sahatakip.photo.PhotoStore
import com.aricansoft.sahatakip.ui.InfoTooltip
import com.aricansoft.sahatakip.ui.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val advantageContainer=Color(0xFFE6F4D7)
private val advantageContent=Color(0xFF285F16)

private data class FindingRecordContext(
    val specificDescription:String?,
    val floor:String?,
    val unitNumber:String?,
    val unitName:String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkItemDetailScreen(
    repository:SahaRepository,
    blockWorkItemId:String,
    onBack:()->Unit
){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val item by remember(blockWorkItemId){repository.observeBlockWorkItem(blockWorkItemId)}
        .collectAsStateWithLifecycle(initialValue=null)
    val definition by produceState<com.aricansoft.sahatakip.data.db.WorkItemDefinitionEntity?>(null,item?.workItemDefinitionId){
        value=item?.workItemDefinitionId?.let{repository.getWorkItemDefinition(it)}
    }
    val projectId by produceState<String?>(null,blockWorkItemId){
        value=repository.getProjectIdForBlockWorkItem(blockWorkItemId)
    }
    val photoContext by produceState<com.aricansoft.sahatakip.data.db.PhotoContextRow?>(null,blockWorkItemId){
        value=repository.getPhotoContext(blockWorkItemId)
    }

    val findingDefinitionsFlow=remember(projectId){
        projectId?.let{repository.observeProblemDefinitions(it)} ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val findingDefs by findingDefinitionsFlow.collectAsStateWithLifecycle(initialValue=emptyList())
    val findings by remember(blockWorkItemId){repository.observeProblemRecords(blockWorkItemId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val problems=remember(findings){findings.filter{it.kind==FindingKind.PROBLEM}}
    val advantages=remember(findings){findings.filter{it.kind==FindingKind.ADVANTAGE}}
    val deficiencies by remember(blockWorkItemId){repository.observeDeficiencies(blockWorkItemId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val notes by remember(blockWorkItemId){repository.observeNotes(blockWorkItemId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val photos by remember(blockWorkItemId){repository.observePhotos(blockWorkItemId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

    var showNote by remember{mutableStateOf(false)}
    var showProblem by remember{mutableStateOf(false)}
    var showAdvantage by remember{mutableStateOf(false)}
    var showDeficiency by remember{mutableStateOf(false)}
    var editingDeficiency by remember{mutableStateOf<DeficiencyEntity?>(null)}
    var pendingPhoto by remember{mutableStateOf<PendingPhoto?>(null)}
    var pendingFindingRecordId by remember{mutableStateOf<String?>(null)}
    var pendingDeficiencyId by remember{mutableStateOf<String?>(null)}
    var photoInReport by remember{mutableStateOf(true)}

    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){success->
        val pending=pendingPhoto
        val findingRecordId=pendingFindingRecordId
        val deficiencyId=pendingDeficiencyId
        if(success && pending!=null){
            scope.launch{
                repository.addPhoto(
                    blockWorkItemId=blockWorkItemId,
                    uri=pending.uri.toString(),
                    includeInReport=if(findingRecordId==null && deficiencyId==null) photoInReport else true,
                    problemRecordId=findingRecordId,
                    deficiencyId=deficiencyId
                )
            }
        }else{
            pending?.file?.delete()
        }
        pendingPhoto=null
        pendingFindingRecordId=null
        pendingDeficiencyId=null
    }

    Scaffold(
        topBar={
            TopAppBar(
                title={Text(definition?.name ?: "İmalat")},
                navigationIcon={
                    IconButton(onClick=onBack){
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack,contentDescription="Geri")
                    }
                }
            )
        }
    ){padding->
        val current=item
        if(current==null){
            Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.Center){
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)
        ){
            if(definition?.kind==WorkItemKind.RELATED_DISCIPLINE){
                Surface(
                    color=MaterialTheme.colorScheme.tertiaryContainer,
                    shape=MaterialTheme.shapes.small
                ){
                    Column(Modifier.fillMaxWidth().padding(10.dp)){
                        Text(
                            "Alakadar başka disiplin kalemi",
                            style=MaterialTheme.typography.labelLarge,
                            color=MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Elektrik imalatı değildir; elektrik işini etkilediği için takip edilir.",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            definition?.tooltip?.let{
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("Açıklama",style=MaterialTheme.typography.labelLarge)
                    InfoTooltip(it)
                }
            }

            QuickStatusSection(
                current=current,
                onProgress={scope.launch{repository.setProgress(blockWorkItemId,it)}},
                onQuality={scope.launch{repository.setQuality(blockWorkItemId,it)}},
                onControl={scope.launch{repository.setControl(blockWorkItemId,it)}},
                onBlocked={scope.launch{repository.setBlocked(blockWorkItemId,it)}}
            )

            StatusSection(
                title="İlerleme",
                values=ProgressStatus.entries,
                selected=current.progressStatus,
                label={it.label},
                onSelect={scope.launch{repository.setProgress(blockWorkItemId,it)}}
            )
            StatusSection(
                title="Kalite",
                values=QualityStatus.entries,
                selected=current.qualityStatus,
                label={it.label},
                onSelect={scope.launch{repository.setQuality(blockWorkItemId,it)}}
            )
            StatusSection(
                title="Kontrol",
                values=ControlStatus.entries,
                selected=current.controlStatus,
                label={it.label},
                onSelect={scope.launch{repository.setControl(blockWorkItemId,it)}}
            )

            Row(verticalAlignment=Alignment.CenterVertically){
                Checkbox(
                    checked=current.isBlocked,
                    onCheckedChange={scope.launch{repository.setBlocked(blockWorkItemId,it)}}
                )
                Text("Bloke")
            }

            FindingSection(
                repository=repository,
                kind=FindingKind.PROBLEM,
                records=problems,
                photoEnabled=photoContext!=null,
                onTakePhoto={recordId->
                    photoContext?.let{pc->
                        val pending=PhotoStore.create(context,pc)
                        pendingPhoto=pending
                        pendingFindingRecordId=recordId
                        camera.launch(pending.uri)
                    }
                },
                onTogglePhotoReport={id,checked->
                    scope.launch{repository.setPhotoReportInclusion(id,checked)}
                },
                onToggleReport={id,checked->scope.launch{repository.setProblemReportInclusion(id,checked)}},
                onClose={id->scope.launch{repository.closeProblem(id)}},
                onAdd={showProblem=true}
            )

            FindingSection(
                repository=repository,
                kind=FindingKind.ADVANTAGE,
                records=advantages,
                photoEnabled=photoContext!=null,
                onTakePhoto={recordId->
                    photoContext?.let{pc->
                        val pending=PhotoStore.create(context,pc)
                        pendingPhoto=pending
                        pendingFindingRecordId=recordId
                        camera.launch(pending.uri)
                    }
                },
                onTogglePhotoReport={id,checked->
                    scope.launch{repository.setPhotoReportInclusion(id,checked)}
                },
                onToggleReport={id,checked->scope.launch{repository.setProblemReportInclusion(id,checked)}},
                onClose={id->scope.launch{repository.closeProblem(id)}},
                onAdd={showAdvantage=true}
            )

            DeficiencySection(
                repository=repository,
                records=deficiencies,
                photoEnabled=photoContext!=null,
                onTakePhoto={deficiencyId->
                    photoContext?.let{pc->
                        val pending=PhotoStore.create(context,pc)
                        pendingPhoto=pending
                        pendingFindingRecordId=null
                        pendingDeficiencyId=deficiencyId
                        camera.launch(pending.uri)
                    }
                },
                onTogglePhotoReport={id,checked->
                    scope.launch{repository.setPhotoReportInclusion(id,checked)}
                },
                onToggleReport={id,checked->
                    scope.launch{repository.setDeficiencyReportInclusion(id,checked)}
                },
                onStatus={id,status->
                    scope.launch{repository.setDeficiencyStatus(id,status)}
                },
                onEdit={record->
                    editingDeficiency=record
                    showDeficiency=true
                },
                onAdd={
                    editingDeficiency=null
                    showDeficiency=true
                }
            )

            HorizontalDivider()
            Text("Notlar",style=MaterialTheme.typography.titleMedium)
            notes.forEach{note->
                OutlinedCard(Modifier.fillMaxWidth()){
                    Column(Modifier.padding(12.dp)){
                        Text(note.text)
                        Spacer(Modifier.height(4.dp))
                        Text(formatTime(note.createdAt),style=MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Checkbox(
                                checked=note.includeInReport,
                                onCheckedChange={checked->
                                    scope.launch{repository.setNoteReportInclusion(note.id,checked)}
                                }
                            )
                            Text("Rapora dahil")
                        }
                    }
                }
            }
            Button(onClick={showNote=true}){Text("Not ekle")}

            HorizontalDivider()
            Text("Fotoğraflar",style=MaterialTheme.typography.titleMedium)
            Row(verticalAlignment=Alignment.CenterVertically){
                Checkbox(checked=photoInReport,onCheckedChange={photoInReport=it})
                Text("Yeni fotoğraf rapora dahil")
            }
            Button(
                onClick={
                    val pc=photoContext ?: return@Button
                    val pending=PhotoStore.create(context,pc)
                    pendingPhoto=pending
                    pendingFindingRecordId=null
                    pendingDeficiencyId=null
                    camera.launch(pending.uri)
                },
                enabled=photoContext!=null
            ){
                Icon(Icons.Outlined.CameraAlt,contentDescription=null)
                Spacer(Modifier.width(8.dp))
                Text("Fotoğraf çek")
            }
            photos.forEach{photo->
                OutlinedCard(Modifier.fillMaxWidth()){
                    Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){
                        LocalPhotoThumbnail(photo.localUri)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)){
                            Text(formatTime(photo.createdAt))
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Checkbox(
                                    checked=photo.includeInReport,
                                    onCheckedChange={checked->
                                        scope.launch{repository.setPhotoReportInclusion(photo.id,checked)}
                                    }
                                )
                                Text("Rapora dahil",style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if(showDeficiency){
        DeficiencyDialog(
            initial=editingDeficiency,
            onDismiss={
                showDeficiency=false
                editingDeficiency=null
            },
            onSave={title,description,floor,unitNumber,unitName,responsible,targetDate,priority,include->
                val editing=editingDeficiency
                scope.launch{
                    if(editing==null){
                        repository.createDeficiency(
                            blockWorkItemId=blockWorkItemId,
                            title=title,
                            description=description,
                            floor=floor,
                            unitNumber=unitNumber,
                            unitName=unitName,
                            responsible=responsible,
                            targetDate=targetDate,
                            priority=priority,
                            includeInReport=include
                        )
                    }else{
                        repository.updateDeficiency(
                            id=editing.id,
                            title=title,
                            description=description,
                            floor=floor,
                            unitNumber=unitNumber,
                            unitName=unitName,
                            responsible=responsible,
                            targetDate=targetDate,
                            priority=priority,
                            includeInReport=include
                        )
                    }
                }
                showDeficiency=false
                editingDeficiency=null
            }
        )
    }

    if(showNote){
        NoteDialog(
            onDismiss={showNote=false},
            onSave={text,include->
                scope.launch{repository.addNote(blockWorkItemId,text,include)}
                showNote=false
            }
        )
    }

    if(showProblem && projectId!=null){
        FindingDialog(
            kind=FindingKind.PROBLEM,
            definitions=findingDefs,
            onDismiss={showProblem=false},
            onExisting={definitionId,recordContext,include->
                scope.launch{
                    repository.attachProblem(
                        blockWorkItemId=blockWorkItemId,
                        problemDefinitionId=definitionId,
                        specificDescription=recordContext.specificDescription,
                        floor=recordContext.floor,
                        unitNumber=recordContext.unitNumber,
                        unitName=recordContext.unitName,
                        includeInReport=include
                    )
                }
                showProblem=false
            },
            onCreate={code,title,tooltip,recordContext,include->
                scope.launch{
                    repository.createProblemAndAttach(
                        blockWorkItemId=blockWorkItemId,
                        projectId=requireNotNull(projectId),
                        code=code,
                        title=title,
                        tooltip=tooltip,
                        specificDescription=recordContext.specificDescription,
                        floor=recordContext.floor,
                        unitNumber=recordContext.unitNumber,
                        unitName=recordContext.unitName,
                        includeInReport=include,
                        kind=FindingKind.PROBLEM
                    )
                }
                showProblem=false
            }
        )
    }

    if(showAdvantage && projectId!=null){
        FindingDialog(
            kind=FindingKind.ADVANTAGE,
            definitions=findingDefs,
            onDismiss={showAdvantage=false},
            onExisting={definitionId,recordContext,include->
                scope.launch{
                    repository.attachProblem(
                        blockWorkItemId=blockWorkItemId,
                        problemDefinitionId=definitionId,
                        specificDescription=recordContext.specificDescription,
                        floor=recordContext.floor,
                        unitNumber=recordContext.unitNumber,
                        unitName=recordContext.unitName,
                        includeInReport=include
                    )
                }
                showAdvantage=false
            },
            onCreate={code,title,tooltip,recordContext,include->
                scope.launch{
                    repository.createProblemAndAttach(
                        blockWorkItemId=blockWorkItemId,
                        projectId=requireNotNull(projectId),
                        code=code,
                        title=title,
                        tooltip=tooltip,
                        specificDescription=recordContext.specificDescription,
                        floor=recordContext.floor,
                        unitNumber=recordContext.unitNumber,
                        unitName=recordContext.unitName,
                        includeInReport=include,
                        kind=FindingKind.ADVANTAGE
                    )
                }
                showAdvantage=false
            }
        )
    }
}

@Composable
private fun FindingSection(
    repository:SahaRepository,
    kind:FindingKind,
    records:List<ProblemRecordRow>,
    photoEnabled:Boolean,
    onTakePhoto:(String)->Unit,
    onTogglePhotoReport:(String,Boolean)->Unit,
    onToggleReport:(String,Boolean)->Unit,
    onClose:(String)->Unit,
    onAdd:()->Unit
){
    HorizontalDivider()
    val isAdvantage=kind==FindingKind.ADVANTAGE
    Text(if(isAdvantage)"Avantajlar" else "Problemler",style=MaterialTheme.typography.titleMedium)

    records.forEach{record->
        OutlinedCard(
            modifier=Modifier.fillMaxWidth(),
            colors=CardDefaults.outlinedCardColors(
                containerColor=if(isAdvantage)advantageContainer else MaterialTheme.colorScheme.surface
            )
        ){
            Column(Modifier.padding(12.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Icon(
                        if(isAdvantage)Icons.Outlined.CheckCircle else Icons.Outlined.ReportProblem,
                        contentDescription=null,
                        tint=if(isAdvantage)advantageContent else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        record.code+" — "+record.title,
                        Modifier.weight(1f),
                        color=if(isAdvantage)advantageContent else Color.Unspecified
                    )
                    record.tooltip?.let{InfoTooltip(it)}
                }
                Text(
                    if(record.status==ProblemRecordStatus.OPEN)"Açık" else "Kapalı",
                    color=if(isAdvantage && record.status==ProblemRecordStatus.OPEN)advantageContent else Color.Unspecified
                )
                record.specificDescription?.let{
                    Text("Özel tanım: "+it,style=MaterialTheme.typography.bodyMedium)
                }
                val locationParts=buildList{
                    record.floor?.let{add("Kat: "+it)}
                    record.unitNumber?.let{add("No: "+it)}
                    record.unitName?.let{add("Mahal / daire / birim: "+it)}
                }
                if(locationParts.isNotEmpty()){
                    Text(locationParts.joinToString(" · "),style=MaterialTheme.typography.bodySmall)
                }
                record.note?.let{Text("Not: "+it)}
                Text(formatTime(record.createdAt),style=MaterialTheme.typography.bodySmall)
                Row(verticalAlignment=Alignment.CenterVertically){
                    Checkbox(
                        checked=record.includeInReport,
                        onCheckedChange={checked->onToggleReport(record.id,checked)}
                    )
                    Text("Rapora dahil")
                }
                if(record.status==ProblemRecordStatus.OPEN){
                    TextButton(onClick={onClose(record.id)}){
                        Text(if(isAdvantage)"Avantajı kapat" else "Problemi kapat")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical=8.dp))
                FindingEvidencePhotos(
                    repository=repository,
                    problemRecordId=record.id,
                    photoEnabled=photoEnabled,
                    onTakePhoto={onTakePhoto(record.id)},
                    onToggleReport=onTogglePhotoReport
                )
            }
        }
    }

    Button(onClick=onAdd){
        Icon(
            if(isAdvantage)Icons.Outlined.CheckCircle else Icons.Outlined.ReportProblem,
            contentDescription=null
        )
        Spacer(Modifier.width(8.dp))
        Text(if(isAdvantage)"Avantaj ekle" else "Problem ekle")
    }
}

@Composable
private fun FindingEvidencePhotos(
    repository:SahaRepository,
    problemRecordId:String,
    photoEnabled:Boolean,
    onTakePhoto:()->Unit,
    onToggleReport:(String,Boolean)->Unit
){
    val photos by remember(problemRecordId){repository.observeFindingPhotos(problemRecordId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween
        ){
            Text(
                "Kanıt fotoğrafları ("+photos.size+")",
                style=MaterialTheme.typography.labelLarge
            )
            FilledTonalButton(
                onClick=onTakePhoto,
                enabled=photoEnabled
            ){
                Icon(Icons.Outlined.CameraAlt,contentDescription=null)
                Spacer(Modifier.width(6.dp))
                Text("Fotoğraf ekle")
            }
        }

        photos.forEach{photo->
            OutlinedCard(Modifier.fillMaxWidth()){
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    LocalPhotoThumbnail(photo.localUri)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){
                        Text(formatTime(photo.createdAt),style=MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Checkbox(
                                checked=photo.includeInReport,
                                onCheckedChange={checked->onToggleReport(photo.id,checked)}
                            )
                            Text("Rapora dahil",style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if(photos.isEmpty()){
            Text(
                "Henüz kanıt fotoğrafı yok. İstediğin kadar ekleyebilirsin.",
                style=MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DeficiencySection(
    repository:SahaRepository,
    records:List<DeficiencyEntity>,
    photoEnabled:Boolean,
    onTakePhoto:(String)->Unit,
    onTogglePhotoReport:(String,Boolean)->Unit,
    onToggleReport:(String,Boolean)->Unit,
    onStatus:(String,DeficiencyStatus)->Unit,
    onEdit:(DeficiencyEntity)->Unit,
    onAdd:()->Unit
){
    HorizontalDivider()
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween
    ){
        Text("İmalat Eksikleri",style=MaterialTheme.typography.titleMedium)
        Text(
            records.count{it.status!=DeficiencyStatus.VERIFIED}.toString()+" aktif",
            style=MaterialTheme.typography.labelMedium,
            color=MaterialTheme.colorScheme.tertiary
        )
    }

    records.forEach{record->
        OutlinedCard(
            modifier=Modifier.fillMaxWidth(),
            colors=CardDefaults.outlinedCardColors(
                containerColor=if(record.status==DeficiencyStatus.VERIFIED)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.tertiaryContainer
            )
        ){
            Column(
                Modifier.padding(12.dp),
                verticalArrangement=Arrangement.spacedBy(6.dp)
            ){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription=null,
                        tint=if(record.priority==DeficiencyPriority.CRITICAL)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(record.title,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
                    AssistChip(onClick={},label={Text(record.priority.label)})
                }

                record.description?.let{Text(it)}
                val location=buildList{
                    record.floor?.let{add("Kat: "+it)}
                    record.unitNumber?.let{add("No: "+it)}
                    record.unitName?.let{add("Mahal / daire / birim: "+it)}
                }
                if(location.isNotEmpty()){
                    Text(location.joinToString(" · "),style=MaterialTheme.typography.bodySmall)
                }
                record.responsible?.let{
                    Text("Sorumlu: "+it,style=MaterialTheme.typography.bodySmall)
                }
                record.targetDate?.let{
                    Text("Hedef: "+it,style=MaterialTheme.typography.bodySmall)
                }

                Text("Durum",style=MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    items(DeficiencyStatus.entries,key={it.name}){status->
                        FilterChip(
                            selected=record.status==status,
                            onClick={onStatus(record.id,status)},
                            label={Text(status.label)}
                        )
                    }
                }

                Row(verticalAlignment=Alignment.CenterVertically){
                    Checkbox(
                        checked=record.includeInReport,
                        onCheckedChange={checked->onToggleReport(record.id,checked)}
                    )
                    Text("Rapora dahil")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick={onEdit(record)}){Text("Düzenle")}
                }

                HorizontalDivider(Modifier.padding(vertical=4.dp))
                DeficiencyEvidencePhotos(
                    repository=repository,
                    deficiencyId=record.id,
                    photoEnabled=photoEnabled,
                    onTakePhoto={onTakePhoto(record.id)},
                    onToggleReport=onTogglePhotoReport
                )
            }
        }
    }

    Button(onClick=onAdd){
        Icon(Icons.Outlined.Warning,contentDescription=null)
        Spacer(Modifier.width(8.dp))
        Text("Eksik ekle")
    }
}

@Composable
private fun DeficiencyEvidencePhotos(
    repository:SahaRepository,
    deficiencyId:String,
    photoEnabled:Boolean,
    onTakePhoto:()->Unit,
    onToggleReport:(String,Boolean)->Unit
){
    val photos by remember(deficiencyId){repository.observeDeficiencyPhotos(deficiencyId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween
        ){
            Text("Eksik fotoğrafları ("+photos.size+")",style=MaterialTheme.typography.labelLarge)
            FilledTonalButton(onClick=onTakePhoto,enabled=photoEnabled){
                Icon(Icons.Outlined.CameraAlt,contentDescription=null)
                Spacer(Modifier.width(6.dp))
                Text("Fotoğraf ekle")
            }
        }
        photos.forEach{photo->
            OutlinedCard(Modifier.fillMaxWidth()){
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    LocalPhotoThumbnail(photo.localUri)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){
                        Text(formatTime(photo.createdAt),style=MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Checkbox(
                                checked=photo.includeInReport,
                                onCheckedChange={checked->onToggleReport(photo.id,checked)}
                            )
                            Text("Rapora dahil",style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if(photos.isEmpty()){
            Text("Henüz eksik fotoğrafı yok.",style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DeficiencyDialog(
    initial:DeficiencyEntity?=null,
    onDismiss:()->Unit,
    onSave:(String,String?,String?,String?,String?,String?,String?,DeficiencyPriority,Boolean)->Unit
){
    var title by remember(initial?.id){mutableStateOf(initial?.title.orEmpty())}
    var description by remember(initial?.id){mutableStateOf(initial?.description.orEmpty())}
    var floor by remember(initial?.id){mutableStateOf(initial?.floor.orEmpty())}
    var unitNumber by remember(initial?.id){mutableStateOf(initial?.unitNumber.orEmpty())}
    var unitName by remember(initial?.id){mutableStateOf(initial?.unitName.orEmpty())}
    var responsible by remember(initial?.id){mutableStateOf(initial?.responsible.orEmpty())}
    var targetDate by remember(initial?.id){mutableStateOf(initial?.targetDate.orEmpty())}
    var priority by remember(initial?.id){mutableStateOf(initial?.priority ?: DeficiencyPriority.NORMAL)}
    var include by remember(initial?.id){mutableStateOf(initial?.includeInReport ?: true)}
    val targetValid=targetDate.isBlank() || Regex("""\d{4}-\d{2}-\d{2}""").matches(targetDate.trim())

    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(if(initial==null)"İmalat eksiği ekle" else "İmalat eksiğini düzenle")},
        text={
            Column(
                Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(8.dp)
            ){
                OutlinedTextField(
                    value=title,
                    onValueChange={title=it},
                    label={Text("Eksik / yapılacak iş")},
                    modifier=Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value=description,
                    onValueChange={description=it},
                    label={Text("Açıklama")},
                    modifier=Modifier.fillMaxWidth(),
                    minLines=2
                )
                OutlinedTextField(
                    value=floor,
                    onValueChange={floor=it},
                    label={Text("Kat")},
                    modifier=Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedTextField(
                        value=unitNumber,
                        onValueChange={unitNumber=it},
                        label={Text("Mahal / daire / birim no")},
                        modifier=Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value=unitName,
                        onValueChange={unitName=it},
                        label={Text("Mahal / daire / birim adı")},
                        modifier=Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value=responsible,
                    onValueChange={responsible=it},
                    label={Text("Sorumlu kişi / ekip")},
                    modifier=Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value=targetDate,
                    onValueChange={targetDate=it},
                    label={Text("Hedef tarih (YYYY-AA-GG)")},
                    isError=!targetValid,
                    supportingText={
                        if(!targetValid) Text("Örn. 2026-10-15")
                    },
                    modifier=Modifier.fillMaxWidth()
                )
                Text("Öncelik",style=MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    items(DeficiencyPriority.entries,key={it.name}){value->
                        FilterChip(
                            selected=priority==value,
                            onClick={priority=value},
                            label={Text(value.label)}
                        )
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically){
                    Checkbox(checked=include,onCheckedChange={include=it})
                    Text("Rapora dahil")
                }
                Text(
                    "Giderildi durumu eksik kaydını kapatmaz; Kontrol edildi olana kadar aktif listede kalır.",
                    style=MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton={
            TextButton(
                onClick={
                    onSave(
                        title,
                        description.trim().ifBlank{null},
                        floor.trim().ifBlank{null},
                        unitNumber.trim().ifBlank{null},
                        unitName.trim().ifBlank{null},
                        responsible.trim().ifBlank{null},
                        targetDate.trim().ifBlank{null},
                        priority,
                        include
                    )
                },
                enabled=title.isNotBlank() && targetValid
            ){Text(if(initial==null)"Eksik aç" else "Kaydet")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}}
    )
}

@Composable
private fun QuickStatusSection(
    current:com.aricansoft.sahatakip.data.db.BlockWorkItemEntity,
    onProgress:(ProgressStatus)->Unit,
    onQuality:(QualityStatus)->Unit,
    onControl:(ControlStatus)->Unit,
    onBlocked:(Boolean)->Unit
){
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("Hızlı durum",style=MaterialTheme.typography.titleSmall)
            InfoTooltip("Hızlı seçim yalnızca ilgili ekseni değiştirir; diğer ilerleme, kalite, kontrol veya bloke bilgilerini silmez.")
        }
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            item{
                FilterChip(
                    selected=current.progressStatus==ProgressStatus.NOT_STARTED,
                    onClick={onProgress(ProgressStatus.NOT_STARTED)},
                    label={Text("Başlanmadı")}
                )
            }
            item{
                FilterChip(
                    selected=current.progressStatus==ProgressStatus.IN_PROGRESS,
                    onClick={onProgress(ProgressStatus.IN_PROGRESS)},
                    label={Text("Devam")}
                )
            }
            item{
                FilterChip(
                    selected=current.qualityStatus==QualityStatus.DEFECTIVE,
                    onClick={onQuality(QualityStatus.DEFECTIVE)},
                    label={Text("Kusurlu")}
                )
            }
            item{
                FilterChip(
                    selected=current.qualityStatus==QualityStatus.CRITICAL_DEFECT,
                    onClick={onQuality(QualityStatus.CRITICAL_DEFECT)},
                    label={Text("Ağır kusurlu")}
                )
            }
            item{
                FilterChip(
                    selected=current.progressStatus==ProgressStatus.FINISHED,
                    onClick={onProgress(ProgressStatus.FINISHED)},
                    label={Text("Bitti")}
                )
            }
            item{
                FilterChip(
                    selected=current.controlStatus==ControlStatus.CANNOT_CHECK,
                    onClick={onControl(ControlStatus.CANNOT_CHECK)},
                    label={Text("Kontrol edilemedi")}
                )
            }
            item{
                FilterChip(
                    selected=current.isBlocked,
                    onClick={onBlocked(!current.isBlocked)},
                    label={Text("Bloke")}
                )
            }
            item{
                FilterChip(
                    selected=current.controlStatus==ControlStatus.ACCEPTED,
                    onClick={onControl(ControlStatus.ACCEPTED)},
                    label={Text("Kabul")}
                )
            }
        }
    }
}

@Composable
private fun <T> StatusSection(
    title:String,
    values:List<T>,
    selected:T,
    label:(T)->String,
    onSelect:(T)->Unit
){
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text(title,style=MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            items(values){value->
                FilterChip(
                    selected=value==selected,
                    onClick={onSelect(value)},
                    label={Text(label(value))}
                )
            }
        }
    }
}

@Composable
private fun NoteDialog(onDismiss:()->Unit,onSave:(String,Boolean)->Unit){
    var text by remember{mutableStateOf("")}
    var include by remember{mutableStateOf(true)}
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Yeni not")},
        text={
            Column{
                OutlinedTextField(
                    value=text,
                    onValueChange={text=it},
                    label={Text("Saha notu")},
                    modifier=Modifier.fillMaxWidth(),
                    minLines=3
                )
                Row(verticalAlignment=Alignment.CenterVertically){
                    Checkbox(checked=include,onCheckedChange={include=it})
                    Text("Rapora dahil et")
                }
                Text("Tarih/saat otomatik eklenecek.",style=MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton={
            TextButton(onClick={onSave(text,include)},enabled=text.isNotBlank()){Text("Kaydet")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}}
    )
}

@Composable
private fun FindingDialog(
    kind:FindingKind,
    definitions:List<ProblemDefinitionEntity>,
    onDismiss:()->Unit,
    onExisting:(String,FindingRecordContext,Boolean)->Unit,
    onCreate:(String,String,String?,FindingRecordContext,Boolean)->Unit
){
    val singular=if(kind==FindingKind.ADVANTAGE)"Avantaj" else "Problem"
    val plural=if(kind==FindingKind.ADVANTAGE)"avantajlar" else "problemler"
    var code by remember{mutableStateOf("")}
    var title by remember{mutableStateOf("")}
    var tooltip by remember{mutableStateOf("")}
    var search by remember{mutableStateOf("")}
    var specificDescription by remember{mutableStateOf("")}
    var floor by remember{mutableStateOf("")}
    var unitNumber by remember{mutableStateOf("")}
    var unitName by remember{mutableStateOf("")}
    var include by remember{mutableStateOf(true)}

    fun recordContext()=FindingRecordContext(
        specificDescription=specificDescription.trim().ifBlank{null},
        floor=floor.trim().ifBlank{null},
        unitNumber=unitNumber.trim().ifBlank{null},
        unitName=unitName.trim().ifBlank{null}
    )
    val sameKindDefinitions=definitions.filter{it.kind==kind}
    val visibleDefinitions=sameKindDefinitions.filter{
        search.isBlank() ||
            it.code.contains(search,ignoreCase=true) ||
            it.title.contains(search,ignoreCase=true)
    }
    val normalizedCode=code.trim().uppercase(Locale.forLanguageTag("tr-TR"))
    val existingAnyKind=definitions.firstOrNull{
        it.code.uppercase(Locale.forLanguageTag("tr-TR"))==normalizedCode && normalizedCode.isNotBlank()
    }
    val existingDefinition=existingAnyKind?.takeIf{it.kind==kind}

    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(singular+" ekle")},
        text={
            Column(
                Modifier.heightIn(max=540.dp).verticalScroll(rememberScrollState())
            ){
                Text("Bu kayda özel bağlam",style=MaterialTheme.typography.labelLarge)
                Text(
                    "Aşağıdaki bilgiler katalog tanımını değiştirmez; yalnız bu saha kaydına aittir.",
                    style=MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value=specificDescription,
                    onValueChange={specificDescription=it},
                    label={Text("Bu probleme/avantaja özel tanım")},
                    modifier=Modifier.fillMaxWidth(),
                    minLines=2
                )
                OutlinedTextField(
                    value=floor,
                    onValueChange={floor=it},
                    label={Text("Kat")},
                    modifier=Modifier.fillMaxWidth(),
                    singleLine=true
                )
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedTextField(
                        value=unitNumber,
                        onValueChange={unitNumber=it},
                        label={Text("Mahal / daire / birim no")},
                        modifier=Modifier.weight(1f),
                        singleLine=true
                    )
                    OutlinedTextField(
                        value=unitName,
                        onValueChange={unitName=it},
                        label={Text("Mahal / daire / birim adı")},
                        modifier=Modifier.weight(1f),
                        singleLine=true
                    )
                }

                HorizontalDivider(Modifier.padding(vertical=12.dp))
                Text("Tanımlı "+plural,style=MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value=search,
                    onValueChange={search=it},
                    label={Text(singular+" kodu veya tanımı ara")},
                    modifier=Modifier.fillMaxWidth(),
                    singleLine=true
                )
                visibleDefinitions.forEach{def->
                    TextButton(
                        onClick={onExisting(def.id,recordContext(),include)},
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Text(def.code+" — "+def.title,Modifier.fillMaxWidth())
                    }
                }
                HorizontalDivider(Modifier.padding(vertical=12.dp))
                Text("On the fly yeni "+singular.lowercase(Locale.forLanguageTag("tr-TR")),style=MaterialTheme.typography.labelLarge)
                OutlinedTextField(code,{code=it},label={Text("Kod")},modifier=Modifier.fillMaxWidth())
                if(existingAnyKind!=null){
                    OutlinedCard(Modifier.fillMaxWidth().padding(top=8.dp)){
                        Column(Modifier.padding(10.dp)){
                            if(existingDefinition!=null){
                                Text("Bu kod zaten tanımlı.",style=MaterialTheme.typography.labelLarge)
                                Text(existingDefinition.code+" — "+existingDefinition.title)
                                TextButton(onClick={onExisting(existingDefinition.id,recordContext(),include)}){
                                    Text("Mevcut "+singular.lowercase(Locale.forLanguageTag("tr-TR"))+"ı kullan")
                                }
                            }else{
                                Text(
                                    "Bu kod zaten "+existingAnyKind.kind.label.lowercase(Locale.forLanguageTag("tr-TR"))+
                                        " olarak kullanılıyor.",
                                    style=MaterialTheme.typography.labelLarge
                                )
                                Text("Kodlar proje içinde tek anlam taşır; farklı bir kod kullan.")
                            }
                        }
                    }
                }
                OutlinedTextField(
                    title,
                    {title=it},
                    label={Text("Tanım")},
                    modifier=Modifier.fillMaxWidth(),
                    enabled=existingAnyKind==null
                )
                OutlinedTextField(
                    tooltip,
                    {tooltip=it},
                    label={Text("Tooltip")},
                    modifier=Modifier.fillMaxWidth(),
                    enabled=existingAnyKind==null
                )
                Row(verticalAlignment=Alignment.CenterVertically){
                    Checkbox(checked=include,onCheckedChange={include=it})
                    Text("Rapora dahil et")
                }
                Button(
                    onClick={onCreate(code,title,tooltip.ifBlank{null},recordContext(),include)},
                    enabled=code.isNotBlank() && title.isNotBlank() && existingAnyKind==null
                ){Text("Tanımla ve bu imalata ekle")}
            }
        },
        confirmButton={},
        dismissButton={TextButton(onClick=onDismiss){Text("Kapat")}}
    )
}

@Composable
private fun LocalPhotoThumbnail(uriString:String){
    val context=LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null,uriString){
        value=withContext(Dispatchers.IO){
            runCatching{
                context.contentResolver.openInputStream(Uri.parse(uriString)).use{BitmapFactory.decodeStream(it)}
            }.getOrNull()
        }
    }
    Surface(
        modifier=Modifier.size(84.dp),
        shape=MaterialTheme.shapes.small,
        tonalElevation=2.dp
    ){
        val bmp=bitmap
        if(bmp!=null){
            Image(
                bitmap=bmp.asImageBitmap(),
                contentDescription="Saha fotoğrafı",
                modifier=Modifier.fillMaxSize(),
                contentScale=ContentScale.Crop
            )
        }else{
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                Icon(Icons.Outlined.CameraAlt,contentDescription=null)
            }
        }
    }
}
