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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.getgero.motionsdk.service.GeroAccelerometerService;

/**
 * Receives com.getgero.motionsdk.service.ACTION_SIGNIFICANT_MOVEMENT_AFTER_SLEEP event.
 */
public class SleepStateReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        long startTime = intent.getLongExtra(GeroAccelerometerService.EXTRA_SLEEP_START_TIME, 0);
        long endTime = intent.getLongExtra(GeroAccelerometerService.EXTRA_SLEEP_END_TIME, 0);
        int deltaMinutes = (int) ((endTime - startTime) / 60);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(android.R.drawable.stat_notify_sync)
                        .setContentTitle(context.getString(R.string.label_sleep_notification))
                        .setContentText(String.format(context.getString(R.string.label_sleep_notification_content, deltaMinutes)));
        Intent resultIntent = new Intent(context, LoginActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setDefaults(Notification.DEFAULT_ALL);
        mBuilder.setAutoCancel(true);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
    }
}


