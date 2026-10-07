package com.aricansoft.sahatakip.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aricansoft.sahatakip.SahaTakipApplication
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.ui.screens.BlockScreen
import com.aricansoft.sahatakip.ui.screens.DeficiencyScreen
import com.aricansoft.sahatakip.ui.screens.HomeScreen
import com.aricansoft.sahatakip.ui.screens.MatrixScreen
import com.aricansoft.sahatakip.ui.screens.ProjectScreen
import com.aricansoft.sahatakip.ui.screens.WorkItemDetailScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SahaApp(
    externalGkteUri:Uri?=null,
    onExternalGkteConsumed:()->Unit={}
){
    val nav=rememberNavController()
    val context=LocalContext.current
    val app=context.applicationContext as SahaTakipApplication
    val repository:SahaRepository=app.repository

    LaunchedEffect(externalGkteUri){
        val uri=externalGkteUri ?: return@LaunchedEffect
        onExternalGkteConsumed()

        runCatching{
            withContext(Dispatchers.IO){
                app.sitePackManager.importProject(uri)
            }
        }.onSuccess{projectId->
            nav.navigate("project/"+projectId){
                popUpTo("home")
                launchSingleTop=true
            }
            Toast.makeText(context,"GKTE proje dosyası içe aktarıldı.",Toast.LENGTH_SHORT).show()
        }.onFailure{error->
            Toast.makeText(
                context,
                "GKTE dosyası açılamadı: "+(error.message ?: "Bilinmeyen hata"),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    NavHost(navController=nav,startDestination="home"){
        composable("home"){
            HomeScreen(repository){projectId->nav.navigate("project/"+projectId)}
        }
        composable("project/{projectId}"){entry->
            val projectId=requireNotNull(entry.arguments?.getString("projectId"))
            ProjectScreen(
                repository=repository,
                projectId=projectId,
                onBack={nav.popBackStack()},
                onBlock={blockId->nav.navigate("block/"+projectId+"/"+blockId)},
                onWorkItem={id->nav.navigate("work/"+id)},
                onMatrix={nav.navigate("matrix/"+projectId)},
                onDeficiencies={nav.navigate("deficiencies/"+projectId)}
            )
        }
        composable("deficiencies/{projectId}"){entry->
            val projectId=requireNotNull(entry.arguments?.getString("projectId"))
            DeficiencyScreen(
                repository=repository,
                projectId=projectId,
                onBack={nav.popBackStack()},
                onWorkItem={id->nav.navigate("work/"+id)}
            )
        }
        composable("matrix/{projectId}"){entry->
            val projectId=requireNotNull(entry.arguments?.getString("projectId"))
            MatrixScreen(
                repository=repository,
                projectId=projectId,
                onBack={nav.popBackStack()},
                onWorkItem={id->nav.navigate("work/"+id)}
            )
        }
        composable("block/{projectId}/{blockId}"){entry->
            val projectId=requireNotNull(entry.arguments?.getString("projectId"))
            val blockId=requireNotNull(entry.arguments?.getString("blockId"))
            BlockScreen(
                repository=repository,
                projectId=projectId,
                blockId=blockId,
                onBack={nav.popBackStack()},
                onWorkItem={id->nav.navigate("work/"+id)}
            )
        }
        composable("work/{workItemId}"){entry->
            val id=requireNotNull(entry.arguments?.getString("workItemId"))
            WorkItemDetailScreen(repository=repository,blockWorkItemId=id,onBack={nav.popBackStack()})
        }
    }
}
