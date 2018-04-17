Sample glTF renderer
====================

glTF is a new standard 3D transmission format by the Khronos Group. Facebook has recently made sharing 3D content in newsfeed as easy as sharing an image or a video using glTF 2.0.

This demo shows the mechanics of parsing and rendering a sample "Hello World!" glTF file. This code is reference material for the Android Makers Paris 2018 talk titled: *"The JPEG of 3D: Bringing 3D scenes and objects into your 2D Android app with glTF"*.

It provides a straightforward look at how to render a 3D scene using glTF and the OpenGL ES API. This is a good reference for developers who are learning about 3D rendering on Android. This is low level GPU driver API code; you may want to check out the new Sceneform API in the AR Core 1.2 SDK if you want a high-level API that can render glTF, OBJ, and FBX 3D scenes.

The renderer is trivial and will only work with the *hello-world.gltf* example. It is not intended to be a full glTF renderer but rather a minimal-abstraction view at the basics of how a glTF file is parsed and rendered. In some cases the renderer might be making assumptions for the *hello-world.gltf* use case that would not be valid for a generic glTF file.

The demo will render the *hello-world.gltf* scene, a single triangle using the default glTF material (using unlit red for the sample), into the TextureView's SurfaceTexture.

The code will:
- Create a render thread with a message queue for the TextureView (SampleGLTFView)
- Once the SurfaceTexture for the TextureView is available, it will initialize the loading of the *helloworld.gltf* asset
- `SampleGLTFRenderer.createOnGlThread(context, "helloworld.gltf")` is called to initialize shader program and use `SampleGLTFReader.read(input)` to parse the JSON file and generate a "scene graph"
- `SampleGLTFRenderer.createGLTFRenderObjects(gltfScene)` is called to create the GPU ready representation of the *helloworld.gltf* scene
- Each frame the Choreographer VSYNC triggers `SampleGLTFRenderer.draw(viewMatrix, projectionMatrix)` which executes OpenGL ES draw commmands using the buffer data from the *helloworld.gltf* file.

SampleGLTFRenderer will also plug-in to the AR Core SDK hello_ar_java sample (https://github.com/google-ar/arcore-android-sdk/tree/v1.2.1/samples/hello_ar_java); just replace the usage of ObjectRenderer with SampleGLTFRenderer in HelloArActivity so you can render the *helloworld.gltf* triangle in AR.

In `HelloArActivity.onSurfaceCreated(...)`:
- `sampleGLTFRenderer.createOnGlthread(context, "helloworld.gltf")`

In `HelloArActivity.onDrawFrame(...)`:
- `sampleGLTFRenderer.updateModelMatrix(anchorMatrix, scaleMatrix)`
- `sampleGLTFRenderer.draw(viewmtx, projmtx)`

Visit:

* https://developers.facebook.com/docs/sharing/3d-posts - To learn about sharing glTF 3D scenes on Facebook with 3D posts
* https://github.com/KhronosGroup/glTF - To learn more about the glTF format (read specs and tutorials, find tools and sample models, etc)

## Disclaimer

* This code is intended as a limited example for learning purposes and is by no means production quality.
* It may not be entirely stable.
* It has been tested on limited high end Android devices
* It's not intended as a demonstration of "the right way" to do things.
* It will not be supported.

## Requirements

Requires Android OS version Marshmallow 6.0 (API 23) or higher

To build you'll need the Android SDK with build tools.

*Note: This code was developed on Android Studio 3.1.2*

## Usage

Open the directory in Android Studio and the gradle project should be imported.
Run & Debug options should allow you to build the project.

### Command Line

Assure you have a local.properties file in the top level directory with:
*sdk.dir=/path/to/your/Android/sdk*

    $ gradlew installDebug
    $ adb shell am start -n com.facebook.sample/.SampleGLTFActivity

## License

glTF-Renderer is Creative Commons CC BY-NC 4.0 Attribution-NonCommercial licensed, as found in the LICENSE file.
