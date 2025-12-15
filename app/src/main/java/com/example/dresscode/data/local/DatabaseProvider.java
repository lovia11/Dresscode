package com.example.dresscode.data.local;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public final class DatabaseProvider {
    private static volatile AppDatabase instance;

    private DatabaseProvider() {
    }

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (DatabaseProvider.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "dresscode.db"
                    ).addMigrations(MIGRATION_1_2).build();
                }
            }
        }
        return instance;
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `outfits` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`tags` TEXT NOT NULL, " +
                            "`gender` TEXT NOT NULL, " +
                            "`style` TEXT NOT NULL, " +
                            "`season` TEXT NOT NULL, " +
                            "`scene` TEXT NOT NULL, " +
                            "`weather` TEXT NOT NULL, " +
                            "`colorHex` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL" +
                            ")"
            );
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`outfitId` INTEGER NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL" +
                            ")"
            );
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_outfitId` ON `favorites` (`outfitId`)");
        }
    };
}
