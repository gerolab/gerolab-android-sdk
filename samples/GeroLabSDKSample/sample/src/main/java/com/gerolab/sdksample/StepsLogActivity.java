/**
 * Copyright 2014-present Gero One Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gerolab.sdksample;

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.getgero.motionsdk.storage.StepsProvider;
import com.getgero.motionsdk.storage.TableSteps;

/**
 * Activity demonstrates how to load steps data from database
 */
public class StepsLogActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID_STEPS = 0;

    // simple cursor adapter
    private SimpleCursorAdapter mAdapter;

    // UI views
    private View mLoadingView;
    private TextView mTotalTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        mLoadingView = findViewById(R.id.loading);
        mTotalTextView = (TextView) findViewById(R.id.text_total);
        ListView listView = (ListView) findViewById(R.id.list);
        mAdapter = new SimpleCursorAdapter(this,
                R.layout.simple_list_item,
                null,
                new String[]{TableSteps.COLUMN_ID, TableSteps.COLUMN_STEPS, TableSteps.COLUMN_CALORIES, TableSteps.COLUMN_DISTANCE},
                new int[]{R.id.text1, R.id.text2, R.id.text3, R.id.text4},
                0);
        listView.setAdapter(mAdapter);
        mLoadingView.setVisibility(View.VISIBLE);
        // init loader
        getSupportLoaderManager().initLoader(LOADER_ID_STEPS, null, this);
        // load complex data from DB
        new TotalStepsLoadTask().execute();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(getApplicationContext(), StepsProvider.CONTENT_URI, new String[]{
                TableSteps.COLUMN_ID, TableSteps.COLUMN_STEPS, TableSteps.COLUMN_CALORIES, TableSteps.COLUMN_DISTANCE
        }, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null) {
            mAdapter.swapCursor(cursor);
        }
        mLoadingView.setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    private class TotalStepsLoadTask extends AsyncTask<Void, Void, Void> {

        private int mTotalSteps;
        private int mTotalCal;
        private int mTotalDistance;

        @Override
        protected Void doInBackground(Void... params) {
            // example of raw query to database
            ContentProviderClient client = getContentResolver().acquireContentProviderClient(StepsProvider.AUTHORITY);
            SQLiteDatabase db = ((StepsProvider) client.getLocalContentProvider()).getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT SUM(" + TableSteps.COLUMN_CALORIES + "), SUM(" + TableSteps.COLUMN_STEPS +
                        "), SUM(" + TableSteps.COLUMN_DISTANCE + ") FROM " + TableSteps.TABLE_NAME, null);
                if (cursor != null && cursor.moveToFirst()) {
                    mTotalCal = cursor.getInt(0);
                    mTotalSteps = cursor.getInt(1);
                    mTotalDistance = cursor.getInt(2);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // calories information is stored in cal in database
            // 1000 cal = 1Kcal = 1 calorie
            mTotalTextView.setText(String.format(getApplicationContext().getString(R.string.label_log_total), mTotalSteps, ((float) mTotalCal) / 1000, mTotalDistance));
        }
    }

}
