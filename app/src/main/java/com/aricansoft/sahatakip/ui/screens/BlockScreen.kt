package com.aricansoft.sahatakip.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.data.db.WorkItemDefinitionEntity
import com.aricansoft.sahatakip.data.model.QualityStatus
import com.aricansoft.sahatakip.ui.InfoTooltip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockScreen(
    repository:SahaRepository,
    projectId:String,
    blockId:String,
    onBack:()->Unit,
    onWorkItem:(String)->Unit
){
    val scope=rememberCoroutineScope()
    val workItems by remember(blockId){repository.observeBlockWorkItems(blockId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val attributes by remember(blockId){repository.observeBlockAttributes(blockId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val definitions by remember(projectId){repository.observeWorkItemDefinitions(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val block by produceState<com.aricansoft.sahatakip.data.db.BlockEntity?>(null,blockId){
        value=repository.getBlock(blockId)
    }
    var showAdd by remember{mutableStateOf(false)}

    Scaffold(
        topBar={
            TopAppBar(
                title={Text(block?.code ?: "Blok")},
                navigationIcon={
                    IconButton(onClick=onBack){
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack,contentDescription="Geri")
                    }
                },
                actions={
                    IconButton(onClick={showAdd=true}){
                        Icon(Icons.Outlined.Add,contentDescription="İmalat ekle")
                    }
                }
            )
        }
    ){padding->
        Column(Modifier.fillMaxSize().padding(padding)){
            if(attributes.isNotEmpty()){
                Surface(tonalElevation=1.dp){
                    Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp)){
                        attributes.forEach{attr->
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text(attr.name+": "+attr.value,style=MaterialTheme.typography.bodyMedium)
                                attr.tooltip?.let{InfoTooltip(it)}
                            }
                        }
                    }
                }
            }

            androidx.compose.foundation.lazy.LazyColumn(
                modifier=Modifier.fillMaxSize(),
                contentPadding=PaddingValues(12.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)
            ){
                items(workItems.size,key={workItems[it].id}){index->
                    val item=workItems[index]
                    Card(
                        modifier=Modifier.fillMaxWidth().clickable{onWorkItem(item.id)}
                    ){
                        Column(Modifier.fillMaxWidth().padding(14.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text(item.name,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
                                item.tooltip?.let{InfoTooltip(it)}
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                horizontalArrangement=Arrangement.spacedBy(8.dp),
                                verticalAlignment=Alignment.CenterVertically
                            ){
                                AssistChip(onClick={},label={Text(item.progressStatus.label)})
                                if(item.qualityStatus==QualityStatus.DEFECTIVE || item.qualityStatus==QualityStatus.CRITICAL_DEFECT){
                                    Icon(Icons.Outlined.Warning,contentDescription=null)
                                    Text(item.qualityStatus.label,style=MaterialTheme.typography.labelMedium)
                                }
                                if(item.isBlocked){
                                    Text("BLOKE",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if(showAdd){
        AddWorkItemDialog(
            definitions=definitions,
            attachedIds=workItems.map{it.workItemDefinitionId}.toSet(),
            onDismiss={showAdd=false},
            onAttach={id->
                scope.launch{repository.attachWorkItem(blockId,id)}
                showAdd=false
            },
            onCreate={name,tooltip->
                scope.launch{repository.createWorkItemAndAttach(blockId,projectId,name,tooltip)}
                showAdd=false
            }
        )
    }
}

@Composable
private fun AddWorkItemDialog(
    definitions:List<WorkItemDefinitionEntity>,
    attachedIds:Set<String>,
    onDismiss:()->Unit,
    onAttach:(String)->Unit,
    onCreate:(String,String?)->Unit
){
    var name by remember{mutableStateOf("")}
    var tooltip by remember{mutableStateOf("")}
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("İmalat ekle")},
        text={
            Column(
                Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState())
            ){
                Text("Mevcut tanımlar",style=MaterialTheme.typography.labelLarge)
                definitions.filterNot{it.id in attachedIds}.forEach{def->
                    TextButton(onClick={onAttach(def.id)},modifier=Modifier.fillMaxWidth()){
                        Text(def.name,Modifier.fillMaxWidth())
                    }
                }
                HorizontalDivider(Modifier.padding(vertical=12.dp))
                Text("On the fly yeni tanım",style=MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value=name,
                    onValueChange={name=it},
                    label={Text("İmalat adı")},
                    modifier=Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value=tooltip,
                    onValueChange={tooltip=it},
                    label={Text("Tooltip / kısa açıklama")},
                    modifier=Modifier.fillMaxWidth()
                )
                Button(
                    onClick={onCreate(name,tooltip.ifBlank{null})},
                    enabled=name.isNotBlank(),
                    modifier=Modifier.padding(top=8.dp)
                ){Text("Yeni tanımla ve GK/GB/A bloğa ekle")}
            }
        },
        confirmButton={},
        dismissButton={TextButton(onClick=onDismiss){Text("Kapat")}}
    )
}
