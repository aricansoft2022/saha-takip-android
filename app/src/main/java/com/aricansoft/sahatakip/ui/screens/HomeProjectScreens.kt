package com.aricansoft.sahatakip.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aricansoft.sahatakip.SahaTakipApplication
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.data.db.BlockTypeEntity
import com.aricansoft.sahatakip.report.ReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository:SahaRepository,onProject:(String)->Unit){
    val context=LocalContext.current
    val app=context.applicationContext as SahaTakipApplication
    val scope=rememberCoroutineScope()
    val snackbar=remember{SnackbarHostState()}
    val projects by repository.observeProjects().collectAsStateWithLifecycle(initialValue=emptyList())
    var showAdd by remember{mutableStateOf(false)}
    var importing by remember{mutableStateOf(false)}

    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            importing=true
            scope.launch{
                runCatching{
                    withContext(Dispatchers.IO){app.sitePackManager.importProject(uri)}
                }.onSuccess{projectId->
                    importing=false
                    onProject(projectId)
                }.onFailure{
                    importing=false
                    snackbar.showSnackbar("Yedek içe aktarılamadı: "+(it.message ?: "Bilinmeyen hata"))
                }
            }
        }
    }

    Scaffold(
        snackbarHost={SnackbarHost(snackbar)},
        topBar={
            TopAppBar(
                title={Text("Saha Takip")},
                actions={
                    if(importing){
                        CircularProgressIndicator(modifier=Modifier.size(22.dp),strokeWidth=2.dp)
                    }else{
                        IconButton(onClick={
                            importLauncher.launch(arrayOf("application/zip","application/octet-stream","*/*"))
                        }){
                            Icon(Icons.Outlined.Unarchive,contentDescription="Sitepack içe aktar")
                        }
                    }
                }
            )
        },
        floatingActionButton={
            FloatingActionButton(onClick={showAdd=true}){
                Icon(Icons.Outlined.Add,contentDescription="Proje ekle")
            }
        }
    ){padding->
        if(projects.isEmpty()){
            Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.Center){
                CircularProgressIndicator()
            }
        }else{
            LazyColumn(
                modifier=Modifier.fillMaxSize().padding(padding),
                contentPadding=PaddingValues(16.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ){
                items(projects,key={it.id}){project->
                    ElevatedCard(
                        modifier=Modifier.fillMaxWidth().clickable{onProject(project.id)}
                    ){
                        Row(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Icon(Icons.Outlined.Apartment,contentDescription=null)
                            Spacer(Modifier.width(12.dp))
                            Column{
                                Text(project.name,style=MaterialTheme.typography.titleMedium)
                                Text("Blokları aç",style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if(showAdd){
        AddProjectDialog(
            onDismiss={showAdd=false},
            onCreate={name->
                scope.launch{
                    val project=repository.createProject(name)
                    showAdd=false
                    onProject(project.id)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    repository:SahaRepository,
    projectId:String,
    onBack:()->Unit,
    onBlock:(String)->Unit,
    onMatrix:()->Unit
){
    val context=LocalContext.current
    val app=context.applicationContext as SahaTakipApplication
    val scope=rememberCoroutineScope()
    val snackbar=remember{SnackbarHostState()}
    var exporting by remember{mutableStateOf(false)}
    var backingUp by remember{mutableStateOf(false)}
    var showAddBlock by remember{mutableStateOf(false)}
    val blocks by remember(projectId){repository.observeBlocks(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val blockTypes by remember(projectId){repository.observeBlockTypes(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

    fun exportBackup(){
        if(backingUp) return
        backingUp=true
        scope.launch{
            runCatching{
                val uri=withContext(Dispatchers.IO){
                    app.sitePackManager.exportProject(projectId)
                }
                val share=Intent(Intent.ACTION_SEND).apply{
                    type="application/zip"
                    putExtra(Intent.EXTRA_STREAM,uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share,"Proje yedeğini paylaş"))
            }.onFailure{
                snackbar.showSnackbar("Yedek oluşturulamadı: "+(it.message ?: "Bilinmeyen hata"))
            }
            backingUp=false
        }
    }

    fun exportPdf(){
        if(exporting) return
        exporting=true
        scope.launch{
            runCatching{
                val snapshot=withContext(Dispatchers.IO){
                    repository.getProjectReportSnapshot(projectId)
                        ?: error("Proje bulunamadı.")
                }
                val uri=withContext(Dispatchers.IO){
                    ReportExporter.exportProject(context,snapshot)
                }
                val share=Intent(Intent.ACTION_SEND).apply{
                    type="application/pdf"
                    putExtra(Intent.EXTRA_STREAM,uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share,"Saha raporunu paylaş"))
            }.onFailure{
                snackbar.showSnackbar("PDF oluşturulamadı: "+(it.message ?: "Bilinmeyen hata"))
            }
            exporting=false
        }
    }

    Scaffold(
        snackbarHost={SnackbarHost(snackbar)},
        topBar={
            TopAppBar(
                title={Text("Bloklar")},
                navigationIcon={
                    IconButton(onClick=onBack){
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack,contentDescription="Geri")
                    }
                },
                actions={
                    if(backingUp){
                        CircularProgressIndicator(modifier=Modifier.size(22.dp),strokeWidth=2.dp)
                    }else{
                        IconButton(onClick={exportBackup()}){
                            Icon(Icons.Outlined.Archive,contentDescription="Sitepack yedeği oluştur")
                        }
                    }
                    IconButton(onClick=onMatrix){
                        Icon(Icons.Outlined.TableChart,contentDescription="İmalat matrisi")
                    }
                    if(exporting){
                        CircularProgressIndicator(
                            modifier=Modifier.size(22.dp),
                            strokeWidth=2.dp
                        )
                    }else{
                        IconButton(onClick={exportPdf()}){
                            Icon(Icons.Outlined.PictureAsPdf,contentDescription="PDF rapor oluştur")
                        }
                    }
                }
            )
        },
        floatingActionButton={
            FloatingActionButton(onClick={showAddBlock=true}){
                Icon(Icons.Outlined.Add,contentDescription="Blok ekle")
            }
        }
    ){padding->
        LazyColumn(
            modifier=Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            items(blocks,key={it.id}){block->
                Card(
                    modifier=Modifier.fillMaxWidth().clickable{onBlock(block.id)}
                ){
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement=Arrangement.SpaceBetween,
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        Text(block.code,style=MaterialTheme.typography.titleMedium)
                        Text("İmalatlar →",style=MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if(showAddBlock){
        AddBlockDialog(
            blockTypes=blockTypes,
            onDismiss={showAddBlock=false},
            onCreate={typeId,newTypeCode,newTypeName,newTypeTooltip,sequence->
                scope.launch{
                    runCatching{
                        val resolvedTypeId=typeId ?: repository.createBlockType(
                            projectId=projectId,
                            code=newTypeCode,
                            name=newTypeName,
                            tooltip=newTypeTooltip
                        ).id
                        repository.createBlock(projectId,resolvedTypeId,sequence)
                    }.onSuccess{
                        showAddBlock=false
                    }.onFailure{
                        snackbar.showSnackbar(it.message ?: "Blok oluşturulamadı.")
                    }
                }
            }
        )
    }
}

@Composable
private fun AddProjectDialog(onDismiss:()->Unit,onCreate:(String)->Unit){
    var name by remember{mutableStateOf("")}
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Yeni proje")},
        text={
            OutlinedTextField(
                value=name,
                onValueChange={name=it},
                label={Text("Proje adı")},
                modifier=Modifier.fillMaxWidth()
            )
        },
        confirmButton={
            TextButton(onClick={onCreate(name)},enabled=name.isNotBlank()){Text("Oluştur")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}}
    )
}

@Composable
private fun AddBlockDialog(
    blockTypes:List<BlockTypeEntity>,
    onDismiss:()->Unit,
    onCreate:(String?,String,String?,String?,Int)->Unit
){
    var selectedTypeId by remember{mutableStateOf<String?>(blockTypes.firstOrNull()?.id)}
    var creatingType by remember{mutableStateOf(blockTypes.isEmpty())}
    var typeCode by remember{mutableStateOf("")}
    var typeName by remember{mutableStateOf("")}
    var typeTooltip by remember{mutableStateOf("")}
    var sequenceText by remember{mutableStateOf("")}

    val sequence=sequenceText.toIntOrNull()

    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Blok ekle")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                if(!creatingType){
                    Text("Blok tipi",style=MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        blockTypes.take(5).forEach{type->
                            FilterChip(
                                selected=selectedTypeId==type.id,
                                onClick={selectedTypeId=type.id},
                                label={Text(type.code)}
                            )
                        }
                    }
                    TextButton(onClick={
                        creatingType=true
                        selectedTypeId=null
                    }){Text("+ Yeni blok tipi tanımla")}
                }else{
                    Text("Yeni blok tipi",style=MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        typeCode,
                        {typeCode=it},
                        label={Text("Kod (örn. GK)")},
                        modifier=Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        typeName,
                        {typeName=it},
                        label={Text("Ad (opsiyonel)")},
                        modifier=Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        typeTooltip,
                        {typeTooltip=it},
                        label={Text("Tooltip / açıklama (opsiyonel)")},
                        modifier=Modifier.fillMaxWidth()
                    )
                    if(blockTypes.isNotEmpty()){
                        TextButton(onClick={
                            creatingType=false
                            selectedTypeId=blockTypes.first().id
                        }){Text("Mevcut blok tipini seç")}
                    }
                }

                OutlinedTextField(
                    value=sequenceText,
                    onValueChange={sequenceText=it.filter(Char::isDigit)},
                    label={Text("Blok numarası (örn. 9)")},
                    modifier=Modifier.fillMaxWidth()
                )

                val typeCodePreview=if(creatingType) typeCode.trim().uppercase()
                    else blockTypes.firstOrNull{it.id==selectedTypeId}?.code.orEmpty()
                if(typeCodePreview.isNotBlank() && sequence!=null){
                    Text(
                        "Oluşacak blok: "+typeCodePreview+"-"+sequence,
                        style=MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton={
            TextButton(
                onClick={
                    onCreate(
                        if(creatingType)null else selectedTypeId,
                        typeCode,
                        typeName.ifBlank{null},
                        typeTooltip.ifBlank{null},
                        requireNotNull(sequence)
                    )
                },
                enabled=sequence!=null && sequence>0 &&
                    ((!creatingType && selectedTypeId!=null) || (creatingType && typeCode.isNotBlank()))
            ){Text("Ekle")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}}
    )
}
