package com.specknet.pdiotapp.live

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.RecordingActivity
import com.specknet.pdiotapp.bluetooth.BluetoothSpeckService
import com.specknet.pdiotapp.bluetooth.ConnectingActivity
import com.specknet.pdiotapp.onboarding.OnBoardingActivity
import com.specknet.pdiotapp.resultAnalysis
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import com.specknet.pdiotapp.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class LiveDataActivity : AppCompatActivity() {
    private val TAG = this.javaClass.name
    lateinit var username: String

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
    val JSON = MediaType.get("application/json")

    private val predictionToActivity = HashMap<String, String>()
    private val acitivityToIcon = HashMap<String, Int>()


    var respeckDataArr = arrayOf<Array<Float>>()
    var thingyDataArr = arrayOf<Array<Float>>()


    var currnet_activity = "N/A"
    var icon = R.drawable.health_pic



    // permissions
    lateinit var permissionAlertDialog: AlertDialog.Builder

    val permissionsForRequest = arrayListOf<String>()

    var locationPermissionGranted = false
    var cameraPermissionGranted = false
    var readStoragePermissionGranted = false
    var writeStoragePermissionGranted = false

    // broadcast receiver
    val filter = IntentFilter()

    var isUserFirstTime = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)

        val bundle = this.intent.extras
        val str = bundle!!.getString("username")
        if (str != null) {
           username = str
        }


        // check whether the onboarding screen should be shown
        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.PREF_USER_FIRST_TIME)) {
            isUserFirstTime = false
        }
        else {
            isUserFirstTime = true
            sharedPreferences.edit().putBoolean(Constants.PREF_USER_FIRST_TIME, false).apply()
            val introIntent = Intent(this, OnBoardingActivity::class.java)
            startActivity(introIntent)
        }


        permissionAlertDialog = AlertDialog.Builder(this)

        setupPermissions()

        setupBluetoothService()

        // register a broadcast receiver for respeck status
        filter.addAction(Constants.ACTION_RESPECK_CONNECTED)
        filter.addAction(Constants.ACTION_RESPECK_DISCONNECTED)


        setupCharts()

        cur_activity = findViewById(R.id.current_activity)
        cur_activity.setText(currnet_activity)

        setupClickListeners()

        setupCurrentActivity()

        predictionToActivity.put("Climbing stairs", "Stairs")
        predictionToActivity.put("Descending stairs", "Stairs")
        predictionToActivity.put("Desk work", "Desk work")
        predictionToActivity.put("Lying down left", "Lying down")
        predictionToActivity.put("Lying down on back", "Lying down")
        predictionToActivity.put("Lying down right", "Lying down")
        predictionToActivity.put("Lying down on stomach", "Lying down")
        predictionToActivity.put("Movement", "Movement")
        predictionToActivity.put("Running", "Running")
        predictionToActivity.put("Sitting", "Sitting")
        predictionToActivity.put("Sitting bent backward", "Sitting")
        predictionToActivity.put("Sitting bent forward", "Sitting")
        predictionToActivity.put("Standing", "Standing")
        predictionToActivity.put("Walking at normal speed", "Walking")

        acitivityToIcon.put("Walking", R.drawable.walking)
        acitivityToIcon.put("Sitting", R.drawable.sitting)
        acitivityToIcon.put("Lying down", R.drawable.sleeping)
        acitivityToIcon.put("Desk work", R.drawable.desk)
        acitivityToIcon.put("Stairs", R.drawable.staris)
        acitivityToIcon.put("Movement", R.drawable.movement)
        acitivityToIcon.put("Standing", R.drawable.standing)
        acitivityToIcon.put("Running", R.drawable.running)


        val c = Calendar.getInstance().time
        println("Current time => $c")

        val df = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val formattedDate = df.format(c)

        checkDataForDateExists(username, formattedDate)




        /*

        var finalDataArr = arrayOf<Array<Float>>()

        for (i in 1..50) {
            var reading = arrayOf<Float>(0.2f,0.3f, 0.1f, 0.4f,
                0.2f,0.3f, 0.1f, 0.4f,
                0.2f,0.3f, 0.1f, 0.4f
                )
            finalDataArr += reading
        }

        sendPostRequest(finalDataArr);

        */

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


                    if (respeckDataArr.size >= 50 && thingyDataArr.size >= 50 ){
                        getActivity()
                    }
                    addRespeckData(liveData)

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


                    addThingyData(liveData)



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

    fun addRespeckData(reSpeckLiveData: RESpeckLiveData){
        var recordingData = arrayOf<Float>(
            reSpeckLiveData.accelX, reSpeckLiveData.accelY, reSpeckLiveData.accelZ,
            reSpeckLiveData.gyro.x, reSpeckLiveData.gyro.y, reSpeckLiveData.gyro.z,
        )

        respeckDataArr += recordingData
    }

    fun addThingyData(thingyLiveData: ThingyLiveData){
        var recordingData = arrayOf<Float>(
            thingyLiveData.accelX, thingyLiveData.accelY, thingyLiveData.accelZ,
            thingyLiveData.gyro.x, thingyLiveData.gyro.y, thingyLiveData.gyro.z,
        )

        thingyDataArr += recordingData
    }



    private fun getActivity(){
        try {

            var finalDataArr = arrayOf<Array<Float>>()

            for (i in 0..49) {
                var reading = respeckDataArr[i] + thingyDataArr[i]
                finalDataArr += reading
            }

            sendPostRequest(finalDataArr);

            Log.d(TAG,"function called")
            respeckDataArr = arrayOf<Array<Float>>()
            thingyDataArr = arrayOf<Array<Float>>()

        } catch (e: IOException) {
            // TODO Handle the exception
        }
    }


    fun sendPostRequest(data: Array<Array<Float>>) {

        val gson = Gson()
        val dataStr = gson.toJson(data).toString();

        val okHttpClient = OkHttpClient()

        val requestBody: RequestBody = RequestBody.create(JSON, dataStr)
        val request = Request.Builder()
            .method("POST", requestBody)
            .url("https://finalmodel-dbtkg2xr6q-ey.a.run.app")
            .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle this
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonData: String = response.body()!!.string()
                Log.d(TAG, jsonData)
                val Jobject = JSONObject(jsonData)
                currnet_activity = Jobject.getString("prediction")
                storeActivityReading(currnet_activity)
            }
        })

    }

    fun storeActivityReading(data: String){
        // CODE FOR GETTING DATE
        var field = predictionToActivity.get(data)
        //var field = data;
        // CODE FOR GETTING DATE
        val c = Calendar.getInstance().time
        println("Current time => $c")

        val df = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val formattedDate = df.format(c)

        val db = FirebaseFirestore.getInstance()

        var dateRef = db.collection("Users").document(username).collection("Data").document(formattedDate)

        if (field != null) {
            dateRef
                .update(field, FieldValue.increment(2))
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully updated!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
        }

        icon = acitivityToIcon.get(field)!!;


    }

    fun checkDataForDateExists(username: String, date: String ){
        val db = FirebaseFirestore.getInstance()

        var dateRef = db.collection("Users").document(username).collection("Data").document(date)

        dateRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document.exists()) {
                    Log.d(TAG, "Document exists!")
                } else {
                    Log.d(TAG, "Document does not exist!")
                    initialiseDataForDate(username, date)
                }
            } else {
                Log.d(TAG, "Failed with: ", task.exception)
            }
        }
    }

    fun initialiseDataForDate(username: String, date: String ) {
        val db = FirebaseFirestore.getInstance()

        // CODE FOR CREATING SAMPLE DATA
        val activities = hashMapOf(
            "Lying down" to 0,
            "Sitting" to 0,
            "Stairs" to 0,
            "Standing" to 0,
            "Walking" to 0,
            "Running" to 0,
            "Movement" to 0,
            "Desk work" to 0
        )

        var dateRef = db.collection("Users").document(username).collection("Data").document(date)

        // CODE FOR SETTING DATA FOR DATE
        dateRef
            .set(activities)
            .addOnSuccessListener(OnSuccessListener<Void?> {
                Log.d(
                    TAG,
                    "DocumentSnapshot added with ID: "
                )
            })
            .addOnFailureListener(OnFailureListener { e -> Log.w(TAG, "Error adding document", e) })


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
            val bundle = Bundle()
            bundle.putString("username",username)
            intent.putExtras(bundle)
            startActivity(intent)
            this.overridePendingTransition(0, 0)
        }

        menu_record.setOnClickListener {
            val intent = Intent(this, RecordingActivity::class.java)
            val bundle = Bundle()
            bundle.putString("username",username)
            intent.putExtras(bundle)
            startActivity(intent)
            this.overridePendingTransition(0, 0)
        }

        menu_history.setOnClickListener {
            val intent = Intent(this, resultAnalysis::class.java)
            val bundle = Bundle()
            bundle.putString("username",username)
            intent.putExtras(bundle)
            startActivity(intent)
            this.overridePendingTransition(0, 0)
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



    fun setupPermissions() {
        // request permissions

        // location permission
        Log.i("Permissions", "Location permission = " + locationPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsForRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsForRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        else {
            locationPermissionGranted = true
        }

        // camera permission
        Log.i("Permissions", "Camera permission = " + cameraPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Camera permission = " + cameraPermissionGranted)
            permissionsForRequest.add(Manifest.permission.CAMERA)
        }
        else {
            cameraPermissionGranted = true
        }

        // read storage permission
        Log.i("Permissions", "Read st permission = " + readStoragePermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Read st permission = " + readStoragePermissionGranted)
            permissionsForRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        else {
            readStoragePermissionGranted = true
        }

        // write storage permission
        Log.i("Permissions", "Write storage permission = " + writeStoragePermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Write storage permission = " + writeStoragePermissionGranted)
            permissionsForRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        else {
            writeStoragePermissionGranted = true
        }

        if (permissionsForRequest.size >= 1) {
            ActivityCompat.requestPermissions(this,
                permissionsForRequest.toTypedArray(),
                Constants.REQUEST_CODE_PERMISSIONS)
        }

    }

    fun setupBluetoothService() {
        val isServiceRunning = Utils.isServiceRunning(BluetoothSpeckService::class.java, applicationContext)
        Log.i("debug","isServiceRunning = " + isServiceRunning)

        // check sharedPreferences for an existing Respeck id
        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.RESPECK_MAC_ADDRESS_PREF)) {
            Log.i("sharedpref", "Already saw a respeckID, starting service and attempting to reconnect")

            // launch service to reconnect
            // start the bluetooth service if it's not already running
            if(!isServiceRunning) {
                Log.i("service", "Starting BLT service")
                val simpleIntent = Intent(this, BluetoothSpeckService::class.java)
                this.startService(simpleIntent)
            }
        }
        else {
            Log.i("sharedpref", "No Respeck seen before, must pair first")
            // TODO then start the service from the connection activity
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if(grantResults.isNotEmpty()) {
                for (i in grantResults.indices) {
                    when(permissionsForRequest[i]) {
                        Manifest.permission.ACCESS_COARSE_LOCATION -> locationPermissionGranted = true
                        Manifest.permission.ACCESS_FINE_LOCATION -> locationPermissionGranted = true
                        Manifest.permission.CAMERA -> cameraPermissionGranted = true
                        Manifest.permission.READ_EXTERNAL_STORAGE -> readStoragePermissionGranted = true
                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> writeStoragePermissionGranted = true
                    }

                }
            }
        }

        // count how many permissions need granting
        var numberOfPermissionsUngranted = 0
        if (!locationPermissionGranted) numberOfPermissionsUngranted++
        if (!cameraPermissionGranted) numberOfPermissionsUngranted++
        if (!readStoragePermissionGranted) numberOfPermissionsUngranted++
        if (!writeStoragePermissionGranted) numberOfPermissionsUngranted++

        // show a general message if we need multiple permissions
        if (numberOfPermissionsUngranted > 1) {
            val generalSnackbar = Snackbar
                .make(coordinatorLayout, "Several permissions are needed for correct app functioning", Snackbar.LENGTH_LONG)
                .setAction("SETTINGS") {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
                .show()
        }
        else if(numberOfPermissionsUngranted == 1) {
            var snackbar: Snackbar = Snackbar.make(coordinatorLayout, "", Snackbar.LENGTH_LONG)
            if (!locationPermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Location permission needed for Bluetooth to work.",
                        Snackbar.LENGTH_LONG
                    )
            }

            if(!cameraPermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Camera permission needed for QR code scanning to work.",
                        Snackbar.LENGTH_LONG
                    )
            }

            if(!readStoragePermissionGranted || !writeStoragePermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Storage permission needed to record sensor.",
                        Snackbar.LENGTH_LONG
                    )
            }

            snackbar.setAction("SETTINGS") {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                startActivity(settingsIntent)
            }
                .show()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.show_tutorial) {
            val introIntent = Intent(this, OnBoardingActivity::class.java)
            startActivity(introIntent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

}
