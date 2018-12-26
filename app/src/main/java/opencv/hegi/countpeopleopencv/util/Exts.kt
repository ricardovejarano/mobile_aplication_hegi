package opencv.hegi.countpeopleopencv.util

import android.widget.EditText
import com.google.firebase.database.DataSnapshot
import java.text.SimpleDateFormat
import java.util.*

fun EditText.text(): String = text.toString()

fun DataSnapshot.value(path: String) = this.child(path).value

fun Date.format(): String = SimpleDateFormat("yyyy-MM-dd").format(this)

fun Date.formatHour(): String = SimpleDateFormat("HH:mm:ss").format(this)