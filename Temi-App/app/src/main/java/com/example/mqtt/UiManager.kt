package com.example.mqtt

import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import coil.load
import org.json.JSONObject


class UIManager(
        private val imageView: ImageView,
        private val videoView: VideoView,
        private val textView: TextView,
        private val publishMessage: (String, String) -> Unit
) {

    fun clearTablet(id : String) {
        resetTablet()
        if(id != "nothing"){
            publishMessage("temi/clear/finished/$id", "done")
        }

    }
    fun showVideo(videoUrl: String) {
        // Bild und Text ausblenden
        imageView.visibility = View.GONE
        textView.text = ""

        // Video URI setzen und abspielen
        videoView.apply {
            setVideoURI(Uri.parse(videoUrl))
            visibility = View.VISIBLE
            start()
        }

        // Nach Videostart, Node beenden
        publishMessage("temi/vid/finished", "done")
    }
    fun showImage(jsonString: String) {
        val jsonObject = JSONObject(jsonString)
        val imageUrl = jsonObject.getString("image")
        val id = jsonObject.getString("id")
        videoView.visibility = View.GONE
        textView.text = ""

        imageView.apply {
            visibility = View.VISIBLE
            load(imageUrl) {
                crossfade(true)
                listener(onError = { _, _ ->
                    visibility = View.GONE // Bei einem Fehler, Bild ausblenden
                })
            }
        }

        publishMessage("temi/img/finished/$id", "done")
    }
    fun showText(jsonString:String) {
        val jsonObject = JSONObject(jsonString)
        val text = jsonObject.getString("text")
        val id = jsonObject.getString("id")
        videoView.visibility = View.GONE
        imageView.visibility = View.GONE

        textView.apply {
            visibility = View.VISIBLE
            setText(text)
        }

        publishMessage("temi/txt/finished/$id", "done")
    }
    // Methode zum Abwarten in Sekunden
    fun wait(jsonString: String) {
        val jsonObject = JSONObject(jsonString)
        val time = jsonObject.getString("time")
        val id = jsonObject.getString("id")

        try {
            Thread.sleep(time.toLong() * 1000)

            publishMessage("temi/wait/finished/$id", "done")

        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resetTablet() {
        imageView.visibility = View.GONE
        videoView.visibility = View.GONE
        textView.text = ""
    }


}