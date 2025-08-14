
## Table of Contents <!-- omit from toc -->
- [Temi MQTT app](#temi-mqtt-app)
- [Android Studio Setup](#android-studio-setup)
- [Gradle Issues](#gradle-issues)
- [Temi Connection](#temi-connection)
  - [Temi ADB open port](#1-using-temi-tablet-navigate-to-settings---development-tools-adb-connection-to-temi)
  - [ADB connection](#2-connect-to-temi-via-adb)
  - [Checking Android Studio connection](#3-check-the-connection-to-android-studio)
- [Software Architecture](#software-architecture)
  - [MainActivity Script](#1-mainactivity)
  - [RobotController Script](#2-robotcontroller)
  - [UiManager Script](#3-uimanager)
  - [Tips/MQTT Explorer](#tips)
- [Age and Gender Estimation Feature](#age-and-gender-estimation-feature)


# Temi MQTT app

This is the main directory for using the MQTT application with the Temi robot. The app connects to an MQTT broker, allowing you to communicate with Temi through topics. You can use these topics to send commands and make the robot perform actions like moving, speaking, or playing media.

What It Does:

- Connects to MQTT: Links Temi to an MQTT broker for real-time messaging.
- Uses Topics: Sends and receives messages via specific topics to control the robot vid sdk functions ad methods.
- Performs Actions: Executes commands like navigation, interaction, or other tasks through topic-based communication.


MQTT is used for communication between Temi and Node-Red.

![grafik](/uploads/553f05de38d28d93884883a9e6f86184/grafik.png)

You can create simple nodes that trigger your actions on the Temi or give the user parameters that they can change with your own custom node.

> **Note: IMPORTANT: If you are going to make any changes to the Temi-MQTT app code, you should read the Temi SDK documentation..**

https://github.com/robotemi/sdk?tab=readme-ov-file

## Android Studio Setup

Since Temi is an Android-based robot, the first step is to [install Android Studio](https://developer.android.com/studio) on your system. The minimum required version is Meerkat Feature Drop.

### 1. Clone the Repository:
Clone this directory to your local machine using Git or your preferred method.

```sh
  git clone -b Master2.0 https://gitlab-fi.ostfalia.de/hcr-lab/robot-control/middleware/node-red-setup-for-temi-robot.git
```

### 2. Open the Temi-App Folder:
Navigate to the Temi-App folder, which contains the app pre-installed on Temi. Open this folder in Android Studio.

*Automatic Configuration*: Once the Temi-App folder is open, some configurations and downloads will begin automatically. These processes are managed by Gradle, Android's build tool, which will set up the project structure for you.

After these steps, you’ll be ready to build and deploy the application!

![Android directory](/images/app_structure.png)

### Gradle Issues

*IMPORTANT:* You may encounter issues with different Gradle versions, especially if the project’s Gradle version doesn’t match your Android Studio setup.

- Check the Logs: Review the logs in Android Studio for error details.
- Use the AGP Upgrade Assistant: If needed, you can use the Gradle Assistant tool to resolve version conflicts. Go to: Menu -> Tools -> AGP Upgrade Assistant

With these tools, you can troubleshoot and fix Gradle version issues quickly.

After resolving any issues, you’ll be ready to build and deploy the application! 

If you encounter a problem with the gradle version, you need to change it in the build.gradle file.

![Gradle Version Error](/images/gradle_error.png)

![Version 8.6.1](/images/version_config.png)


## Temi Connection

Enable ADB Connection on Temi:

#### 1. Using Temi tablet, navigate to: Settings -> Development Tools ADB Connection to Temi 

Then, Click Open Port to enable ADB connections.

> **Note:  Always ensure the port is open before trying to connect Android Studio to Temi..**

![ADB Port Connection](/images/open_port.png)

#### 2. Connect to Temi via ADB:

On your machine (Windows Users), Open the Windows Command Prompt (cmd) and run the following commands:

> **Note:  Replace xxx with your Windows username. This sets up the Android SDK path on your system...**

```sh
set PATH=C:\Users\xxx\Appdata\Local\Android\Sdk\platform-tools;%PATH%
```

Then, type the adb connect command:

> **Note:  Replace xxx.xx.xx.xx with Temi’s IP address.**


```sh
adb connect xxx.xx.xx.xx  
```
![ADB connected](/images/adb_connected.png)

### 3. Check the connection to Android Studio:

After connecting via adb, Temi should appear in your android studio as a different name than before. This means that you are able to run/install the application on Temi.

![Temi connected](/images/temi_connected.png)


### 4. After connecting to Temi, you can install the application by clicking Run.


> **Note:  In case you have some problems to install the new MQTT app, you have to delete the old version using cmd prompt...**

```sh
adb uninstall com.example.mqtt 
```
After it, try to install (Android run) again. 



![Run app](/images/android_run.png)

# Software Architecture

The MQTT application is developed in Java/Kotlin and is structured around three main scripts that govern the robot’s operation:

![MQTT App Folders](/images/mqtt_app_folders.png)

### 1. MainActivity

The *MainActivity* script acts as the core of the application, managing:

- Connection to the MQTT Broker: Handles subscribing to and publishing messages on topics.
- Orchestration of Actions: Based on the received MQTT messages, it determines whether to:
- Trigger robot functions through the RobotController (e.g., moving, talking, listening).
- Manage screen interactions via the UIManager (e.g., displaying text, videos, or images).

*How it Works:*

When the MainActivity subscribes to an MQTT topic and receives a message, it triggers the corresponding action.

For example, when the "Goto" topic is received, MainActivity directs the RobotController to move the robot to the specified location.

![goTo example](/images/goto_topic.png)

### 2. RobotController:

The RobotController script is responsible for controlling Temi’s physical actuators. This includes:

- Navigation (moving to a location).
- Speech (talking).
- Wait for Keyword (listening).


Using the GoTo example: when the MainActivity script calls a function in the RobotController, it triggers the execution of that action.

The RobotController also communicates with MQTT by publishing the status of each action (e.g., going, waiting, done). 

This status tracking is crucial for creating a sequential workflow for the robot’s actions, ensuring that each action begins only after the previous one is marked as done.

> **Note:  These status updates are particularly important when using Temi with Node-RED, as Node-RED relies on sequential execution. A node will only proceed once its predecessor is complete, using the action status as a signal to move forward.**

![Goto Function](/images/goto_function.png)


### 3. UIManager:

The UIManager script manages the robot’s user interface (UI). This includes:

- Displaying or clearing text.
- Playing videos or showing images on the screen.
- It focuses exclusively on interactions that involve Temi’s display.


# First steps:

## MQTT Explorer:

To test the connection with MQTT (e.g., Node-RED → MQTT or MQTT → Temi), one effective method is to use the MQTT Explorer application. This tool allows you to monitor and verify if messages are being published correctly to the intended topics.

### 1. Install MQTT Explorer:

First install Mqtt Explorer on your computer (https://mqtt-explorer.com/), then connect to a server as shown below.

![mqtt_explorer_setup](/images/mqtt_setup.png)

### 2. Run some tests:

#### Test 1: Checking that the Mqtt application on Temi is working normally.

First start the Mqtt application on Temi: 

![temi_app](/images/temi_app.png)

After the start screen appears, open the MQTT Explorer and publish a message on the topic you want to use (topics can be seen inside the MainActivity - android studio script).

As an example, test the text-to-speech function:

In the topic field, enter: ```temi/tts ```

Then, in the message field, enter the code below and press publish. 

```sh
{
  "text": "Hello Temi",
  "language": "english",
  "animation": "true",
  "id": "test"
}
```

![publish_mqtt_topic](/images/publish_mqtt_topic.png)


After that, Temi will say “Hello Temi”. If it doesn't:
 1. Check the volume;
 2. Check that the mqtt application is open;
 3. Restart the robot.

#### Test 2: Checking that Node-RED is sending messages correctly to the topic in mqtt.

![nodeRED_mqtt](/images/nodeRED_mqtt.png)

#### Test 3: Checking that the display buttons work correctly with Node-RED.

![temi_layout](/images/temi_layout.png)

If you look at the start screen you can see two flag buttons. By pressing one of them you select the language that Temi should use as well as start the Face and Age detection. After a person is identified, the application publishes the chosen language and identified age to MQTT. 

The layout then changes to an empty screen with a home button in the bottom left corner. By pressing the home button you stop a running flow and get back to the start screen.

![mqtt_node_setup](/images/mqtt_node_setup.png)

To use the published language as a start signal you can add a 'mqtt in node' and a follow up node in a flow. Configure the mqtt node as in the picture above with a broker and a topic it should subscribe to.

If it shows the green connected sign, the flow can be automatically activated by pressing on of the language buttons.

# Age and Gender Estimation Feature

This section details the implementation of real-time age and gender estimation using the device's camera, with results published via MQTT.

## Overview

The application has been enhanced to detect faces in the camera feed and estimate the age range and gender of individuals. This processing happens directly on the device, ensuring low latency and offline capability. The results are then broadcasted via MQTT, allowing other systems to subscribe and react to these insights.

## How it Works

1.  **Camera Feed Processing**: The app continuously captures camera frames.
2.  **Face Detection**: Google ML Kit is used to efficiently detect faces within these frames.
3.  **Age & Gender Estimation**: For each detected face, a specialized TensorFlow Lite model (`face_model_v5.tflite`) processes the cropped face image to predict the age range and gender.
4.  **MQTT Publishing**: The estimated age and gender are published to specific MQTT topics.

## Key Components & Files

*   `AgeGenderEstimator.kt`: This new Kotlin class encapsulates the TensorFlow Lite model loading, image preprocessing, and inference logic for age and gender estimation.
*   `MainActivity.kt`: Modified to integrate ML Kit for face detection, manage camera frames, and call the `AgeGenderEstimator` for processing. It also handles the MQTT publishing of the results.
*   `LuminosityAnalyzer.kt`: Adapted to efficiently pass camera `ImageProxy` objects directly to `MainActivity` for ML Kit processing.
*   `build.gradle.kts`: Updated to include necessary dependencies for ML Kit and TensorFlow Lite.
*   `assets/face_model_v5.tflite`: The TensorFlow Lite model file used for inference.
*   `assets/labelmap.txt`: Contains the labels for age ranges and gender categories used by the model.

## MQTT Topics

The age and gender estimation results are published to the following MQTT topics:

*   `temi/age_estimation`: Publishes the estimated age range (e.g., "0-14yo", "15-40yo", "41-60yo", "61-100yo").
*   `temi/gender_estimation`: Publishes the estimated gender (e.g., "Female", "Male").

## Usage & Testing

To test this feature, ensure the app is running on your Temi device with camera access. You can subscribe to the MQTT topics `temi/age_estimation` and `temi/gender_estimation` using any MQTT client to receive real-time updates.
