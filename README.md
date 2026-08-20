# Voxel Renderer

Voxel Renderer is an Android application developed in Java using OpenGL ES 3.0 for real-time rendering of voxel-based models.

The project focuses on efficient GPU rendering through instanced rendering, allowing large numbers of voxels to be rendered using a single cube mesh and per-instance data.

## Features
* OpenGL ES 3.0
* Instanced rendering
* .vly format parsing
* Real-time 3D rendering on Android

## Technologies
* Java
* Android
* OpenGL ES 3.0
* GLSL ES 3.0

## Getting Started

Clone the repository and open it with Android Studio.
``` bash
git clone https://github.com/Il-castor/VoxelRenderer.git
```

### Place voxel models inside:
```
app/src/main/assets/
```

Then build and run the application on an Android device or emulator with OpenGL ES 3.0 support.

## What is VLY Format?

A simple voxel serialization format similar to PLY but simpler, developed specifically as a challenge for this project.

You can find more precise requirements in [ProgettoCG2324.pdf.](https://github.com/Il-castor/VoxelRenderer/blob/main/ProgettoCG2324.pdf)

## Project

This project was developed as part of a Computer Graphics assignment and explores GPU-based techniques for efficient voxel rendering on mobile devices.
