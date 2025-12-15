package com.example.dresscode.data.local;

import android.content.Context;

import androidx.room.Room;

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
                    ).build();
                }
            }
        }
        return instance;
    }
}

