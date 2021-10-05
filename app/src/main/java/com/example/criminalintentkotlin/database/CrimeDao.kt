package com.example.criminalintentkotlin.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.criminalintentkotlin.Crime
import java.util.*

@Dao
interface CrimeDao {
    @Query("SELECT * FROM Crime")
    fun getCrimes(): LiveData<List<Crime>>


    @Query("SELECT * FROM Crime WHERE id=(:id)")
    fun getCrime(id: UUID): LiveData<Crime?>

    @Update
    fun updateCrime(crime: Crime)

    @Insert
    fun addCrime(crime: Crime)
}