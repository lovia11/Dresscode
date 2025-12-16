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
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build();
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

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `outfits` ADD COLUMN `coverResId` INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `swap_jobs` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`outfitId` INTEGER NOT NULL, " +
                            "`personImageUri` TEXT NOT NULL, " +
                            "`resultImageUri` TEXT NOT NULL, " +
                            "`status` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL" +
                            ")"
            );
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `closet_items` ADD COLUMN `owner` TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE `favorites` ADD COLUMN `owner` TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE `swap_jobs` ADD COLUMN `owner` TEXT NOT NULL DEFAULT ''");
            database.execSQL("DROP INDEX IF EXISTS `index_favorites_outfitId`");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_owner_outfitId` ON `favorites` (`owner`, `outfitId`)");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `closet_items` ADD COLUMN `scene` TEXT NOT NULL DEFAULT ''");
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `outfits` ADD COLUMN `tagSource` TEXT NOT NULL DEFAULT 'SEED'");
            database.execSQL("ALTER TABLE `outfits` ADD COLUMN `tagModel` TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE `outfits` ADD COLUMN `aiTagsJson` TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE `outfits` ADD COLUMN `tagUpdatedAt` INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `closet_items` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0");

            database.execSQL("ALTER TABLE `swap_jobs` ADD COLUMN `sourceType` TEXT NOT NULL DEFAULT 'OUTFIT'");
            database.execSQL("ALTER TABLE `swap_jobs` ADD COLUMN `sourceRefId` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `swap_jobs` ADD COLUMN `sourceTitle` TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE `swap_jobs` ADD COLUMN `sourceImageUri` TEXT NOT NULL DEFAULT ''");

            database.execSQL("UPDATE `swap_jobs` SET `sourceRefId` = `outfitId` WHERE `sourceRefId` = 0 AND `outfitId` > 0");
            database.execSQL(
                    "UPDATE `swap_jobs` SET `sourceTitle` = (" +
                            "SELECT `title` FROM `outfits` o WHERE o.`id` = `swap_jobs`.`outfitId`" +
                            ") WHERE (`sourceTitle` = '' OR `sourceTitle` IS NULL) AND `outfitId` > 0"
            );
        }
    };
}
