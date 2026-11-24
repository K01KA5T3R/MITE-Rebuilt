#version 150

uniform float MiteClassicWaterDensity;
uniform int MiteClassicWaterRadial;
uniform int MiteClassicAirFog;
uniform int MiteClassicAirRadial;

vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {
    if (MiteClassicWaterDensity > 0.0) {
        float fogValue = 1.0 - exp(-MiteClassicWaterDensity * max(vertexDistance, 0.0));
        return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
    }
    if (vertexDistance <= fogStart) {
        return inColor;
    }

    float fogValue = vertexDistance < fogEnd
        ? (MiteClassicAirFog != 0 ? (vertexDistance - fogStart) / (fogEnd - fogStart)
                                  : smoothstep(fogStart, fogEnd, vertexDistance)) : 1.0;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

float linear_fog_fade(float vertexDistance, float fogStart, float fogEnd) {
    if (MiteClassicWaterDensity > 0.0) {
        return exp(-MiteClassicWaterDensity * max(vertexDistance, 0.0));
    }
    if (vertexDistance <= fogStart) {
        return 1.0;
    } else if (vertexDistance >= fogEnd) {
        return 0.0;
    }

    if (MiteClassicAirFog != 0) {
        return (fogEnd - vertexDistance) / (fogEnd - fogStart);
    }
    return smoothstep(fogEnd, fogStart, vertexDistance);
}

float fog_distance(mat4 modelViewMat, vec3 pos, int shape) {
    if (MiteClassicWaterDensity > 0.0) {
        vec3 eye = (modelViewMat * vec4(pos, 1.0)).xyz;
        return MiteClassicWaterRadial != 0 ? length(eye) : abs(eye.z);
    }
    if (MiteClassicAirFog != 0) {
        vec3 eye = (modelViewMat * vec4(pos, 1.0)).xyz;
        return MiteClassicAirRadial != 0 ? length(eye) : abs(eye.z);
    }
    if (shape == 0) {
        return length((modelViewMat * vec4(pos, 1.0)).xyz);
    } else {
        float distXZ = length((modelViewMat * vec4(pos.x, 0.0, pos.z, 1.0)).xyz);
        float distY = length((modelViewMat * vec4(0.0, pos.y, 0.0, 1.0)).xyz);
        return max(distXZ, distY);
    }
}
