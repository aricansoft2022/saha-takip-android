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
    val notes by remember(blockWorkItemId){repository.observeNotes(blockWorkItemId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val photos by remember(blockWorkItemId){repository.observePhotos(blockWorkItemId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

    var showNote by remember{mutableStateOf(false)}
    var showProblem by remember{mutableStateOf(false)}
    var showAdvantage by remember{mutableStateOf(false)}
    var pendingPhoto by remember{mutableStateOf<PendingPhoto?>(null)}
    var pendingFindingRecordId by remember{mutableStateOf<String?>(null)}
    var photoInReport by remember{mutableStateOf(true)}

    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){success->
        val pending=pendingPhoto
        val findingRecordId=pendingFindingRecordId
        if(success && pending!=null){
            scope.launch{
                repository.addPhoto(
                    blockWorkItemId=blockWorkItemId,
                    uri=pending.uri.toString(),
                    includeInReport=if(findingRecordId==null) photoInReport else true,
                    problemRecordId=findingRecordId
                )
            }
        }else{
            pending?.file?.delete()
        }
        pendingPhoto=null
        pendingFindingRecordId=null
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
            onExisting={definitionId,include->
                scope.launch{repository.attachProblem(blockWorkItemId,definitionId,includeInReport=include)}
                showProblem=false
            },
            onCreate={code,title,tooltip,include->
                scope.launch{
                    repository.createProblemAndAttach(
                        blockWorkItemId=blockWorkItemId,
                        projectId=requireNotNull(projectId),
                        code=code,
                        title=title,
                        tooltip=tooltip,
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
            onExisting={definitionId,include->
                scope.launch{repository.attachProblem(blockWorkItemId,definitionId,includeInReport=include)}
                showAdvantage=false
            },
            onCreate={code,title,tooltip,include->
                scope.launch{
                    repository.createProblemAndAttach(
                        blockWorkItemId=blockWorkItemId,
                        projectId=requireNotNull(projectId),
                        code=code,
                        title=title,
                        tooltip=tooltip,
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
                record.note?.let{Text(it)}
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
    onExisting:(String,Boolean)->Unit,
    onCreate:(String,String,String?,Boolean)->Unit
){
    val singular=if(kind==FindingKind.ADVANTAGE)"Avantaj" else "Problem"
    val plural=if(kind==FindingKind.ADVANTAGE)"avantajlar" else "problemler"
    var code by remember{mutableStateOf("")}
    var title by remember{mutableStateOf("")}
    var tooltip by remember{mutableStateOf("")}
    var search by remember{mutableStateOf("")}
    var include by remember{mutableStateOf(true)}
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
                        onClick={onExisting(def.id,include)},
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
                                TextButton(onClick={onExisting(existingDefinition.id,include)}){
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
                    onClick={onCreate(code,title,tooltip.ifBlank{null},include)},
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
