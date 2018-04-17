/**
 * Copyright 2018 Facebook Inc. All Rights Reserved.
 *
 * Licensed under the Creative Commons CC BY-NC 4.0 Attribution-NonCommercial
 * License (the "License"). You may obtain a copy of the License at
 * https://creativecommons.org/licenses/by-nc/4.0/.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.sample.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.facebook.sample.gles.GLHelpers;
import com.facebook.sample.gles.ShaderProgram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

/**
 * This is a trivial glTF renderer that issues GLES draw commands to render the primitive meshes
 * parsed by <em>SampleGLTFReader</em>. Note that this renderer is a sample that is only intended to
 * render helloworld.gltf. This is not intended to be a generic glTF renderer; it is intended to
 * be an introduction to 3D scene rendering with OpenGL ES APIs and the glTF format.
 */
public class SampleGLTFRenderer {
    private static final String TAG = SampleGLTFRenderer.class.getSimpleName();

    private static final int COORDS_PER_VERTEX = 3;
    private static final int BYTES_PER_FLOAT = 4;
    private static final int BYTES_PER_SHORT = 2;

    private ArrayList<GLTFRenderObject> gltfRenderObjects;

    private ShaderProgram shaderProgram;

    private int modelViewProjectionUniform;
    private int positionAttribute;

    private final float[] modelMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];

    public SampleGLTFRenderer() {}

    // For simplicity we're hardcoding the component types for our render objects rather than
    // creating the typed Buffers based on the accessor's componentType.
    //
    // We'll reference the BufferView buffers directly to avoid copying data. So we need the
    // byte offset and length for the vertex and index buffers.
    public static class GLTFRenderObject {
        ShortBuffer indices;
        int indexByteOffset, indexByteLength;
        int indexBufferId;

        FloatBuffer vertices;
        int vertexByteOffset, vertexByteLength;
        int vertexBufferId;
    }

    // Prepares render data for each glTF mesh primitive.
    private ArrayList<GLTFRenderObject> CreateGLTFRenderObjects(SampleGLTFReader.GLTFScene gltfScene) {
        ArrayList<GLTFRenderObject> renderObjects = new ArrayList<>();
        // Note in real glTF nodes can have children. Here we're only loading top level siblings for the sample.
        for (int i = 0; i < gltfScene.meshes.size(); ++i) {
            SampleGLTFReader.GLTFScene.Mesh mesh = gltfScene.meshes.get(i);
            // Add each primitive into the render object list.
            for (int j = 0; j < mesh.primitives.size(); ++j) {
                GLTFRenderObject renderObject = new GLTFRenderObject();

                SampleGLTFReader.GLTFScene.Primitive primitive = mesh.primitives.get(j);
                // Find which accessor contains the data for this attribute
                int accessorIdx = primitive.attributes.get("POSITION");
                SampleGLTFReader.GLTFScene.Accessor accessor = gltfScene.accessors.get(accessorIdx);
                SampleGLTFReader.GLTFScene.BufferView bufferView = gltfScene.bufferViews.get(accessor.bufferView);
                SampleGLTFReader.GLTFScene.Buffer buffer = gltfScene.buffers.get(bufferView.buffer);

                // Load vertex data embedded in JSON
                if (accessor.componentType == SampleGLTFReader.COMPONENT_TYPE_FLOAT) {
                    renderObject.vertices = buffer.data.asFloatBuffer();
                    renderObject.vertexByteLength = bufferView.byteLength;
                    renderObject.vertexByteOffset = bufferView.byteOffset;
                    renderObject.vertices.position(bufferView.byteOffset / BYTES_PER_FLOAT);
                } else {
                    // Not needed for our example.
                    // Would need to initialize the correct Buffer type given the componentType.
                    Log.e(TAG, "Not implemented");
                }

                // Load index data embedded in JSON
                int indicesAccessor = primitive.indices;
                accessor = gltfScene.accessors.get(indicesAccessor);
                bufferView = gltfScene.bufferViews.get(accessor.bufferView);
                buffer = gltfScene.buffers.get(bufferView.buffer);

                if (bufferView.target == SampleGLTFReader.TARGET_ELEMENT_ARRAY_BUFFER) {
                    renderObject.indices = buffer.data.asShortBuffer();
                    renderObject.indexByteLength = bufferView.byteLength;
                    renderObject.indexByteOffset = bufferView.byteOffset;
                    renderObject.indices.position(bufferView.byteOffset / BYTES_PER_SHORT);
                } else {
                    Log.e(TAG, "Index buffer is invalid");
                }

                // Prepare and upload GPU data.
                int[] buffers = new int[2];

                GLES20.glGenBuffers(2, buffers, 0);
                renderObject.vertexBufferId = buffers[0];
                renderObject.indexBufferId = buffers[1];

                // Upload vertex buffer to GPU
                renderObject.vertices.position(renderObject.vertexByteOffset / BYTES_PER_FLOAT);
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, renderObject.vertexBufferId);
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, renderObject.vertexByteLength, renderObject.vertices, GLES20.GL_STATIC_DRAW);

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

                // Upload index buffer to GPU
                renderObject.indices.position(renderObject.indexByteOffset / BYTES_PER_SHORT);
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, renderObject.indexBufferId);
                GLES20.glBufferData(
                        GLES20.GL_ELEMENT_ARRAY_BUFFER, renderObject.indexByteLength, renderObject.indices, GLES20.GL_STATIC_DRAW);

                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

                GLHelpers.checkGlError("glTF buffer load");

                renderObjects.add(renderObject);
            }
        }
        return renderObjects;
    }

    private static String readAsset(Context context, String asset) {
        try {
            InputStream is = context.getAssets().open(asset);
            InputStreamReader reader = new InputStreamReader(is);
            BufferedReader buf = new BufferedReader(reader);
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = buf.readLine()) != null) {
                text.append(line).append('\n');
            }
            return text.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public void createOnGlThread(Context context, String glTFAssetName)
            throws IOException {

        shaderProgram = new ShaderProgram(
                readAsset(context, "gltfobjectvert.glsl"),
                readAsset(context, "gltfobjectfrag.glsl"));

        GLES20.glUseProgram(shaderProgram.getShaderHandle());

        modelViewProjectionUniform = shaderProgram.getUniform("u_ModelViewProjection");
        positionAttribute = shaderProgram.getAttribute("a_Position");
        Matrix.setIdentityM(modelMatrix, 0);

        // Read the gltf file and create render objects.
        InputStream gltfInput = context.getAssets().open(glTFAssetName);
        SampleGLTFReader.GLTFScene gltfScene = SampleGLTFReader.read(gltfInput);
        gltfRenderObjects = CreateGLTFRenderObjects(gltfScene);
    }

    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
    }

    public void draw(float[] cameraView, float[] cameraPerspective) {
        GLHelpers.checkGlError("Before draw");

        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);

        GLES20.glUseProgram(shaderProgram.getShaderHandle());

        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

        for (int i = 0; i < gltfRenderObjects.size(); ++i) {
            GLTFRenderObject renderObject = gltfRenderObjects.get(i);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, renderObject.vertexBufferId);

            int verticesBaseAddress = 0;

            GLES20.glVertexAttribPointer(
                    positionAttribute, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, verticesBaseAddress);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

            GLES20.glEnableVertexAttribArray(positionAttribute);

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, renderObject.indexBufferId);
            int numElements = renderObject.indexByteLength / BYTES_PER_SHORT;
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, GLES20.GL_UNSIGNED_SHORT, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

            GLES20.glDisableVertexAttribArray(positionAttribute);
        }

        GLHelpers.checkGlError("After draw");
    }

    public void release() {
       shaderProgram.release();
       for (GLTFRenderObject object : gltfRenderObjects) {
           int[] buffers = { object.vertexBufferId, object.indexBufferId };
           GLES20.glDeleteBuffers(2, buffers,0);
       }
    }
}
