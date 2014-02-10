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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.getgero.common.serverapi.data.ProfileInfo;
import com.getgero.motionsdk.service.GeroAccelerometerService;
import com.getgero.motionsdk.service.GeroAccelerometerServiceUtils;
import com.getgero.motionsdk.service.IGeroAccelerometerService;
import com.getgero.motionsdk.service.IGeroAccelerometerServiceCallback;
import com.getgero.motionsdk.storage.StepsDbManager;
import com.getgero.motionsdk.utils.StepUtils;
import com.google.android.gms.location.DetectedActivity;

/**
 * This is simple example how to use GeroLab SDK
 * <p/>
 * Main activity class
 * - login
 * - registration
 * - restore password
 * - view information
 */
@SuppressWarnings("ALL")
public class LoginActivity extends Activity {

    private static final String LOG_TAG = "[ LoginActivity ]";

    private static float[] SENSITIVITY_ARRAY = {
            StepUtils.SENSITIVITY_1,
            StepUtils.SENSITIVITY_2,
            StepUtils.SENSITIVITY_3,
            StepUtils.SENSITIVITY_4,
            StepUtils.SENSITIVITY_5,
            StepUtils.SENSITIVITY_6,
            StepUtils.SENSITIVITY_7,
            StepUtils.SENSITIVITY_8,
            StepUtils.SENSITIVITY_9
    };

    private static int[] RECORD_TIME_ARRAY = {
            0,
            1,
            3,
            6,
            12
    };

    // async tasks
    private ServerTask mServerTask = null;

    // loading views
    private View mLoadingView;
    private TextView mLoadingMessageView;

    private View mScrollView;

    // Login UI
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mLoginLayout;

    // Restore password UI
    private EditText mEmailRestoreView;
    private View mRestoreLayout;

    // Register UI
    private EditText mEmailRegisterView;
    private EditText mPasswordRegisterView;
    private EditText mNameRegisterView;
    private EditText mBirthdayRegisterView;
    private Spinner mGenderSpinner;
    private EditText mWeightRegisterView;
    private EditText mHeightRegisterView;
    private View mRegisterLayout;

    // Main screen UI (after success login)
    private CheckBox mRecordOnlyWalkingCheckBox;
    private Spinner mSensitivitySpinner;
    private Spinner mRecordingTimeSpinner;
    private View mMainLayout;

    // base status fields UI
    private TextView mTextSteps;
    private TextView mTextStatus;
    private TextView mTextUserInfo;

    // local steps counter
    // steps from last activity startup
    private int mStepsCount;

    // service connection to GeroLab accelerometer service
    private GeroAccelerometerServiceConnection mConnection;
    private IGeroAccelerometerService mGeroAccelerometerService;

    // accelerometer service state
    private int mServiceState;
    // Google activity type
    private int mActivityType = DetectedActivity.UNKNOWN;

    // spinner listener to change step detection sensitivity
    private AdapterView.OnItemSelectedListener mOnSensitivityItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            try {
                mGeroAccelerometerService.setStepSensitivity(SENSITIVITY_ARRAY[position]);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private AdapterView.OnItemSelectedListener mOnRecordTimeItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            try {
                mGeroAccelerometerService.setMaximumRecordTime(RECORD_TIME_ARRAY[position]);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    // accelerometer service callback
    private final IGeroAccelerometerServiceCallback mCallback = new IGeroAccelerometerServiceCallback.Stub() {
        @Override
        public void onServiceStateChanged(int state) throws RemoteException {
            Log.d(LOG_TAG, "onServiceStateChanged(): state = " + state);
            mServiceState = state;
            updateServiceStatusUI();
        }

        @Override
        public void onActivityChanged(int activity) throws RemoteException {
            mActivityType = activity;
            updateServiceStatusUI();
        }

        @Override
        public void onStepDetected() throws RemoteException {
            Log.d(LOG_TAG, "onStepDetected()");
            mStepsCount++;
            mTextSteps.post(new Runnable() {
                @Override
                public void run() {
                    mTextSteps.setText(String.format(getString(R.string.text_steps), mStepsCount));
                }
            });
        }
    };

    // user-readable accelerometer service states
    private String getServiceState(int state) {
        switch (state) {
            case GeroAccelerometerService.STATE_SERVICE_STARTED:
                return "STARTED";
            case GeroAccelerometerService.STATE_SERVICE_PAUSED_LOW_BATTERY:
                return "PAUSED_LOW_BATTERY";
            case GeroAccelerometerService.STATE_SERVICE_PAUSED_AUTH_ERROR:
                return "PAUSED_AUTH_ERROR";
            case GeroAccelerometerService.STATE_SERVICE_PAUSED_NOT_WALKING:
                return "PAUSED_NOT_WALKING";
            default:
            case GeroAccelerometerService.STATE_SERVICE_STOPPED:
                return "STOPPED";
        }
    }

    // user-readable Google activity type names
    private String getActivityName(int activityType) {
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "IN_VEHICLE";
            case DetectedActivity.ON_BICYCLE:
                return "ON_BICYCLE";
            case DetectedActivity.ON_FOOT:
                return "ON_FOOT";
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.TILTING:
                return "TILTING";
            default:
            case DetectedActivity.UNKNOWN:
                return "UNKNOWN";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mScrollView = findViewById(R.id.scroll_view);
        mLoadingView = findViewById(R.id.loading_status);
        mLoadingMessageView = (TextView) findViewById(R.id.loading_status_message);

        // info UI
        mTextSteps = (TextView) findViewById(R.id.text_steps);
        mTextStatus = (TextView) findViewById(R.id.text_status);
        mTextUserInfo = (TextView) findViewById(R.id.text_user_info);

        // Login UI
        mEmailView = (EditText) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        mLoginLayout = findViewById(R.id.login_layout);
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        // Restore UI
        mRestoreLayout = findViewById(R.id.restore_layout);
        mEmailRestoreView = (EditText) findViewById(R.id.email_restore);
        mEmailRestoreView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.restore || id == EditorInfo.IME_NULL) {
                    attemptRestore();
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.restore_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRestore();
            }
        });

        // Register UI
        mEmailRegisterView = (EditText) findViewById(R.id.email_register);
        mPasswordRegisterView = (EditText) findViewById(R.id.password_register);
        mNameRegisterView = (EditText) findViewById(R.id.name_register);
        mBirthdayRegisterView = (EditText) findViewById(R.id.birthday);
        mWeightRegisterView = (EditText) findViewById(R.id.weight);
        mHeightRegisterView = (EditText) findViewById(R.id.height);

        mRegisterLayout = findViewById(R.id.register_layout);
        mGenderSpinner = (Spinner) findViewById(R.id.gender);
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mGenderSpinner.setAdapter(genderAdapter);

        // Main UI (after login)
        mMainLayout = findViewById(R.id.main_layout);
        findViewById(R.id.logout_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // clears credential information
                new LogoutTask().execute();
            }
        });
        mRecordOnlyWalkingCheckBox = (CheckBox) findViewById(R.id.walking_only_checkbox);
        mRecordOnlyWalkingCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mRecordingTimeSpinner.setEnabled(isChecked);
                try {
                    mGeroAccelerometerService.setAccelerometerCaptureMode(isChecked ? GeroAccelerometerService.MODE_ACCELEROMETER_WALKING_ONLY : GeroAccelerometerService.MODE_ACCELEROMETER_CONSTANT);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        mRecordingTimeSpinner = (Spinner) findViewById(R.id.recording_time);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this,
                R.array.recording_time, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRecordingTimeSpinner.setAdapter(adapter1);

        mSensitivitySpinner = (Spinner) findViewById(R.id.sensitivity);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.sensitivity, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSensitivitySpinner.setAdapter(adapter2);

        findViewById(R.id.log_step_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), StepsLogActivity.class));
            }
        });
        findViewById(R.id.log_sleep_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SleepLogActivity.class));
            }
        });
        mHeightRegisterView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.register || id == EditorInfo.IME_NULL) {
                    attemptRegister();
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.register_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        // connect to the service via AIDL
        mConnection = new GeroAccelerometerServiceConnection();
        bindService(new Intent(this, GeroAccelerometerService.class), mConnection, Context.BIND_AUTO_CREATE);

        // set initial steps count
        mTextSteps.setText(String.format(getString(R.string.text_steps), mStepsCount));

        // GeroAccelerometerServiceUtils.hasCredentials methods are synchronous - move off UI thread
        new StartInitTask().execute();
    }

    @Override
    protected void onDestroy() {
        if (mConnection != null) {
            unbindService(mConnection);
            mConnection = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == R.id.action_start_service) {
            GeroAccelerometerServiceUtils.startDataCapture(getApplicationContext());
            return true;
        } else if (item.getItemId() == R.id.action_stop_service) {
            GeroAccelerometerServiceUtils.stopDataCapture(getApplicationContext());
            return true;
        } else {
            return super.onMenuItemSelected(featureId, item);
        }
    }

    /**
     * Login checks
     */
    public void attemptLogin() {
        if (mServerTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (password.length() < GeroAccelerometerServiceUtils.PASSWORD_LENGTH_MIN || password.length() > GeroAccelerometerServiceUtils.PASSWORD_LENGTH_MAX) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!GeroAccelerometerServiceUtils.isValidEmail(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoadingMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            mServerTask = new ServerTask(ServerTask.TYPE_LOGIN);
            mServerTask.execute((Void) null);
        }
    }

    /**
     * Restore password checks
     */
    public void attemptRestore() {
        if (mServerTask != null) {
            return;
        }

        // Reset errors.
        mEmailRestoreView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailRestoreView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailRestoreView.setError(getString(R.string.error_field_required));
            focusView = mEmailRestoreView;
            cancel = true;
        } else if (!GeroAccelerometerServiceUtils.isValidEmail(email)) {
            mEmailRestoreView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailRestoreView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoadingMessageView.setText(R.string.login_progress_restore);
            showProgress(true);
            mServerTask = new ServerTask(ServerTask.TYPE_RESTORE);
            mServerTask.execute((Void) null);
        }
    }

    /**
     * Register checks
     */
    public void attemptRegister() {
        if (mServerTask != null) {
            return;
        }

        // Reset errors.
        mEmailRegisterView.setError(null);
        mPasswordRegisterView.setError(null);
        mNameRegisterView.setError(null);
        mBirthdayRegisterView.setError(null);
        mWeightRegisterView.setError(null);
        mHeightRegisterView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailRegisterView.getText().toString();
        String password = mPasswordRegisterView.getText().toString();
        String name = mNameRegisterView.getText().toString();
        String birthday = mBirthdayRegisterView.getText().toString();
        String weight = mWeightRegisterView.getText().toString();
        String height = mHeightRegisterView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(password)) {
            mPasswordRegisterView.setError(getString(R.string.error_field_required));
            focusView = mPasswordRegisterView;
            cancel = true;
        } else if (password.length() < GeroAccelerometerServiceUtils.PASSWORD_LENGTH_MIN || password.length() > GeroAccelerometerServiceUtils.PASSWORD_LENGTH_MAX) {
            mPasswordRegisterView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordRegisterView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailRegisterView.setError(getString(R.string.error_field_required));
            focusView = mEmailRegisterView;
            cancel = true;
        } else if (!GeroAccelerometerServiceUtils.isValidEmail(email)) {
            mEmailRegisterView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailRegisterView;
            cancel = true;
        }

        if (TextUtils.isEmpty(name)) {
            mNameRegisterView.setError(getString(R.string.error_field_required));
            focusView = mNameRegisterView;
            cancel = true;
        }

        if (TextUtils.isEmpty(birthday)) {
            mBirthdayRegisterView.setError(getString(R.string.error_field_required));
            focusView = mBirthdayRegisterView;
            cancel = true;
        }

        if (TextUtils.isEmpty(weight)) {
            mWeightRegisterView.setError(getString(R.string.error_field_required));
            focusView = mWeightRegisterView;
            cancel = true;
        }

        if (TextUtils.isEmpty(height)) {
            mHeightRegisterView.setError(getString(R.string.error_field_required));
            focusView = mHeightRegisterView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoadingMessageView.setText(R.string.login_progress_register);
            showProgress(true);
            mServerTask = new ServerTask(ServerTask.TYPE_REGISTER);
            mServerTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoadingView.setVisibility(View.VISIBLE);
            mLoadingView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoadingView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mScrollView.setVisibility(View.VISIBLE);
            mScrollView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mScrollView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoadingView.setVisibility(show ? View.VISIBLE : View.GONE);
            mScrollView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    // updates service status
    private void updateServiceStatusUI() {
        mTextStatus.post(new Runnable() {
            @Override
            public void run() {
                mTextStatus.setText("service state: " + getServiceState(mServiceState) + "\nactivity type: " + getActivityName(mActivityType));
            }
        });
    }

    // updates UI
    private void updateUI(final boolean isLogin) {
        mLoginLayout.setVisibility(isLogin ? View.VISIBLE : View.GONE);
        mRestoreLayout.setVisibility(isLogin ? View.VISIBLE : View.GONE);
        mRegisterLayout.setVisibility(isLogin ? View.VISIBLE : View.GONE);
        mMainLayout.setVisibility(isLogin ? View.GONE : View.VISIBLE);
        if (isLogin) {
            mTextUserInfo.setText("");
        } else {
            ProfileInfo info = GeroAccelerometerServiceUtils.getProfileInfo(getApplicationContext());
            if (info == null) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("email: ").append(info.getEmail()).append('\n');
            sb.append("name: ").append(info.getFirstName()).append('\n');
            sb.append("gender: ").append(info.getGender()).append('\n');
            sb.append("weight: ").append(info.getWeight() / 1000).append('\n');
            sb.append("height: ").append(info.getHeight());
            mTextUserInfo.setText(sb.toString());
        }
    }

    /**
     * Logout
     */
    public class LogoutTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            mLoadingMessageView.setText(R.string.login_progress_loading);
            mLoadingView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            GeroAccelerometerServiceUtils.logout(getApplicationContext());
            StepsDbManager.clearDatabase(getApplicationContext());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            updateUI(true);
            mLoadingView.setVisibility(View.GONE);
        }
    }

    /**
     * Start init task
     */
    public class StartInitTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            mLoadingMessageView.setText(R.string.login_progress_loading);
            mLoadingView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return GeroAccelerometerServiceUtils.hasCredentials(getApplicationContext());
        }

        @Override
        protected void onPostExecute(Boolean hasCredentials) {
            updateUI(!hasCredentials);
            mLoadingView.setVisibility(View.GONE);
        }
    }

    /**
     * Login task
     */
    public class ServerTask extends AsyncTask<Void, Void, Integer> {

        public static final int TYPE_LOGIN = 0;
        public static final int TYPE_RESTORE = 1;
        public static final int TYPE_REGISTER = 2;

        private int mType;
        private String mInnerSDKErrorMessage;

        public ServerTask(int type) {
            mType = type;
        }

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

        @Override
        protected void onPostExecute(final Integer result) {
            mServerTask = null;
            showProgress(false);

            if (result == GeroAccelerometerServiceUtils.RESPONSE_OK) {
                if (mType == TYPE_LOGIN || mType == TYPE_REGISTER) {
                    GeroAccelerometerServiceUtils.startDataCapture(getApplicationContext());
                    updateUI(false);
                } else {
                    mEmailRestoreView.setText("");
                    Toast.makeText(LoginActivity.this, "check your email!", Toast.LENGTH_LONG).show();
                }
            } else if (result == GeroAccelerometerServiceUtils.RESPONSE_INCORRECT_CREDENTIALS) {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            } else if (result == GeroAccelerometerServiceUtils.RESPONSE_EMAIL_IS_NOT_REGISTERED) {
                mEmailRestoreView.setError(getString(R.string.error_email_not_registered));
                mEmailRestoreView.requestFocus();
            } else if (result == GeroAccelerometerServiceUtils.RESPONSE_EMAIL_ALREADY_REGISTERED) {
                mEmailRegisterView.setError(getString(R.string.error_email_already_registered));
                mEmailRegisterView.requestFocus();
            } else if (result != -1) {
                Toast.makeText(LoginActivity.this, "error: " + result, Toast.LENGTH_SHORT).show();
            } else {
                if (mInnerSDKErrorMessage != null) {
                    Toast.makeText(LoginActivity.this, mInnerSDKErrorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mServerTask = null;
            showProgress(false);
        }
    }

    /**
     * Gero service connection
     */
    private class GeroAccelerometerServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(LOG_TAG, "onServiceConnected()");
            mGeroAccelerometerService = IGeroAccelerometerService.Stub.asInterface(service);
            try {
                mGeroAccelerometerService.registerCallback(mCallback);
                mServiceState = mGeroAccelerometerService.getServiceState();
                mActivityType = mGeroAccelerometerService.getLastActivityType();
                updateServiceStatusUI();
                boolean walkingOnly = mGeroAccelerometerService.getAccelerometerCaptureMode() == GeroAccelerometerService.MODE_ACCELEROMETER_WALKING_ONLY;
                mRecordOnlyWalkingCheckBox.setChecked(walkingOnly);

                mSensitivitySpinner.setOnItemSelectedListener(null);
                float sensitivity = mGeroAccelerometerService.getStepSensitivity();
                for (int i = 0; i < SENSITIVITY_ARRAY.length; i++) {
                    if (sensitivity == SENSITIVITY_ARRAY[i]) {
                        mSensitivitySpinner.setSelection(i);
                        break;
                    }
                }
                mSensitivitySpinner.setOnItemSelectedListener(mOnSensitivityItemSelectedListener);
                //
                mRecordingTimeSpinner.setOnItemSelectedListener(null);
                int time =  mGeroAccelerometerService.getMaximumRecordTime();
                for (int i = 0; i < RECORD_TIME_ARRAY.length; i++) {
                    if (time == RECORD_TIME_ARRAY[i]) {
                        mRecordingTimeSpinner.setSelection(i);
                        break;
                    }
                }
                mRecordingTimeSpinner.setOnItemSelectedListener(mOnRecordTimeItemSelectedListener);
                mRecordingTimeSpinner.setEnabled(walkingOnly);

                mGeroAccelerometerService.setMinimumSleepNotificationTime(SleepLogActivity.DEFAULT_SLEEP_DURATION);
            } catch (RemoteException e) {
                Log.d(LOG_TAG, "onServiceConnected() [RemoteException]");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(LOG_TAG, "onServiceDisconnected()");
            mGeroAccelerometerService = null;
        }
    }
}
