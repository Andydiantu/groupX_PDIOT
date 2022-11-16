package com.specknet.pdiotapp.live

import android.content.*
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.RecordingActivity
import com.specknet.pdiotapp.bluetooth.ConnectingActivity
import com.specknet.pdiotapp.resultAnalysis
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import org.json.JSONArray
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifDrawableBuilder
import pl.droidsonroids.gif.GifImageView
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder


class LiveDataActivity : AppCompatActivity() {
    private val TAG = this.javaClass.name

    // global graph variables
    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet

    lateinit var dataSet_thingy_accel_x: LineDataSet
    lateinit var dataSet_thingy_accel_y: LineDataSet
    lateinit var dataSet_thingy_accel_z: LineDataSet

    var time = 0f
    lateinit var allRespeckData: LineData

    lateinit var allThingyData: LineData

    lateinit var respeckChart: LineChart
    lateinit var thingyChart: LineChart

    lateinit var cur_activity: TextView
    lateinit var menu_live: Button
    lateinit var menu_history: Button
    lateinit var menu_record: Button
    lateinit var menu_connect: Button

    lateinit var imageView: ImageView

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var thingyLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    lateinit var looperThingy: Looper

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

//    private var myService: ActivityIdentifyService.ActivityIdentifyBinder? = null
//    private var mIsBound = false
//    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName, service: IBinder) {
//            Log.d(TAG, "onServiceConnected: LiveDataActivity")
//            myService = service as ActivityIdentifyService.ActivityIdentifyBinder
//            mIsBound = true
//        }
//
//        override fun onServiceDisconnected(name: ComponentName) {
//            Log.d(TAG, "onServiceDisconnected: onServiceDisconnected")
//            mIsBound = false
//        }
//    }

    var currnet_activity = "N/A"
    var icon = R.drawable.lying

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)

        setupCharts()

        cur_activity = findViewById(R.id.current_activity)
        cur_activity.setText(currnet_activity)

        setupClickListeners()

        setupCurrentActivity()



//        startService(Intent(this, ActivityIdentifyService::class.java))
//        this.bindService(
//            Intent(this, ActivityIdentifyService::class.java),
//            mServiceConnection, BIND_AUTO_CREATE
//        )

        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ



                    time += 1
                    updateGraph("respeck", x, y, z)


                }
            }
        }

        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)

        // set up the broadcast receiver
        thingyLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    time += 1
                    updateGraph("thingy", x, y, z)

                }
            }
        }

        // register receiver on another thread
        val handlerThreadThingy = HandlerThread("bgThreadThingyLive")
        handlerThreadThingy.start()
        looperThingy = handlerThreadThingy.looper
        val handlerThingy = Handler(looperThingy)
        this.registerReceiver(thingyLiveUpdateReceiver, filterTestThingy, null, handlerThingy)

    }


    fun setupCharts() {
        respeckChart = findViewById(R.id.respeck_chart)
        thingyChart = findViewById(R.id.thingy_chart)

        // Respeck

        time = 0f
        val entries_res_accel_x = ArrayList<Entry>()
        val entries_res_accel_y = ArrayList<Entry>()
        val entries_res_accel_z = ArrayList<Entry>()

        dataSet_res_accel_x = LineDataSet(entries_res_accel_x, "Accel X")
        dataSet_res_accel_y = LineDataSet(entries_res_accel_y, "Accel Y")
        dataSet_res_accel_z = LineDataSet(entries_res_accel_z, "Accel Z")

        dataSet_res_accel_x.setDrawCircles(false)
        dataSet_res_accel_y.setDrawCircles(false)
        dataSet_res_accel_z.setDrawCircles(false)

        dataSet_res_accel_x.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        dataSet_res_accel_y.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        dataSet_res_accel_z.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsRes = ArrayList<ILineDataSet>()
        dataSetsRes.add(dataSet_res_accel_x)
        dataSetsRes.add(dataSet_res_accel_y)
        dataSetsRes.add(dataSet_res_accel_z)

        allRespeckData = LineData(dataSetsRes)
        respeckChart.data = allRespeckData
        respeckChart.invalidate()

        // Thingy

        time = 0f
        val entries_thingy_accel_x = ArrayList<Entry>()
        val entries_thingy_accel_y = ArrayList<Entry>()
        val entries_thingy_accel_z = ArrayList<Entry>()

        dataSet_thingy_accel_x = LineDataSet(entries_thingy_accel_x, "Accel X")
        dataSet_thingy_accel_y = LineDataSet(entries_thingy_accel_y, "Accel Y")
        dataSet_thingy_accel_z = LineDataSet(entries_thingy_accel_z, "Accel Z")

        dataSet_thingy_accel_x.setDrawCircles(false)
        dataSet_thingy_accel_y.setDrawCircles(false)
        dataSet_thingy_accel_z.setDrawCircles(false)

        dataSet_thingy_accel_x.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        dataSet_thingy_accel_y.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        dataSet_thingy_accel_z.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsThingy = ArrayList<ILineDataSet>()
        dataSetsThingy.add(dataSet_thingy_accel_x)
        dataSetsThingy.add(dataSet_thingy_accel_y)
        dataSetsThingy.add(dataSet_thingy_accel_z)

        allThingyData = LineData(dataSetsThingy)
        thingyChart.data = allThingyData
        thingyChart.invalidate()
    }

    fun updateGraph(graph: String, x: Float, y: Float, z: Float) {
        // take the first element from the queue
        // and update the graph with it
        if (graph == "respeck") {
            dataSet_res_accel_x.addEntry(Entry(time, x))
            dataSet_res_accel_y.addEntry(Entry(time, y))
            dataSet_res_accel_z.addEntry(Entry(time, z))

            runOnUiThread {
                allRespeckData.notifyDataChanged()
                respeckChart.notifyDataSetChanged()
                respeckChart.invalidate()
                respeckChart.setVisibleXRangeMaximum(150f)
                respeckChart.moveViewToX(respeckChart.lowestVisibleX + 40)
            }
        } else if (graph == "thingy") {
            dataSet_thingy_accel_x.addEntry(Entry(time, x))
            dataSet_thingy_accel_y.addEntry(Entry(time, y))
            dataSet_thingy_accel_z.addEntry(Entry(time, z))

            runOnUiThread {
                allThingyData.notifyDataChanged()
                thingyChart.notifyDataSetChanged()
                thingyChart.invalidate()
                thingyChart.setVisibleXRangeMaximum(150f)
                thingyChart.moveViewToX(thingyChart.lowestVisibleX + 40)
            }
        }


    }

    private fun getActivity(reSpeckLiveData: RESpeckLiveData ){
        try {
            val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * 50 * 12)
            // Creates inputs for reference.
            //val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 50, 12), DataType.FLOAT32)
            byteBuffer.order(ByteOrder.nativeOrder());
            // ADD VALUES
            for (j in 0 until 50) {
                byteBuffer.putFloat(reSpeckLiveData.accelX)
                byteBuffer.putFloat(reSpeckLiveData.accelY)
                byteBuffer.putFloat(reSpeckLiveData.accelZ)
                byteBuffer.putFloat(reSpeckLiveData.gyro.x)
                byteBuffer.putFloat(reSpeckLiveData.gyro.y)
                byteBuffer.putFloat(reSpeckLiveData.gyro.z)

                byteBuffer.putFloat(reSpeckLiveData.accelX)
                byteBuffer.putFloat(reSpeckLiveData.accelY)
                byteBuffer.putFloat(reSpeckLiveData.accelZ)
                byteBuffer.putFloat(reSpeckLiveData.gyro.x)
                byteBuffer.putFloat(reSpeckLiveData.gyro.y)
                byteBuffer.putFloat(reSpeckLiveData.gyro.z)
            }

            sendPostRequest(byteBuffer)
            Log.d(TAG,"function called")

        } catch (e: IOException) {
            // TODO Handle the exception
        }
    }

    fun sendPostRequest(data: ByteBuffer) {
        val arr = ByteArray(data.remaining())
        data.get(arr)

        val jsonArray = JSONArray(arr);

        var reqParam = URLEncoder.encode("liveData", "UTF-8") + "=" + jsonArray
        val mURL = URL("<Your API Link>")

        with(mURL.openConnection() as HttpURLConnection) {
            // optional default is GET
            requestMethod = "POST"

            val wr = OutputStreamWriter(getOutputStream());
            wr.write(reqParam);
            wr.flush();

            println("URL : $url")
            println("Response Code : $responseCode")

            BufferedReader(InputStreamReader(inputStream)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                println("Response from cloud: $response")

            }
        }
    }



    fun setupClickListeners() {
        menu_live=findViewById(R.id.live_live_button)
        menu_history=findViewById(R.id.live_history_button)
        menu_record=findViewById(R.id.live_record_button)
        menu_connect=findViewById(R.id.live_ble_button)
        val iconfont = Typeface.createFromAsset(assets, "iconfont.ttf")
        menu_live.setTypeface(iconfont)
        menu_history.setTypeface(iconfont)
        menu_record.setTypeface(iconfont)
        menu_connect.setTypeface(iconfont)

        menu_connect.setOnClickListener {
            val intent = Intent(this, ConnectingActivity::class.java)
            startActivity(intent)
        }

        menu_record.setOnClickListener {
            val intent = Intent(this, RecordingActivity::class.java)
            startActivity(intent)
        }

        menu_history.setOnClickListener {
            val intent = Intent(this, resultAnalysis::class.java)
            startActivity(intent)
        }

        menu_live.setOnClickListener {
            currnet_activity = "running"
            icon = R.drawable.running
        }
    }

    fun setupCurrentActivity() {
        imageView = findViewById(R.id.imageView_activity)
        var mTimeHandler: Handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == 0) {
                    cur_activity.setText(currnet_activity)
                    imageView.setImageResource(icon)
                    sendEmptyMessageDelayed(0, 500)
                }
            }
        }
        mTimeHandler.sendEmptyMessageDelayed(0, 500)
    }
//    override fun onDestroy() {
//        super.onDestroy()
//        unbindService(mServiceConnection)
//        mIsBound = false
//        unregisterReceiver(respeckLiveUpdateReceiver)
//        unregisterReceiver(thingyLiveUpdateReceiver)
//        looperRespeck.quit()
//        looperThingy.quit()
//    }
}
