package com.xckrt.studentplanner.db
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE subjectTitle = :subjectName")
    fun getNoteBySubject(subjectName: String): Flow<NoteEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)
    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<NoteEntity>>
    @Query("UPDATE notes SET isCompleted = :completed WHERE subjectTitle = :subject")
    suspend fun updateNoteStatus(subject: String, completed: Boolean)
    @Query("DELETE FROM notes WHERE subjectTitle = :subjectName")
    suspend fun deleteNoteBySubject(subjectName: String)
}