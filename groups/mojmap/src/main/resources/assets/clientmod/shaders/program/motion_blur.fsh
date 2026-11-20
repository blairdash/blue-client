#version 150

uniform sampler2D DiffuseSampler;   // current frame
uniform sampler2D PrevFrameSampler; // last frame's output (bound in Java, see MotionBlurEffect)
uniform float BlurStrength;         // 0 = off, higher = trails last longer

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 current = texture(DiffuseSampler, texCoord);
    vec4 previous = texture(PrevFrameSampler, texCoord);
    fragColor = mix(current, previous, clamp(BlurStrength, 0.0, 0.95));
}
