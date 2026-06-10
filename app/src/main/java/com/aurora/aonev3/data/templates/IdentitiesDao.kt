package com.aurora.aonev3.data.templates

import androidx.room.*

@Dao
interface IdentitiesDao {

    @Query("SELECT deviceClass FROM identities WHERE defaultName = :defaultName LIMIT 1")
    suspend fun getDeviceClassForDefaultName(defaultName: String): String?

    @Query("SELECT * FROM identities")
    suspend fun getAll(): List<Identity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg identity: Identity)

    @Delete
    suspend fun delete(vararg identities: Identity)

    @Query("DELETE FROM identities")
    suspend fun delete()
}