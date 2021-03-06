# Description

HeartAlarm is an Android app, written in Java, that can be used to monitor both heart rate and ECG. Currently based around the Polar H10 exercise monitor and the Polar SDK (GitHub repo [here](https://github.com/polarofficial/polar-ble-sdk)), it has been developed in such a manner to permit easy extension for compatability other ECG-capable devices. As it stands, the app currently features the ability to set audible alarms based upon high and low heart rate alarms with monitoring in the background via Android's foreground service feature, as well as the ability to record and display both heart rate and ECG through Android's builtin SQLite capabilities and the GraphView library (GitHub repo [here](https://github.com/jjoe64/GraphView)).

## Disclaimer

This app is not intended for any serious, real-world medical usage. Do not use it in this manner. Seriously, don't. It's a pet project, not a vetted medical tool or device. If you ignore this disclaimer and do so anyways, I am not liable or responsible in any way for your decision and any harm or injury that results. 

## License Information

For all code used and/or derived from the Polar SDK, the Polar license and copyright applies, and is included in this repo as required. Similarly, the license for GraphView applies to the code using that library. All other code is my own and falls under a standard MIT license. Additionally, the alarm sounds were derived from a BBC sound library and are not for commercial usage (nothing here is, actually...but best to be clear on that point).

## Design

Due to restrictions the Android OS places upon non-foreground apps, it was largely necessary to design the app around its foreground service to permit long-running operations such as constant monitoring for the alarms and recording without the screen being on or the app being in the foreground itself. As such, the MonitorService foreground service class is the core of the actual heavy-duty operations of the app. Favoring composition, it contains several different class objects within it that are responsible for various aspects requiring continuous, long-running operations, such as the HrSQLiteRecorder which writes heart rate time-series information to a specified SQLite table in a SPSC concurrency model, or the DataDisplay, which is responsible for coordinating the rendering of the data being passed through the MonitorService to the various GraphView displays in various activities when the app is in the foreground. 

To support future extension, the MonitorService does not expect to directly link with a PolarDataSource (although it does directly create such an object in its constructor) but instead two (or possibly one) objects implementing the ECGDataSource and HRDataSource interfaces (note PolarDataSource implements both). These interfaces, as their names imply, indicate the class is a source of ECG and/or HR time-series data, which is to be handled through the specified consumer callbacks which can be set as specified by the interfaces. Any devices which can interface with the Android OS in a similar manner to the Polar H10 does through the Polar SDK can be added simply by wrapping the device interface in a class that implements the proper DataSource interface(s) and then linking it into the MonitorService through a trivially implemented selection activity. 

## Future Plans

If I acquire another ECG-capable device that can interface with the Android OS in a similar manner to the Polar H10 + Polar SDK, I will add a proper selection activity to let users pick between devices as the source of HR + ECG signal. Additionally, as a future project I intend to implement some research papers on CNN's interpreting single-lead ECG signal; if that goes well, I will likely bring that into this project as a library to provide continuous background interpretation of the ECG, akin to the computer interpretation present in Holter monitor like devices. 