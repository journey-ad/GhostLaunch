# Compose
-dontwarn androidx.compose.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class re.ovo.ghostlaunch.data.HiddenApp { *; }

# Shizuku
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.server.** { *; }

# 反射调用 Shizuku.newProcess
-keep class rikka.shizuku.Shizuku { *; }

# 保留 Compose 语义信息（可选）
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations
