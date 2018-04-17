/**
 * Copyright 2016-present, Facebook, Inc.
 * All rights reserved.
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

package com.facebook.sample.gles;

import android.opengl.GLES20;
import android.util.Log;

public class ShaderProgram {
    private static final String TAG = ShaderProgram.class.getSimpleName();

    private int shaderProgramHandle;

    public ShaderProgram(String vertexShader, String fragmentShader) {
        shaderProgramHandle = createProgram(vertexShader, fragmentShader);
    }

    public int getShaderHandle() {
        return shaderProgramHandle;
    }

    public void release() {
        GLES20.glDeleteProgram(shaderProgramHandle);
        shaderProgramHandle = -1;
    }

    private static void checkLocation(int location, String name) {
        if (location >= 0) {
            return;
        }
        throw new RuntimeException("Could not find location for " + name);
    }

    public int getAttribute(String name) {
        int loc = GLES20.glGetAttribLocation(shaderProgramHandle, name);
        checkLocation(loc, name);
        return loc;
    }

    public int getUniform(String name) {
        int loc = GLES20.glGetUniformLocation(shaderProgramHandle, name);
        checkLocation(loc, name);
        return loc;
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GLES20.glCreateProgram();
        GLHelpers.checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
            return 0;
        }
        GLES20.glAttachShader(program, vertexShader);
        GLHelpers.checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        GLHelpers.checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        GLHelpers.checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }
}
