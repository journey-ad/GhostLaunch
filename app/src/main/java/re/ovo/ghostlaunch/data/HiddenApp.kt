package re.ovo.ghostlaunch.data

import com.google.gson.annotations.SerializedName

data class HiddenApp(
    @SerializedName("packageName")
    val packageName: String,
    @SerializedName("className")
    val className: String? = null,
    @SerializedName("label")
    val label: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("lastLaunched")
    val lastLaunched: Long = 0L
)
