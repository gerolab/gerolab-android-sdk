gerolab-android-sdk
===================

Gero Lab
--
[<b>GERO Lab</b>](https://getgero.com) is a crowd-research platform that enables high frequency data collection from accelerometer in your mobile phone. Collected data will be used for GERO Lab project studies of indicators of slowly developing diseases in correlation with human locomotor activity.
Our goal is to create a technology for early stage diagnostics of age-related conditions through analysis of everyday activity. Our primary focus is on specific neurological, psychiatric and metabolic conditions, especially on Parkinson's, Alzheimer's disease, Depression, Hypertension and Diabetes type 2.

We're maintaining a research to be able to identify early stages of these diseases and estimate related health conditions trends, which would potentially be allowing for patients to evaluate their treatment efficiency. We are already able to approximate these parameters with a high level of accuracy and provide personal reports to our users.

In order to make our technology more stable and accurate we are looking for more participants in our research.

You can download [Gero Lab Android application](https://play.google.com/store/apps/details?id=com.getgero.gerolab)

Gero Lab Android SDK
--
You can integrate Gero Lab cloud SDK in your Android application to collect and upload accelerometer data.

SDK features:

* Login to Gero cloud/Register/Restore password/Update user info
* Calculate steps
* Calculate calories burned
* Calculate distance
* Collect raw accelerometer data and upload to Gero cloud to conduct research
* Sleep detection

How to integrate SDK
--
Read [gero-lab-android-sdk-manual.md](manual/gero-lab-android-sdk-manual.md)

Changes
--
**v.1.3**
* Added setHz() and getHz() methods for custom frequency setting

**v.1.2**

* Added "walking only" mode for power optimization
* Removed option to turn off Google Play Services
* Step detection optimizations

**v.1.1**

* Added sleep detection

**v.1.0**

* First version
