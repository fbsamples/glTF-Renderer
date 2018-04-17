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

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This is a trivial example of a glTF file reader that parses the JSON of the helloworld.gltf,
 * creates the node mesh list, and decodes the data URI buffer in preparation for rendering with
 * OpenGL ES.
 *
 * This is not intended to be a generic glTF reader; it will only work for the helloworld.gltf
 * example. It should however help provide a basic understanding of how a glTF renderer goes from
 * loading a JSON file to rendering the glTF scene.
 */
public class SampleGLTFReader {
    private static final String TAG = SampleGLTFReader.class.getSimpleName();
    private static final String DATA_URI_PREFIX = "data:application/octet-stream;base64,";

    static final int TARGET_ARRAY_BUFFER = 34962;
    static final int TARGET_ELEMENT_ARRAY_BUFFER = 34963;

    static final int COMPONENT_TYPE_BYTE = 5120;
    static final int COMPONENT_TYPE_UNSIGNED_BYTE = 5121;
    static final int COMPONENT_TYPE_SHORT = 5122;
    static final int COMPONENT_TYPE_UNSIGNED_SHORT = 5123;
    static final int COMPONENT_TYPE_INT = 5124;
    static final int COMPONENT_TYPE_UNSIGNED_INT  = 5125;
    static final int COMPONENT_TYPE_FLOAT = 5126;
    static final int COMPONENT_TYPE_DOUBLE = 5127;

    static class GLTFScene {
        static class Scene {
            String name;
            ArrayList<Integer> nodes = new ArrayList<>();
        }

        static class Node {
            String name;
            int mesh;
            ArrayList<Integer> children = new ArrayList<>();
        }

        static class Primitive {
            HashMap<String, Integer> attributes = new HashMap<>();
            int indices; // index of accessor containing indices
        }

        static class Mesh {
            String name;
            ArrayList<Primitive> primitives = new ArrayList<>();
        }

        static class Buffer {
            String name;
            ByteBuffer data;
            int byteLength;
            String uri;
        }

        static class BufferView {
            String name;
            int buffer;
            int byteOffset;
            int byteLength;
            int byteStride;
            int target;
        }

        static class Accessor {
            String name;
            int bufferView;
            int byteOffset;
            int componentType;
            int count;
            String type;
        }

        ArrayList<Scene> scenes = new ArrayList<>();
        ArrayList<Node> nodes = new ArrayList<>();
        ArrayList<Mesh> meshes = new ArrayList<>();
        ArrayList<Buffer> buffers = new ArrayList<>();
        ArrayList<BufferView> bufferViews = new ArrayList<>();
        ArrayList<Accessor> accessors = new ArrayList<>();
    }

    private static String readFile(InputStream stream, Charset cs)
            throws IOException {
        try {
            Reader reader = new BufferedReader(new InputStreamReader(stream, cs));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } finally {
            stream.close();
        }
    }

    static GLTFScene read(InputStream gltfStream) {
        GLTFScene gltfScene = new GLTFScene();
        try {
            String gltfStr = readFile(gltfStream, Charset.defaultCharset());

            JSONObject root = new JSONObject(gltfStr);

            JSONArray scenes = (JSONArray) root.get("scenes");
            parseScenes(gltfScene, scenes);

            JSONArray nodes = (JSONArray) root.get("nodes");
            parseNodes(gltfScene, nodes);

            JSONArray meshes = (JSONArray) root.get("meshes");
            parseMeshes(gltfScene, meshes);

            JSONArray buffers = (JSONArray) root.get("buffers");
            parseBuffers(gltfScene, buffers);

            JSONArray bufferViews = (JSONArray) root.get("bufferViews");
            parseBufferViews(gltfScene, bufferViews);

            JSONArray accessors = (JSONArray) root.get("accessors");
            parseAccessors(gltfScene, accessors);

        } catch(Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return gltfScene;
    }

    private static void parseScenes(GLTFScene output, JSONArray scenes) {
        for (int i = 0; i < scenes.length(); ++i) {
            try {
                JSONObject jsonObject = scenes.getJSONObject(i);
                GLTFScene.Scene scene = new GLTFScene.Scene();
                if (jsonObject.has("name")) {
                    scene.name = jsonObject.getString("name");
                }
                JSONArray nodes = jsonObject.getJSONArray("nodes");
                for (int j = 0; j < nodes.length(); ++j) {
                    scene.nodes.add((Integer)nodes.get(j));
                }
                output.scenes.add(scene);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private static void parseNodes(GLTFScene output, JSONArray nodes) {
        for (int i = 0; i < nodes.length(); ++i) {
            try {
                JSONObject jsonObject = nodes.getJSONObject(i);
                GLTFScene.Node node = new GLTFScene.Node();
                if (jsonObject.has("name")) {
                    node.name = jsonObject.getString("name");
                }
                node.mesh = jsonObject.getInt("mesh");
                output.nodes.add(node);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private static void parseMeshes(GLTFScene output, JSONArray meshes) {
        for (int i = 0; i < meshes.length(); ++i) {
            try {
                JSONObject jsonObject = meshes.getJSONObject(i);
                GLTFScene.Mesh mesh = new GLTFScene.Mesh();
                if (jsonObject.has("name")) {
                    mesh.name = jsonObject.getString("name");
                }
                JSONArray primitives = jsonObject.getJSONArray("primitives");
                for (int j = 0; j < primitives.length(); ++j) {
                    GLTFScene.Primitive primitive = new GLTFScene.Primitive();
                    JSONObject jsonPrimitive = (JSONObject)primitives.get(i);
                    JSONObject attributes = (JSONObject)jsonPrimitive.get("attributes");
                    Iterator<String> iter = attributes.keys();

                    while (iter.hasNext()) {
                       String key = iter.next();
                       primitive.attributes.put(key, (Integer)attributes.get(key));
                       primitive.indices = jsonPrimitive.getInt("indices");
                    }
                    mesh.primitives.add(primitive);
                }

                output.meshes.add(mesh);

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private static void parseBuffers(GLTFScene output, JSONArray buffers) {
        for (int i = 0; i < buffers.length(); ++i) {
            try {
                JSONObject jsonObject = buffers.getJSONObject(i);
                GLTFScene.Buffer buffer = new GLTFScene.Buffer();
                if (jsonObject.has("name")) {
                    buffer.name = jsonObject.getString("name");
                }
                buffer.byteLength = jsonObject.getInt("byteLength");
                // For this hello-world loader we'll only support loading from a data URI
                buffer.uri = jsonObject.getString("uri");
                String dataURI = buffer.uri.replaceFirst(DATA_URI_PREFIX, "");
                byte[] bufferData = Base64.decode(dataURI, Base64.DEFAULT);
                // Important to allocateDirect(...); wrap(...) doesn't work as GLES20.glBufferData wants a direct buffer.
                buffer.data = ByteBuffer.allocateDirect(bufferData.length).order(ByteOrder.nativeOrder());
                buffer.data.put(bufferData);
                buffer.data.rewind();
                output.buffers.add(buffer);

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private static void parseBufferViews(GLTFScene output, JSONArray bufferViews) {
        for (int i = 0; i < bufferViews.length(); ++i) {
            try {
                JSONObject jsonObject = bufferViews.getJSONObject(i);
                GLTFScene.BufferView bufferView = new GLTFScene.BufferView();
                if (jsonObject.has("name")) {
                    bufferView.name = jsonObject.getString("name");
                }
                bufferView.buffer = jsonObject.getInt("buffer"); // index to list of buffers
                bufferView.byteLength = jsonObject.getInt("byteLength");
                bufferView.byteOffset = jsonObject.getInt("byteOffset");
                if (jsonObject.has("byteStride")) {
                    bufferView.byteStride = jsonObject.getInt("byteStride");
                }
                bufferView.target = jsonObject.getInt("target");
                output.bufferViews.add(bufferView);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private static void parseAccessors(GLTFScene output, JSONArray accessors) {
         for (int i = 0; i < accessors.length(); ++i) {
            try {
                JSONObject jsonObject = accessors.getJSONObject(i);
                GLTFScene.Accessor accessor = new GLTFScene.Accessor();
                if (jsonObject.has("name")) {
                    accessor.name = jsonObject.getString("name");
                }
                accessor.bufferView = jsonObject.getInt("bufferView"); // index to list of buffers
                accessor.byteOffset = jsonObject.getInt("byteOffset");
                accessor.componentType = jsonObject.getInt("componentType");
                accessor.count = jsonObject.getInt("count");
                accessor.type = jsonObject.getString("type");
                output.accessors.add(accessor);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
