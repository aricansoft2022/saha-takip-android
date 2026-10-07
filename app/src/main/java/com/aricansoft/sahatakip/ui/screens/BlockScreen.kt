package com.aricansoft.sahatakip.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.data.db.BlockAttributeDefinitionEntity
import com.aricansoft.sahatakip.data.db.BlockAttributeRow
import com.aricansoft.sahatakip.data.db.WorkItemDefinitionEntity
import com.aricansoft.sahatakip.data.model.QualityStatus
import com.aricansoft.sahatakip.data.model.WorkItemKind
import com.aricansoft.sahatakip.data.model.WorkItemScope
import com.aricansoft.sahatakip.ui.InfoTooltip
import kotlinx.coroutines.launch
import java.util.Locale

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
    val attributeDefinitions by remember(projectId){repository.observeBlockAttributeDefinitions(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val definitions by remember(projectId){repository.observeWorkItemDefinitions(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val block by produceState<com.aricansoft.sahatakip.data.db.BlockEntity?>(null,blockId){
        value=repository.getBlock(blockId)
    }
    var showAdd by remember{mutableStateOf(false)}
    var showAddAttribute by remember{mutableStateOf(false)}
    var editAttribute by remember{mutableStateOf<BlockAttributeRow?>(null)}

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
            Surface(tonalElevation=1.dp){
                Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp)){
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.SpaceBetween
                    ){
                        Text("Blok parametreleri",style=MaterialTheme.typography.labelLarge)
                        TextButton(onClick={showAddAttribute=true}){Text("+ Parametre")}
                    }
                    if(attributes.isEmpty()){
                        Text("Henüz özel parametre yok.",style=MaterialTheme.typography.bodySmall)
                    }else{
                        attributes.forEach{attr->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable{editAttribute=attr}
                                    .padding(vertical=4.dp),
                                verticalAlignment=Alignment.CenterVertically
                            ){
                                Column(Modifier.weight(1f)){
                                    Row(verticalAlignment=Alignment.CenterVertically){
                                        Text(attr.name+": "+attr.value,style=MaterialTheme.typography.bodyMedium)
                                        attr.tooltip?.let{InfoTooltip(it)}
                                    }
                                }
                                Icon(Icons.Outlined.Edit,contentDescription="Parametreyi düzenle")
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
                        modifier=Modifier.fillMaxWidth().clickable{onWorkItem(item.id)},
                        colors=CardDefaults.cardColors(
                            containerColor=if(item.kind==WorkItemKind.RELATED_DISCIPLINE)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ){
                        Column(Modifier.fillMaxWidth().padding(14.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Column(Modifier.weight(1f)){
                                    Text(item.name,style=MaterialTheme.typography.titleSmall)
                                    if(item.kind==WorkItemKind.RELATED_DISCIPLINE){
                                        Text(
                                            "ALAKADAR BAŞKA DİSİPLİN",
                                            style=MaterialTheme.typography.labelSmall,
                                            color=MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
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
                                if(item.openProblemCount>0){
                                    Text(
                                        "P"+if(item.openProblemCount>1)item.openProblemCount else "",
                                        style=MaterialTheme.typography.labelMedium,
                                        color=MaterialTheme.colorScheme.error
                                    )
                                }
                                if(item.openDeficiencyCount>0){
                                    Text(
                                        "E"+if(item.openDeficiencyCount>1)item.openDeficiencyCount else "",
                                        style=MaterialTheme.typography.labelMedium,
                                        color=MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                if(item.openAdvantageCount>0){
                                    Text(
                                        "A"+if(item.openAdvantageCount>1)item.openAdvantageCount else "",
                                        style=MaterialTheme.typography.labelMedium,
                                        color=MaterialTheme.colorScheme.primary
                                    )
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
            onAttach={id,itemScope->
                scope.launch{repository.attachWorkItem(blockId,id,itemScope)}
                showAdd=false
            },
            onCreate={name,tooltip,itemScope,kind->
                scope.launch{repository.createWorkItemAndAttach(blockId,projectId,name,tooltip,itemScope,kind)}
                showAdd=false
            }
        )
    }

    if(showAddAttribute){
        AddAttributeDialog(
            definitions=attributeDefinitions,
            assignedIds=attributes.map{it.attributeDefinitionId}.toSet(),
            onDismiss={showAddAttribute=false},
            onExisting={definitionId,value->
                scope.launch{repository.setBlockAttributeValue(blockId,definitionId,value)}
                showAddAttribute=false
            },
            onCreate={name,tooltip,value->
                scope.launch{repository.createBlockAttributeAndSet(blockId,projectId,name,tooltip,value)}
                showAddAttribute=false
            }
        )
    }

    editAttribute?.let{attr->
        EditAttributeDialog(
            attribute=attr,
            onDismiss={editAttribute=null},
            onSave={value->
                scope.launch{repository.setBlockAttributeValue(blockId,attr.attributeDefinitionId,value)}
                editAttribute=null
            }
        )
    }
}

@Composable
private fun AddWorkItemDialog(
    definitions:List<WorkItemDefinitionEntity>,
    attachedIds:Set<String>,
    onDismiss:()->Unit,
    onAttach:(String,WorkItemScope)->Unit,
    onCreate:(String,String?,WorkItemScope,WorkItemKind)->Unit
){
    var name by remember{mutableStateOf("")}
    var tooltip by remember{mutableStateOf("")}
    var search by remember{mutableStateOf("")}
    var definitionsExpanded by remember{mutableStateOf(false)}
    var scope by remember{mutableStateOf(WorkItemScope.THIS_BLOCK)}
    var kind by remember{mutableStateOf(WorkItemKind.ELECTRICAL)}
    val trLocale=remember{Locale.forLanguageTag("tr-TR")}
    val normalizedSearch=search.trim().lowercase(trLocale)
    val visibleDefinitions=definitions
        .filterNot{it.id in attachedIds && scope==WorkItemScope.THIS_BLOCK}
        .filter{
            normalizedSearch.isBlank() ||
                it.name.lowercase(trLocale).contains(normalizedSearch) ||
                (it.code?.lowercase(trLocale)?.contains(normalizedSearch)==true)
        }
        .sortedWith(
            compareBy<WorkItemDefinitionEntity>{if(it.kind==WorkItemKind.ELECTRICAL)0 else 1}
                .thenBy{it.name.lowercase(trLocale)}
        )

    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("İmalat ekle")},
        text={
            Column(
                Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState())
            ){
                Text("Kapsam",style=MaterialTheme.typography.labelLarge)
                WorkItemScope.entries.forEach{option->
                    Row(
                        Modifier.fillMaxWidth().clickable{scope=option}.padding(vertical=2.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        RadioButton(selected=scope==option,onClick={scope=option})
                        Text(option.label)
                    }
                }

                HorizontalDivider(Modifier.padding(vertical=12.dp))
                Text("Mevcut tanımlar",style=MaterialTheme.typography.labelLarge)
                Box(Modifier.fillMaxWidth()){
                    OutlinedTextField(
                        value=search,
                        onValueChange={
                            search=it
                            definitionsExpanded=true
                        },
                        label={Text("Mevcut imalat ara / seç")},
                        placeholder={Text("Dokununca tüm tanımlar açılır")},
                        modifier=Modifier
                            .fillMaxWidth()
                            .onFocusChanged{state->
                                if(state.isFocused) definitionsExpanded=true
                            },
                        singleLine=true,
                        trailingIcon={
                            IconButton(onClick={definitionsExpanded=!definitionsExpanded}){
                                Text(if(definitionsExpanded)"▲" else "▼")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded=definitionsExpanded,
                        onDismissRequest={definitionsExpanded=false},
                        modifier=Modifier.fillMaxWidth()
                    ){
                        if(visibleDefinitions.isEmpty()){
                            DropdownMenuItem(
                                text={
                                    Text(
                                        if(definitions.isEmpty())
                                            "Henüz mevcut tanım yok"
                                        else
                                            "Eşleşen tanım yok"
                                    )
                                },
                                onClick={},
                                enabled=false
                            )
                        }else{
                            visibleDefinitions.forEach{def->
                                DropdownMenuItem(
                                    text={
                                        Column{
                                            Text(def.name)
                                            val detail=buildList{
                                                def.code?.takeIf{it.isNotBlank()}?.let{add(it)}
                                                if(def.kind==WorkItemKind.RELATED_DISCIPLINE){
                                                    add("Alakadar başka disiplin")
                                                }
                                            }.joinToString(" · ")
                                            if(detail.isNotBlank()){
                                                Text(
                                                    detail,
                                                    style=MaterialTheme.typography.labelSmall,
                                                    color=if(def.kind==WorkItemKind.RELATED_DISCIPLINE)
                                                        MaterialTheme.colorScheme.tertiary
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick={
                                        definitionsExpanded=false
                                        onAttach(def.id,scope)
                                    }
                                )
                            }
                        }
                    }
                }
                Text(
                    "Arama imalat adı ve kodunda büyük/küçük harf duyarsız çalışır.",
                    style=MaterialTheme.typography.bodySmall
                )

                HorizontalDivider(Modifier.padding(vertical=12.dp))
                Text("On the fly yeni tanım",style=MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.fillMaxWidth().padding(vertical=4.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    Checkbox(
                        checked=kind==WorkItemKind.RELATED_DISCIPLINE,
                        onCheckedChange={checked->
                            kind=if(checked)WorkItemKind.RELATED_DISCIPLINE else WorkItemKind.ELECTRICAL
                        }
                    )
                    Column{
                        Text("Alakadar başka disiplin kalemi")
                        Text(
                            "Elektrik işi değildir; elektriği etkilediği için takip edilir ve listenin sonunda gösterilir.",
                            style=MaterialTheme.typography.bodySmall
                        )
                    }
                }
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
                    onClick={onCreate(name,tooltip.ifBlank{null},scope,kind)},
                    enabled=name.isNotBlank(),
                    modifier=Modifier.padding(top=8.dp)
                ){Text("Tanımla ve seçili kapsama ekle")}
            }
        },
        confirmButton={},
        dismissButton={TextButton(onClick=onDismiss){Text("Kapat")}}
    )
}

@Composable
private fun AddAttributeDialog(
    definitions:List<BlockAttributeDefinitionEntity>,
    assignedIds:Set<String>,
    onDismiss:()->Unit,
    onExisting:(String,String)->Unit,
    onCreate:(String,String?,String)->Unit
){
    var selectedId by remember{mutableStateOf<String?>(null)}
    var name by remember{mutableStateOf("")}
    var tooltip by remember{mutableStateOf("")}
    var value by remember{mutableStateOf("")}
    var creating by remember{mutableStateOf(false)}
    val available=definitions.filterNot{it.id in assignedIds}

    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Blok parametresi ekle")},
        text={
            Column(
                Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(8.dp)
            ){
                if(!creating && available.isNotEmpty()){
                    Text("Mevcut parametreler",style=MaterialTheme.typography.labelLarge)
                    available.forEach{def->
                        FilterChip(
                            selected=selectedId==def.id,
                            onClick={selectedId=def.id},
                            label={Text(def.name)}
                        )
                    }
                    TextButton(onClick={
                        creating=true
                        selectedId=null
                    }){Text("+ Yeni parametre tanımla")}
                }else{
                    Text("Yeni parametre",style=MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        name,
                        {name=it},
                        label={Text("Ad")},
                        modifier=Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        tooltip,
                        {tooltip=it},
                        label={Text("Tooltip / açıklama")},
                        modifier=Modifier.fillMaxWidth()
                    )
                    if(available.isNotEmpty()){
                        TextButton(onClick={creating=false}){Text("Mevcut parametreyi seç")}
                    }
                }
                OutlinedTextField(
                    value,
                    {value=it},
                    label={Text("Değer")},
                    modifier=Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton={
            TextButton(
                onClick={
                    if(creating || available.isEmpty()){
                        onCreate(name,tooltip.ifBlank{null},value)
                    }else{
                        onExisting(requireNotNull(selectedId),value)
                    }
                },
                enabled=value.isNotBlank() &&
                    ((creating || available.isEmpty()) && name.isNotBlank() || (!creating && selectedId!=null))
            ){Text("Kaydet")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}}
    )
}

@Composable
private fun EditAttributeDialog(
    attribute:BlockAttributeRow,
    onDismiss:()->Unit,
    onSave:(String)->Unit
){
    var value by remember(attribute.attributeDefinitionId){mutableStateOf(attribute.value)}
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(attribute.name)},
        text={
            Column{
                attribute.tooltip?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
                OutlinedTextField(
                    value=value,
                    onValueChange={value=it},
                    label={Text("Değer")},
                    modifier=Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton={
            TextButton(onClick={onSave(value)},enabled=value.isNotBlank()){Text("Kaydet")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}}
    )
}
