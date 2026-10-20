package lt.marcomultiservices.kimba.ui.scene

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * The lake surface as a GPU shader (Android 13+): layered waves seen in perspective, the sky
 * reflected with a Fresnel falloff, a glitter path under the sun or moon, raindrop rings and
 * the rings spreading from the float.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class WaterShader {
    val shader = RuntimeShader(SOURCE)

    fun update(
        width: Float, height: Float, horizon: Float, time: Float, colors: SceneColors,
        sunX: Float, floatX: Float, floatY: Float, ripple: Float,
    ) {
        shader.setFloatUniform("res", width, height)
        shader.setFloatUniform("horizon", horizon)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("rain", colors.rain)
        shader.setFloatUniform("wind", colors.wind.coerceAtMost(14f))
        shader.setFloatUniform("sunX", sunX)
        shader.setFloatUniform("glow", colors.glowStrength)
        shader.setFloatUniform("floatP", floatX, floatY, ripple)
        shader.setColorUniform("skyTop", colors.skyTop.toArgb())
        shader.setColorUniform("skyHor", colors.skyHorizon.toArgb())
        shader.setColorUniform("deep", colors.waterDeep.toArgb())
        shader.setColorUniform("glowCol", colors.glow.toArgb())
        shader.setColorUniform("lightCol", Color.White.copy(alpha = colors.daylight).toArgb())
    }

    companion object {
        private const val SOURCE = """
uniform float2 res;
uniform float horizon;
uniform float time;
uniform float rain;
uniform float wind;
uniform float sunX;
uniform float glow;
uniform float3 floatP;
layout(color) uniform half4 skyTop;
layout(color) uniform half4 skyHor;
layout(color) uniform half4 deep;
layout(color) uniform half4 glowCol;
layout(color) uniform half4 lightCol;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }

float waves(float2 q, float t) {
    float h = sin(q.x * 1.3 + t * 0.7 + sin(q.y * 0.7 + t * 0.3)) * 0.5;
    h += sin(q.x * 2.9 - q.y * 1.7 + t * 1.4) * 0.25;
    h += sin(q.x * 5.3 + q.y * 3.1 - t * 2.1) * 0.12;
    h += sin(q.x * 11.0 - q.y * 7.0 + t * 3.3) * 0.05;
    return h;
}

half4 main(float2 p) {
    float wy = (p.y - horizon) / (res.y - horizon);
    float y = max(wy, 0.002);
    float z = 1.0 / (y + 0.04);
    float2 q = float2((p.x / res.x - 0.5) * z * 4.0, z * 2.2);
    float t = time * (0.6 + wind * 0.08);
    float e = 0.04;
    float h0 = waves(q, t);
    float hx = waves(q + float2(e, 0.0), t) - h0;
    float hy = waves(q + float2(0.0, e), t) - h0;
    float amp = (0.05 + wind * 0.012) * smoothstep(0.0, 0.12, y);
    float2 n = float2(hx, hy) / e * amp;

    if (rain > 0.0) {
        float2 g = q * 1.6;
        float2 cell = floor(g);
        float2 f = fract(g) - 0.5;
        float rnd = hash(cell);
        float ph = fract(time * 0.9 + rnd);
        float2 off = float2(hash(cell + 3.1), hash(cell + 7.7)) - 0.5;
        float2 dv = f - off * 0.5;
        float d = length(dv);
        float ring = smoothstep(0.05, 0.0, abs(d - ph * 0.45)) * (1.0 - ph) * step(rnd, rain);
        n += dv / max(d, 0.001) * ring * 0.18;
    }

    float2 dp = (p - floatP.xy) * float2(1.0, 3.2);
    float dd = length(dp);
    float rp = sin(dd * 0.16 - time * 6.5) * exp(-dd * 0.011) * floatP.z;
    n.y += rp * 0.3;

    float r = clamp(y * 1.25 + n.y * 1.5, 0.0, 1.0);
    float3 sky = mix(float3(skyHor.rgb), float3(skyTop.rgb), pow(r, 0.8));
    float fres = clamp(0.35 + 0.65 * pow(1.0 - y, 3.0), 0.0, 1.0);
    float3 col = mix(float3(deep.rgb), sky, fres);

    float dx = (p.x - sunX) / res.x + n.x * 0.6;
    float path = exp(-dx * dx * (40.0 + 160.0 * y));
    float sparkle = smoothstep(0.55, 1.0, h0 * 0.5 + 0.5 + n.x * 2.5);
    col += float3(glowCol.rgb) * glow * path * (0.2 + 1.1 * sparkle) * (1.0 - y * 0.4);

    // Small glints of skylight on wave crests in daylight.
    float glint = smoothstep(0.9, 1.0, h0 * 0.5 + 0.5 + n.y * 3.0) * float(lightCol.a) * 0.12 * (1.0 - y);
    col += glint;

    col *= 1.0 - y * 0.22;
    return half4(half3(col), 1.0);
}
"""
    }
}
