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

package com.getgero.motionsdk.service;

import com.getgero.motionsdk.service.IGeroAccelerometerServiceCallback;

/**
 * Gero Accelerometer service interface definition.
 */
interface IGeroAccelerometerService {
    /**
     * Returns server state.
     */
    int getServiceState();

    /**
     * Returns last detected Google activity type.
     */
    int getLastActivityType();

    /**
     * Returns total recording data time (unix timestamp).
     */
    long getTotalRecordTime();

    /**
     * Register callback interface.
     */
    void registerCallback(IGeroAccelerometerServiceCallback cb);

    /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(IGeroAccelerometerServiceCallback cb);

    /**
     * Returns step detection sensitivity value.
     * List of values is defined in {@link StepUtils}.
     */
    float getStepSensitivity();

    /**
     * Set step detection sensitivity value.
     * List of values is defined in {@link StepUtils}.
     */
    void setStepSensitivity(float value);

    /**
     * Returns minimum sleep notification time. Default is 30 minutes.
     */
    int getMinimumSleepNotificationTime();

    /**
     * Set minimum sleep notification time. Local broadcast {@link GeroAccelerometerService.ACTION_SIGNIFICANT_MOVEMENT_AFTER_SLEEP} will be sent.
     */
    void setMinimumSleepNotificationTime(int value);

    /**
     * Returns accelerometer capture mode. In "walking only" mode sleep is not detected.
     * {@link GeroAccelerometerService.MODE_ACCELEROMETER_CONSTANT} or {@link GeroAccelerometerService.MODE_ACCELEROMETER_WALKING_ONLY}.
     */
    int getAccelerometerCaptureMode();

    /**
     * Set accelerometer capture mode. In "walking only" mode sleep is not detected.
     * {@link GeroAccelerometerService.MODE_ACCELEROMETER_CONSTANT} or {@link GeroAccelerometerService.MODE_ACCELEROMETER_WALKING_ONLY}.
     */
    void setAccelerometerCaptureMode(int mode);

    /**
     * Returns maximum record time (in hours) for {@link GeroAccelerometerService.MODE_ACCELEROMETER_WALKING_ONLY} mode.
     */
    int getMaximumRecordTime();

    /**
     * Set maximum record time (in hours) for {@link GeroAccelerometerService.MODE_ACCELEROMETER_WALKING_ONLY} mode.
     */
    void setMaximumRecordTime(int hours);
}