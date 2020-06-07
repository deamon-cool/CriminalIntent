package android.bignerdranch.com;

import android.bignerdranch.com.database.CrimeBaseHelper;
import android.bignerdranch.com.database.CrimeCursorWrapper;
import android.bignerdranch.com.database.CrimeDbSchema.CrimeTable;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CrimeLab {

    private static CrimeLab sCrimeLab;
    private Context mContext;
    private SQLiteDatabase mDatabase;

    public static CrimeLab get(Context context) {
        if (sCrimeLab == null) {    // CrimeLab jest dostępny na zewnątrz klasy i inne klasy mogą tworzyć sCrimeLab, więc jeżeli jeszcze nie został stworzony to tutaj jest tworzony przez konstruktor
            sCrimeLab = new CrimeLab(context);
        }
        return sCrimeLab;
    }

    private CrimeLab(Context context) { //konstruktor CrimeLab
        mContext = context.getApplicationContext();
        mDatabase = new CrimeBaseHelper(mContext).getWritableDatabase(); // otwiera lub tworzy bazę danych zbrodni jeżeli jeszcze nie istniała, w CrimeBaseHelper extends SQLiteOpenHelper wywołuje onCreate, onUpgrade i "czasem" onOpen, baza danych jest teraz gotowa do modyfikacji, po zakończeniu modifikacji wywołaj close()
    }

    public void addCrime(Crime c) {
        ContentValues values = getContentValues(c);

        mDatabase.insert(CrimeTable.NAME, null, values);    // wstawianie wiersza
    }

    public List<Crime> getCrimes() {
        List<Crime> crimes = new ArrayList<>();

        CrimeCursorWrapper cursor = queryCrimes(null, null);    //czyta wszystkie wiersze dla kursora

        try {
            cursor.moveToFirst();   // przesuń kursor do pierwszego wiersza, jeżeli kursor jest pusty to będzie false
            while (!cursor.isAfterLast()) {     // isAfterLast sprawdza czy kursor jest za ostatnim wierszem
                crimes.add(cursor.getCrime());      // dodaje wiersz z crime do List<crime>
                cursor.moveToNext();    // kursor wskazuje na zapytanie koljengo wiersza
            }
        } finally {
            cursor.close(); // konczy zamknięciem kursora
        }

        return crimes;
    }

    public Crime getCrime(UUID id) {
        CrimeCursorWrapper cursor = queryCrimes(CrimeTable.Cols.UUID + " = ?",
                new String[] {id.toString()});  // czyta tylko kursor o wyspecyfikowanym wierszu

        try {
            if (cursor.getCount() == 0) {
                return null;
            }

            cursor.moveToFirst();
            return cursor.getCrime();
        } finally {
            cursor.close();
        }
    }

    public File getPhotoFile(Crime crime) {
        File filesDir = mContext.getFilesDir(); // główny folder aplikacji (?)
        return new File(filesDir, crime.getPhotoFilename()); // zwraca zapisany lub pusty plik jpg ze ścieżką
    }

    public void updateCrime(Crime crime) {
        String uuidString = crime.getId().toString();
        ContentValues values = getContentValues(crime);

        mDatabase.update(CrimeTable.NAME, values, CrimeTable.Cols.UUID + " = ?",
                new String[]{uuidString}); // to >CrimeTable.Cols.UUID + " = ?"<  czytaj jak: szukaj w kolumnie UUID tego "?", czyli new String[]{uuidString} i to zakutalizuj
    }

    private CrimeCursorWrapper queryCrimes(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(CrimeTable.NAME, null, whereClause, whereArgs,
                null, null, null);

        return new CrimeCursorWrapper(cursor);
    }

    private static ContentValues getContentValues(Crime crime) {    // przypisuje dane Crime do odpowiednich kolumn, wykorzystane w updateCrime i addCrime
        ContentValues values = new ContentValues();
        values.put(CrimeTable.Cols.UUID, crime.getId().toString());
        values.put(CrimeTable.Cols.TITLE, crime.getTitle());
        values.put(CrimeTable.Cols.DATE, crime.getDate().getTime());
        values.put(CrimeTable.Cols.SOLVED, crime.isSolved() ? 1 : 0);

        return values;
    }
}
