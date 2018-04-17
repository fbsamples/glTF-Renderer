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

precision mediump float;

// For our default material we'll just use red with no lighting. In full glTF the material's
// physically-based rendering properties would be parsed from the JSON. Those details are beyond the
// scope of this helloworld.gltf rendering sample.
void main() {
    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
}
