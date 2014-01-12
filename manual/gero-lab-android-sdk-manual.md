Gero Lab Android SDK manual
==
SDK location
--
[https://github.com/gerolab/gerolab-android-sdk](https://github.com/gerolab/gerolab-android-sdk)
SDK integration
--
This is an example of creating sample project using [Android Studio](http://developer.android.com/sdk/installing/studio.html). The complete sample is located here [GeroLabSDKSample](https://github.com/gerolab/gerolab-android-sdk/tree/master/samples/GeroLabSDKSample).

* Create new Android Studio project project.

![001.png](/img/000.png)

* Copy [GeroLab-X.X.jar](https://github.com/gerolab/gerolab-android-sdk/tree/master/libs) to the /libs forder. /aidl files to the /src/main/aidl folder.

![002.png](/img/001.png)

* Modify build.gradle to compile external .jar and Google play services

```
    dependencies {
        compile 'com.android.support:appcompat-v7:+'
        compile 'com.google.android.gms:play-services:4.0.+'
        compile files('libs/GeroSDK-1.0.jar')
    }
```

* Modify AndroidManifest.xml
 * Add required permissions

```xml
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION"/>
```
    INTERNET - to communicate Gero Lab cloud
    RECEIVE_BOOT_COMPLETED - to start accelerometer server at phone startup
    WAKE_LOCK - to collet data in phone idle mode
    WRITE_EXTERNAL_STORAGE - to save binary files to SD partition
    ACTIVITY_RECOGNITION - use Google Play services for activity detection
    
* Add Google Play services `<meta-data>` to `<application>`

```xml
    <meta-data
        android:name="com.google.android.gms.version"
        android:value="@integer/google_play_services_version"/>
```

* Add Gero Lab service, provider and receiver definitions

```xml
    <service
        android:name="com.getgero.motionsdk.service.GeroAccelerometerService"
        android:exported="false"/>
    <provider
        android:name="com.getgero.motionsdk.storage.StepsProvider"
        android:authorities="com.getgero.motionsdk.provider.Steps"
        android:exported="false"/>
    <receiver
        android:name="com.getgero.motionsdk.receiver.BootReceiver"
        android:exported="false">
        <intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED"/>
        </intent-filter>
        <intent-filter>
            <action android:name="android.intent.action.PACKAGE_REPLACED"/>
                <data
                    android:scheme="package"/>
        </intent-filter>
    </receiver>
```

Now you are ready to write some code.
---
* Use AIDL interface to connect to the accelerometer service https://github.com/gerolab/gerolab-android-sdk/blob/master/libs/aidl/com/getgero/motionsdk/service/IGeroAccelerometerService.aidl
* Register AIDL callback to receive service updates https://github.com/gerolab/gerolab-android-sdk/blob/master/libs/aidl/com/getgero/motionsdk/service/IGeroAccelerometerServiceCallback.aidl
* Use static methods in GeroAccelerometerServiceUtils class to:
 * Login to Gero cloud
 * Register to Gero cloud
 * Restore Gero cloud password
 * Update Gero cloud profile info
 * Get profile info
 * Check if service already has credentials
 * Start accelerometer data collection
 * Stop accelerometer data collection
 * Logout

All methods are synchronous - please run them off UI thread.

```java
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                if (mType == TYPE_LOGIN) {
                    return GeroAccelerometerServiceUtils.login(getApplicationContext(), mEmailView.getText().toString(), mPasswordView.getText().toString());
                } else if (mType == TYPE_RESTORE) {
                    return GeroAccelerometerServiceUtils.restorePassword(getApplicationContext(), mEmailRestoreView.getText().toString());
                } else if (mType == TYPE_REGISTER) {
                    return GeroAccelerometerServiceUtils.register(getApplicationContext(), mEmailRegisterView.getText().toString(),
                            mPasswordRegisterView.getText().toString(),
                            mNameRegisterView.getText().toString(),
                            mGenderSpinner.getSelectedItemPosition() == 0,
                            mBirthdayRegisterView.getText().toString(),
                            Integer.valueOf(mWeightRegisterView.getText().toString()),
                            Integer.valueOf(mHeightRegisterView.getText().toString()));

                }
            } catch (IllegalArgumentException e) {
                mInnerSDKErrorMessage = e.getMessage();
            }
            return -1;
        }
```

* You can receive steps/calories/distance history from database via StepsProvider.
 * Steps are calculated via internal algorithm. You can improve walking probability detection by using Google Play activity recognition service. Use void setStepDetectionMode(int mode) method in AIDL interface to set mode.
 * To query database - use cursor loader or more complex rawQuery.

```java
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(getApplicationContext(), StepsProvider.CONTENT_URI, new String[]{
                TableSteps.COLUMN_ID, TableSteps.COLUMN_STEPS, TableSteps.COLUMN_CALORIES, TableSteps.COLUMN_DISTANCE
        }, null, null, null);
    }
```

```java
        @Override
        protected Void doInBackground(Void... params) {
            // example of raw query to database
            ContentProviderClient client = getContentResolver().acquireContentProviderClient(StepsProvider.AUTHORITY);
            SQLiteDatabase db = ((StepsProvider) client.getLocalContentProvider()).getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT SUM(" + TableSteps.COLUMN_CALORIES + "), SUM(" + TableSteps.COLUMN_STEPS +
                    "), SUM(" + TableSteps.COLUMN_DISTANCE + ") FROM " + TableSteps.TABLE_NAME, null);
            if (cursor != null && cursor.moveToFirst()) {
                mTotalCal = cursor.getInt(0);
                mTotalSteps = cursor.getInt(1);
                mTotalDistance = cursor.getInt(2);
            }
            return null;
        }
```

* [Download sample apk here](https://github.com/gerolab/gerolab-android-sdk/raw/master/manual/GeroLabSDKSample.apk)

![004.png](/img/004.png)![005.png](/img/005.png)
 