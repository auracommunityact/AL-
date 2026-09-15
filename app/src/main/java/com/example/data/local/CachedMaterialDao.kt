package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedMaterialDao {
    @Query("SELECT * FROM cached_materials WHERE type = :type ORDER BY cachedAt DESC")
    suspend fun getCachedMaterialsByType(type: String): List<CachedMaterialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<CachedMaterialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: CachedMaterialEntity)
}
