package com.beniezsche.www.projectwhistle

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.activity_main.*

import java.util.Timer
import java.util.TimerTask

import java.lang.Thread.sleep

class MainActivity : AppCompatActivity() {

    private var isEmergency = false
    private var screamPlayer: MediaPlayer? = null
    private var volumeControl: AudioManager? = null
    private var isFlashlightAvailable = false
    private var mCameraManager: CameraManager? = null
    private var mCameraId: String? = null

    private var flashTimer: Timer? = null
    private var layout: ConstraintLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layout = findViewById(R.id.layout)

        val sharedPref = this?.getPreferences(Context.MODE_PRIVATE) ?: return
        val firstTime =  sharedPref.getBoolean(getString(R.string.first_time), true)

        if(firstTime){
            val dialog = DFragment()
            dialog.show(supportFragmentManager, "AboutDialogFragment")
            with (sharedPref.edit()) {
                putBoolean(getString(R.string.first_time), false)
                commit()
            }
        }



        val languagePreference = PreferenceManager.getDefaultSharedPreferences(this@MainActivity).getString("language", "Default list prefs")
        helpButton!!.text = languagePreference


        screamPlayer = MediaPlayer.create(this, R.raw.scream)
        screamPlayer!!.isLooping = true

    }

    override fun onResume() {
        super.onResume()

        val languagePreference = PreferenceManager.getDefaultSharedPreferences(this@MainActivity).getString("language", "Help")
        helpButton!!.text = languagePreference

    }

    override fun onDestroy() {
        super.onDestroy()
        screamPlayer!!.release()
    }


    fun help(view: View) {

        volumeControl = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        isFlashlightAvailable = applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        volumeControl!!.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                volumeControl!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0)


        if (!isEmergency) {
            isEmergency = true

            if(isFlashlightAvailable){

                mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                try {
                    mCameraId = mCameraManager!!.cameraIdList[0]

                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }

                flashTimer = Timer()
                flashTimer!!.scheduleAtFixedRate(FlashThread(), 0, 1000)

            }
            layout!!.setBackgroundColor(Color.parseColor("#E74C3C"))
            helpButton!!.textSize = 100f
            helpButton!!.typeface = Typeface.DEFAULT_BOLD
            screamPlayer!!.start()

        } else {
            isEmergency = false

            if(isFlashlightAvailable){
                flashTimer!!.cancel()
                flashTimer!!.purge()
            }

            layout!!.setBackgroundColor(Color.parseColor("#0C0C0C"))
            helpButton!!.textSize = 30f
            helpButton!!.typeface = Typeface.DEFAULT
            screamPlayer!!.pause()
            screamPlayer!!.seekTo(0)


        }

    }

    fun about(view: View){

        val dialog = DFragment()
        dialog.show(supportFragmentManager, "AboutDialogFragment")

    }

    fun setting(view : View){

        val settingsIntent =  Intent(this,SettingsActivity::class.java)
        startActivity(settingsIntent)

    }



    internal inner class FlashThread : TimerTask() {

        override fun run() {

            try {

                mCameraManager!!.setTorchMode(mCameraId, true)
                sleep(1000)
                mCameraManager!!.setTorchMode(mCameraId, false)

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    class DFragment : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(activity!!)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(inflater.inflate(R.layout.about_dialog, null))

                    .setNegativeButton("OK") { dialog, id -> this@DFragment.dialog?.cancel() }
            return builder.create()
        }


    }

}

