#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler3;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform vec2 ScreenSize;
uniform float Strength;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
noperspective in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    texSize = max(texSize, vec2(1.0));
    ivec2 itex = ivec2(floor(texCoord0 * texSize));
    itex = clamp(itex, ivec2(0), ivec2(texSize) - ivec2(1));
    vec4 skin = texelFetch(Sampler0, itex, 0);
    if (skin.a < 0.5) {
        discard;
    }

    vec4 lit = skin * vertexColor * ColorModulator;
    lit.rgb = mix(overlayColor.rgb, lit.rgb, overlayColor.a);
    lit *= lightMapColor;
    vec4 normalColor = linear_fog(lit, vertexDistance, FogStart, FogEnd, FogColor);

    float amount = clamp(Strength, 0.0, 1.0);
    if (amount <= 0.001) {
        fragColor = normalColor;
        return;
    }

    vec2 texelCenter = (vec2(itex) + 0.5) / texSize;
    vec2 duv = texelCenter - texCoord0;
    vec2 dx = dFdx(texCoord0);
    vec2 dy = dFdy(texCoord0);
    float det = dx.x * dy.y - dx.y * dy.x;
    vec2 samplePos = gl_FragCoord.xy;
    float mag2 = dot(dx, dx) + dot(dy, dy);
    if (det * det > 1.0e-12 * mag2 * mag2) {
        samplePos += vec2((duv.x * dy.y - duv.y * dy.x) / det, (dx.x * duv.y - dx.y * duv.x) / det);
    }

    vec2 maxPos = max(ScreenSize - vec2(1.0), vec2(0.0));
    ivec2 ipos = ivec2(clamp(floor(samplePos), vec2(0.0), maxPos));
    vec4 scene = texelFetch(Sampler3, ipos, 0);
    vec4 cloak = vec4(scene.rgb, 1.0);
    cloak.rgb = mix(overlayColor.rgb, cloak.rgb, overlayColor.a);

    fragColor = mix(normalColor, cloak, amount);
}
