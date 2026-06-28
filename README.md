This project demonstrates real-time video streaming from an Android device to a desktop system using socket programming. The Android application captures live camera frames and sends them over a network to a Python server, which displays the video using OpenCV.
The Android app captures live video frames using the CameraX API.
Frames are converted into JPEG format.
Data is sent over a TCP socket connection to a Python server.
The Python server receives the data.
Frames are decoded using OpenCV.
The live video is displayed on the desktop screen.
