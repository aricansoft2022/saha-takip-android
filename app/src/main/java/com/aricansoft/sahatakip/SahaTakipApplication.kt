package com.aricansoft.sahatakip

import android.app.Application
import androidx.room.Room
import com.aricansoft.sahatakip.backup.SitePackManager
import com.aricansoft.sahatakip.data.LegacyKonyaSeed
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.data.db.SahaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SahaTakipApplication:Application(){
    val database:SahaDatabase by lazy{
        Room.databaseBuilder(this,SahaDatabase::class.java,"saha-takip.db")
            .addMigrations(SahaDatabase.MIGRATION_1_2,SahaDatabase.MIGRATION_2_3)
            .build()
    }
    val repository:SahaRepository by lazy{SahaRepository(database.sahaDao())}
    val sitePackManager:SitePackManager by lazy{SitePackManager(this,database)}

    override fun onCreate(){
        super.onCreate()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch{
            LegacyKonyaSeed.seedIfEmpty(database)
        }
    }
}
