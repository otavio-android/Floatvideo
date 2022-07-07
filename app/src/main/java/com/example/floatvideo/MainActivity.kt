package com.talentica.androidkotlin.videostreaming

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import kotlin.concurrent.fixedRateTimer
import kotlin.math.sign

class MainActivity : AppCompatActivity(), MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, SeekBar.OnSeekBarChangeListener,
    View.OnClickListener {

    lateinit var sampleVideoView: VideoView
    lateinit var seekBar: SeekBar
    lateinit var playPauseButton: ImageView
    lateinit var stopButton: ImageView
    lateinit var runningTime: TextView
    lateinit var btn_playlist:Button
    var currentPosition: Int = 0
    var isRunning = false
    var permissoes = arrayOf(WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE)
    var codigo = 99

    var permissao_concedida = 0

    //Always device to run this App
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sampleVideoView = findViewById<VideoView>(R.id.videoView)
        playPauseButton = findViewById<ImageView>(R.id.playPauseButton)
        playPauseButton.setOnClickListener(this)
        stopButton = findViewById<ImageView>(R.id.stopButton)
        stopButton.setOnClickListener(this)
        seekBar = findViewById<SeekBar>(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(this)
        runningTime = findViewById<TextView>(R.id.runningTime)
        btn_playlist = findViewById<Button>(R.id.button4)
        btn_playlist.setOnClickListener(this)
        runningTime.setText("00:00")
        //Add the listeners
        sampleVideoView.setOnCompletionListener(this)
        sampleVideoView.setOnErrorListener(this)
        sampleVideoView.setOnPreparedListener(this)


    }



    override fun onStart() {
        super.onStart()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Toast.makeText(baseContext, "Play finished", Toast.LENGTH_LONG).show()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e("video", "setOnErrorListener ")
        return true
    }

    override fun onPrepared(mp: MediaPlayer?) {
        seekBar.setMax(sampleVideoView.getDuration()!!)
        sampleVideoView.start()

        val fixedRateTimer = fixedRateTimer(name = "hello-timer",
            initialDelay = 0, period = 300) {
            refreshSeek()
        }

        playPauseButton.setImageResource(R.mipmap.pause_button)
    }

    fun refreshSeek() {
        seekBar.setProgress(sampleVideoView.getCurrentPosition()!!)

        if (sampleVideoView.isPlaying()!! == false) {
            return
        }

        var time = sampleVideoView.getCurrentPosition()!! / 1000;
        var minute = time / 60;
        var second = time % 60;

        runOnUiThread {
            runningTime.setText(minute.toString() + ":" + second.toString());
        }
    }

    var refreshTime = Runnable() {
        fun run() {

        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            val progress = seekBar?.progress
            sampleVideoView.seekTo(progress!!)
        }

    }



    override fun onStartTrackingTouch(seekBar: SeekBar?) {

        //do nothing

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        sampleVideoView.seekTo(seekBar?.getProgress()!!)
        //seekBar.setProgress()

    }



    override fun onClick(v: View?) {
         if(v?.id ==R.id.button4){
             var permission_playlist =  if(permissao_concedida ==1) playlist() else request("playlist")
         }
        if (v?.getId() == R.id.playPauseButton){
            if (!isRunning) {
                isRunning = true
                playPauseButton.setImageResource(R.mipmap.pause_button)
                sampleVideoView.seekTo(currentPosition)
                var start_resume = if(currentPosition>0)sampleVideoView?.start() else request("getallvideos")

            } else { //Pause video
                isRunning = false
                sampleVideoView.pause()
                currentPosition = sampleVideoView?.getCurrentPosition()!!
                playPauseButton.setImageResource(R.mipmap.play_button)

            }
        } else if (v?.getId() == R.id.stopButton) {
            playPauseButton.setImageResource(R.mipmap.play_button)
            sampleVideoView.stopPlayback()
            currentPosition = 0
        }
    }

     fun request(tag:String) {

        if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ){

                ActivityCompat.requestPermissions(this@MainActivity,permissoes,codigo)
        }
      else{
            permissao_concedida+=1
            var ternario = if(tag=="playlist") playlist() else getAllVideos()

        }

    }
    var indice = 0
    fun playlist() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var videos_index:Int? = null

                val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Video.Media.DISPLAY_NAME
                )

                val orderBy:String = MediaStore.Video.Media.DATE_TAKEN

                val cursor = applicationContext.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "$orderBy DESC"
                )

                 videos_index = cursor?.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                 var videos_name:String? = null

                var size_playlist = cursor!!.count
                var playlist_videos = Array<String>(size_playlist){"0"}
                var cont = 0

                while (cursor!!.moveToNext()==true){
                    videos_name = cursor!!.getString(videos_index!!)
                    playlist_videos[cont] = videos_name
                    cont+=1
                }


                dialogo(playlist_videos)
            }

            else{

            }
        }catch (e:Exception){
            Log.e("ERROR", e.message?:"")
        }

    }

    fun dialogo(videos: Array<String>) {
        var dialogo = AlertDialog.Builder(this@MainActivity)
        dialogo.setTitle("Playlist de videos")
        dialogo.setItems(videos) { dialog, which ->
            indice = which

            getAllVideos()

        }
        var mostrar = dialogo.create()
        mostrar.show()

    }


    fun getAllVideos() {
        var listVideo = arrayListOf<String>()
        var cont = 0
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var columnVideo:Int ?= null

                var absolutePathVideo:String ?= null

                val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Video.Media.DATA
                )

                val orderBy:String = MediaStore.Video.Media.DATE_TAKEN

                val cursor = applicationContext.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "$orderBy DESC"
                )

                columnVideo = cursor?.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

                    while (cursor?.moveToNext()==true){
                        absolutePathVideo = cursor!!.getString(columnVideo!!)
                        listVideo.add(absolutePathVideo)
                    }
            }else{
            }

        }catch (e:Exception){
            Log.e("ERROR", e.message?:"")
        }

         playvideo(listVideo[indice])
    }

    fun playvideo(video: String){

        val uri =Uri.parse(video)
        sampleVideoView.setVideoURI(uri)
        sampleVideoView.requestFocus()
        sampleVideoView.start()
    }

}





