package com.example.storyappfinal.utils


import android.annotation.SuppressLint
import android.app.Application
import android.app.Dialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.net.Uri
import android.os.Environment
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.storyappfinal.R
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


object Helper {

    /* -------------------------
    * PERMISSION
    * ------------------------- */

    fun notifyGivePermission(context: Context, message: String) {
        val dialog = dialogInfoBuilder(context, message)
        val button = dialog.findViewById<Button>(R.id.button_ok)
        button.setOnClickListener {
            dialog.dismiss()
            openSettingPermission(context)
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    fun isPermissionGranted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    fun openSettingPermission(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", context.packageName, null)
        context.startActivity(intent)
    }

    /* -------------------------
    *  DATE FORMAT
    * ------------------------- */
    private const val timestampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    private const val simpleDateFormat = "dd MMM yyyy HH.mm"

    /*
    * DATE INSTANCE
    * */
    private var defaultDate = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

    @SuppressLint("ConstantLocale")
    val simpleDate = SimpleDateFormat(simpleDateFormat, Locale.getDefault())

    /* curent date in date */
    private fun getCurrentDate(): Date {
        return Date()
    }

    /* curent date in string */
    fun getCurrentDateString(): String = defaultDate.format(getCurrentDate())

    @SuppressLint("ConstantLocale")
    val currentTimestamp: String = SimpleDateFormat(
        "ddMMyyHHmmssSS",
        Locale.getDefault()
    ).format(System.currentTimeMillis())


    /* simpleDate (Date) to string */
    private fun getSimpleDate(date: Date): String = simpleDate.format(date)

    /* string UTC format to date */
    private fun parseUTCDate(timestamp: String): Date {
        return try {
            val formatter = SimpleDateFormat(timestampFormat, Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            formatter.parse(timestamp) as Date
        } catch (e: ParseException) {
            getCurrentDate()
        }
    }


    /* get readable date of uploaded story, ie : 30 April 2022 00.00 */
    fun getUploadStoryTime(timestamp: String): String {
        val date: Date = parseUTCDate(timestamp)
        return getSimpleDate(date)
    }


    /* -------------------------
    *  CUSTOM DIALOG
    * ------------------------- */

    /* custom dialog info builder -> reuse to another invocation with custom ok button action */
    fun dialogInfoBuilder(
        context: Context,
        message: String,
        alignment: Int = Gravity.CENTER
    ): Dialog {
        val dialog = Dialog(context)
        dialog.setCancelable(false)
        dialog.window!!.apply {
            val params: WindowManager.LayoutParams = this.attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes.windowAnimations = android.R.transition.fade
            setGravity(Gravity.CENTER)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.setContentView(R.layout.custom_dialog_info)
        val tvMessage = dialog.findViewById<TextView>(R.id.message)
        when (alignment) {
            Gravity.CENTER -> tvMessage.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER
            Gravity.START -> tvMessage.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            Gravity.END -> tvMessage.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        tvMessage.text = message
        return dialog
    }

    /* ready use to go dialog with related params */
    fun showDialogInfo(
        context: Context,
        message: String,
        alignment: Int = Gravity.CENTER
    ) {
        val dialog = dialogInfoBuilder(context, message, alignment)
        val btnOk = dialog.findViewById<Button>(R.id.button_ok)
        btnOk.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    /* -------------------------
    * FILE HELPER & BITMAP
    * ------------------------- */

    private fun createCustomTempFile(context: Context): File {
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(currentTimestamp, ".jpg", storageDir)
    }

    fun uriToFile(selectedImg: Uri, context: Context): File {
        val contentResolver: ContentResolver = context.contentResolver
        val myFile = createCustomTempFile(context)
        val inputStream = contentResolver.openInputStream(selectedImg) as InputStream
        val outputStream: OutputStream = FileOutputStream(myFile)
        val buf = ByteArray(1024)
        var len: Int
        while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
        outputStream.close()
        inputStream.close()
        return myFile
    }

    private fun getRandomString(len: Int = 20): String {
        val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return List(len) { alphabet.random() }.joinToString("")
    }

    /* get default exported file name */
    fun getDefaultFileName(): String {
        return "STORY-${getRandomString()}.jpg"
    }

    /* create export file (story / download) to exact path location */
    fun createFile(
        application: Application,
        folder: String = "story",
        filename: String = getDefaultFileName()
    ): File {
        val mediaDir = application.externalMediaDirs.firstOrNull()?.let {
            File(it, folder).apply { mkdirs() }
        }
        val outputDirectory = if (
            mediaDir != null && mediaDir.exists()
        ) mediaDir else application.filesDir
        return File(outputDirectory, filename)
    }

    fun rotateBitmap(bitmap: Bitmap, isBackCamera: Boolean = false): Bitmap {
        val matrix = Matrix()
        return if (isBackCamera) {
            matrix.postRotate(90f)
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        } else {
            matrix.postRotate(-90f)
            matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        }
    }

    /* -------------------------
    * GEOLOCATION & GEOCODER
    * ------------------------- */

    /* parse lat lon coordinate into readable address */
    fun parseAddressLocation(
        context: Context,
        lat: Double,
        lon: Double
    ): String {
        val geocoder = Geocoder(context)
        val geoLocation =
            geocoder.getFromLocation(lat, lon, 1)
        return if (geoLocation.size > 0) {
            val location = geoLocation[0]
            val fullAddress = location.getAddressLine(0)
            StringBuilder("📌 ")
                .append(fullAddress).toString()
        } else {
            "📌 Location Unknown"
        }
    }


    /* -------------------------
    * WIDGET
    * ------------------------- */

//    fun updateWidgetData(context: Context) {
//        Log.i("TEST_WIDGET", "Requested update data")
//        val appWidgetManager = AppWidgetManager.getInstance(context)
//        val ids: IntArray = appWidgetManager.getAppWidgetIds(
//            ComponentName(context, RecentStoryWidget::class.java)
//        )
//        /* if widget update requested -> refresh widget data */
//        appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.stack_view)
//    }

}