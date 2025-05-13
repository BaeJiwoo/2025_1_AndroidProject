package com.example.a2025termproject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.ArrayList;

public class ChatHistoryDatabaseHelper extends SQLiteOpenHelper
{
    public static final String DATABASE_NAME = "ChatHistory.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "chat_summary";

    public ChatHistoryDatabaseHelper(@Nullable Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                "date TEXT PRIMARY KEY, " +
                "summary TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /// 채팅 기록을 추가한다.
    public void insert(String summary)
    {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("date", String.valueOf(System.currentTimeMillis())); // 현재 시간 기준
        values.put("summary", summary);

        db.insert(TABLE_NAME, null, values);
    }

    /// 채팅 요약본을 날짜 오름차순으로 모두 제공한다.
    public List<String> getAllSummaries()
    {
        // date를 기준으로 오름차순 정렬 후 제공
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT summary FROM " + TABLE_NAME + " ORDER BY date", null);

        List<String> summaries = new ArrayList<>();
        while (cursor.moveToNext())
            summaries.add(cursor.getString(0)); // cursor의 summary 값

        cursor.close();
        return summaries;
    }
}