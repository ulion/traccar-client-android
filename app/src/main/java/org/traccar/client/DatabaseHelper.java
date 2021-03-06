/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.client;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "traccar.db";

    public interface DatabaseHandler<T> {
        void onComplete(boolean success, T result);
    }

    private static abstract class DatabaseAsyncTask<T> extends AsyncTask<Void, Void, T> {

        private DatabaseHandler<T> handler;
        private RuntimeException error;

        public DatabaseAsyncTask(DatabaseHandler<T> handler) {
            this.handler = handler;
        }

        @Override
        protected T doInBackground(Void... params) {
            try {
                return executeMethod();
            } catch (RuntimeException error) {
                this.error = error;
                return null;
            }
        }

        protected abstract T executeMethod();

        @Override
        protected void onPostExecute(T result) {
            handler.onComplete(error == null, result);
        }
    }

    private SQLiteDatabase db;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE position (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "deviceId TEXT," +
                "time INTEGER," +
                "latitude REAL," +
                "longitude REAL," +
                "horizontalAccuracy REAL," +
                "altitude REAL," +
                "speed REAL," +
                "course REAL," +
                "battery REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS position;");
        onCreate(db);
    }

    public void insertPosition(Position position) {
        ContentValues values = new ContentValues();
        values.put("deviceId", position.getDeviceId());
        values.put("time", position.getTime().getTime());
        values.put("latitude", position.getLatitude());
        values.put("longitude", position.getLongitude());
        values.put("horizontalAccuracy", position.getHorizontalAccuracy());
        values.put("altitude", position.getAltitude());
        values.put("speed", position.getSpeed());
        values.put("course", position.getCourse());
        values.put("battery", position.getBattery());

        db.insertOrThrow("position", null, values);
    }

    public void insertPositionAsync(final Position position, DatabaseHandler<Void> handler) {
        new DatabaseAsyncTask<Void>(handler) {
            @Override
            protected Void executeMethod() {
                insertPosition(position);
                return null;
            }
        }.execute();
    }

    public Position selectPosition() {
        Position position = new Position();

        Cursor cursor = db.rawQuery("SELECT * FROM position ORDER BY id DESC LIMIT 1", null);
        try {
            if (cursor.getCount() > 0) {

                cursor.moveToFirst();

                position.setId(cursor.getLong(cursor.getColumnIndex("id")));
                position.setDeviceId(cursor.getString(cursor.getColumnIndex("deviceId")));
                position.setTime(new Date(cursor.getLong(cursor.getColumnIndex("time"))));
                position.setLatitude(cursor.getDouble(cursor.getColumnIndex("latitude")));
                position.setLongitude(cursor.getDouble(cursor.getColumnIndex("longitude")));
                position.setHorizontalAccuracy(cursor.getDouble(cursor.getColumnIndex("horizontalAccuracy")));
                position.setAltitude(cursor.getDouble(cursor.getColumnIndex("altitude")));
                position.setSpeed(cursor.getDouble(cursor.getColumnIndex("speed")));
                position.setCourse(cursor.getDouble(cursor.getColumnIndex("course")));
                position.setBattery(cursor.getDouble(cursor.getColumnIndex("battery")));

            } else {
                return null;
            }
        } finally {
            cursor.close();
        }

        return position;
    }

    public List<Position> selectPositions(long fetchLimit) {
        Cursor cursor = db.rawQuery("SELECT * FROM position ORDER BY id DESC LIMIT " + fetchLimit, null);
        try {
            if (cursor.getCount() > 0) {
                List<Position> positions = new LinkedList<Position>();

                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    Position position = new Position();
                    position.setId(cursor.getLong(cursor.getColumnIndex("id")));
                    position.setDeviceId(cursor.getString(cursor.getColumnIndex("deviceId")));
                    position.setTime(new Date(cursor.getLong(cursor.getColumnIndex("time"))));
                    position.setLatitude(cursor.getDouble(cursor.getColumnIndex("latitude")));
                    position.setLongitude(cursor.getDouble(cursor.getColumnIndex("longitude")));
                    position.setHorizontalAccuracy(cursor.getDouble(cursor.getColumnIndex("horizontalAccuracy")));
                    position.setAltitude(cursor.getDouble(cursor.getColumnIndex("altitude")));
                    position.setSpeed(cursor.getDouble(cursor.getColumnIndex("speed")));
                    position.setCourse(cursor.getDouble(cursor.getColumnIndex("course")));
                    position.setBattery(cursor.getDouble(cursor.getColumnIndex("battery")));

                    positions.add(position);

                    cursor.moveToNext();
                }
                return positions;
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    public void selectPositionAsync(DatabaseHandler<Position> handler) {
        new DatabaseAsyncTask<Position>(handler) {
            @Override
            protected Position executeMethod() {
                return selectPosition();
            }
        }.execute();
    }

    public void selectPositionsAsync(final long fetchLimit, DatabaseHandler< List<Position> > handler) {
        new DatabaseAsyncTask< List<Position> >(handler) {
            @Override
            protected List<Position> executeMethod() {
                return selectPositions(fetchLimit);
            }
        }.execute();
    }

    public void deletePosition(long id) {
        if (db.delete("position", "id = ?", new String[] { String.valueOf(id) }) != 1) {
            throw new SQLException();
        }
    }

    public void deletePositions(List<Position> positions) {
        List<String> ids = new LinkedList<>();
        for (Position position: positions) {
            ids.add(String.valueOf(position.getId()));
        }
        if (db.delete("position", "id IN ("+ TextUtils.join(",", ids)+")", null) != 1) {
            throw new SQLException();
        }
    }

    public void deletePositionAsync(final long id, DatabaseHandler<Void> handler) {
        new DatabaseAsyncTask<Void>(handler) {
            @Override
            protected Void executeMethod() {
                deletePosition(id);
                return null;
            }
        }.execute();
    }

    public void deletePositionsAsync(final List<Position> positions, DatabaseHandler<Void> handler) {
        new DatabaseAsyncTask<Void>(handler) {
            @Override
            protected Void executeMethod() {
                deletePositions(positions);
                return null;
            }
        }.execute();
    }

}
