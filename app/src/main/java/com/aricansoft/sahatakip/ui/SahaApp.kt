package com.aricansoft.sahatakip.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aricansoft.sahatakip.SahaTakipApplication
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.ui.screens.BlockScreen
import com.aricansoft.sahatakip.ui.screens.HomeScreen
import com.aricansoft.sahatakip.ui.screens.ProjectScreen
import com.aricansoft.sahatakip.ui.screens.WorkItemDetailScreen

@Composable
fun SahaApp(){
    val nav=rememberNavController()
    val app=LocalContext.current.applicationContext as SahaTakipApplication
    val repository:SahaRepository=app.repository

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
                onBlock={blockId->nav.navigate("block/"+projectId+"/"+blockId)}
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
