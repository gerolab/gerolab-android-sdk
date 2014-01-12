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

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

import com.getgero.motionsdk.storage.StepsDbManager;
import com.getgero.motionsdk.storage.StepsProvider;
import com.getgero.motionsdk.storage.TableSteps;

import java.text.Format;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Activity demonstrates how to calculate sleep periods
 */
public class SleepLogActivity extends FragmentActivity {

    public static final int DEFAULT_SLEEP_DURATION = 30;

    private ListView mListView;

    // UI views
    private View mLoadingView;
    private EditText mDurationEditText;
    private CheckBox mTimezoneCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_sleep);
        mLoadingView = findViewById(R.id.loading);
        mListView = (ListView) findViewById(R.id.list);
        mLoadingView.setVisibility(View.VISIBLE);
        mTimezoneCheckBox = (CheckBox) findViewById(R.id.night);
        mDurationEditText = (EditText) findViewById(R.id.edit_duration);
        mDurationEditText.setText(String.valueOf(DEFAULT_SLEEP_DURATION));
        findViewById(R.id.btn_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterSleepTimes();
            }
        });
        filterSleepTimes();
    }

    private void filterSleepTimes() {
        String duration = mDurationEditText.getText().toString();
        int durationInt = DEFAULT_SLEEP_DURATION;
        if (!TextUtils.isEmpty(duration)) {
            durationInt = Integer.parseInt(duration);
        }
        new TotalSleepLoadTask(durationInt, mTimezoneCheckBox.isChecked()).execute();
    }

    private class TotalSleepLoadTask extends AsyncTask<Void, Void, ArrayList<String>> {

        private int mMinTime;
        private boolean mIsUseTimezone;

        public TotalSleepLoadTask(int minTime, boolean isUseTimezone) {
            mMinTime = minTime;
            mIsUseTimezone = isUseTimezone;
        }

        @Override
        protected void onPreExecute() {
            mLoadingView.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            Cursor c = null;
            ArrayList<String> result = new ArrayList<String>();
            try {
                c = getContentResolver().query(StepsProvider.CONTENT_URI, new String[]{
                        TableSteps.COLUMN_ID, TableSteps.COLUMN_STEPS, TableSteps.COLUMN_CALORIES, TableSteps.COLUMN_DISTANCE, TableSteps.COLUMN_EVENTS_COUNT
                }, null, null, null);
                if (c != null && c.moveToFirst()) {
                    int time = 0;
                    do {
                        int count = StepsDbManager.getEventsCount(c);
                        if (count == 0) {
                            time++;
                        } else if (time >= mMinTime) {
                            String data = getFormattedString(mIsUseTimezone, time, c);
                            if (data != null) {
                                result.add(data);
                            }
                            time = 0;
                        } else {
                            time = 0;
                        }
                    } while (c.moveToNext());
                    if (time >= mMinTime) {
                        c.moveToLast();
                        String data = getFormattedString(mIsUseTimezone, time, c);
                        if (data != null) {
                            result.add(data);
                        }
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            return result;
        }

        private boolean isTimeInNightInterval(long startTime, long endTime) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(startTime);
            int startHour = cal.get(Calendar.HOUR_OF_DAY);
            cal.setTimeInMillis(endTime);
            int endHour = cal.get(Calendar.HOUR_OF_DAY);
            boolean isStartIsNight = startHour <= 6 || startHour >= 22;
            boolean isEndIsNight = endHour <= 6 || endHour >= 22;
            return isStartIsNight || isEndIsNight;
        }

        private String getFormattedString(boolean isUseTimezone, long time, Cursor c) {
            String textFormat = getApplicationContext().getString(R.string.label_sleep_log);
            Format dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
            Format timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
            long endTime = StepsDbManager.getTime(c) * 1000;
            long startTime = endTime - time * 60 * 1000;
            if (isUseTimezone && !isTimeInNightInterval(startTime, endTime)) {
                return null;
            }
            return String.format(textFormat, dateFormat.format(startTime) + ' ' + timeFormat.format(startTime) + '-' + timeFormat.format(endTime), time);
        }

        @Override
        protected void onPostExecute(ArrayList<String> data) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(SleepLogActivity.this, android.R.layout.simple_list_item_1, data);
            mListView.setAdapter(adapter);
            mLoadingView.setVisibility(View.GONE);
        }
    }

}
