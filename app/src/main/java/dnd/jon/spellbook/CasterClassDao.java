package dnd.jon.spellbook;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CasterClassDao extends DAO<CasterClass> {

    @Query("SELECT * FROM classes")
    List<CasterClass> getAllClasses();

    @Query("SELECT * FROM classes WHERE id = :id")
    CasterClass getClassByID(int id);

    @Query("SELECT * FROM classes WHERE name = :name")
    CasterClass getClassByName(String name);

}
