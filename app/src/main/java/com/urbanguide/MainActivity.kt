package com.urbanguide

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.urbanguide.ui.theme.UrbanGuideTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.lang.Integer.parseInt


class MainActivity : ComponentActivity() {
    private lateinit var mqttManager: MQTTManager
    private val mqttEventSharedFlow = MutableSharedFlow<MqttEvent>()

    companion object {
        const val TAG = "Raoul"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ChannelName"
            val descriptionText = "Channel description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("0", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        setContent {
            UrbanGuideTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MenuSheetScaffold(mqttEventSharedFlow, mqttManager)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        mqttManager = MQTTManager(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {

                val topicEnum = Topics.fromTopic(topic)
                val payload = JSONObject(message.toString())
                when (topicEnum) {
                    Topics.DrawPoint -> {
                        lifecycleScope.launch {
                            val event = MqttEvent.DrawPointEvent(
                                title = payload.getString("title"),
                                position = LatLng(
                                    payload.getDouble("lat"),
                                    payload.getDouble("lang")
                                ),
                                topic = topic.orEmpty(),
                                timestamp_sent = payload.getString("timestamp")
                            )
                            mqttEventSharedFlow.emit(event)
                        }
                    }

                    Topics.DrawPointBatch -> {
                        lifecycleScope.launch {

                            val events: MutableList<MqttEvent.DrawPointEvent> = mutableListOf()

                            val rawlist = payload.getJSONArray("list")

                            for (i in 0 until rawlist.length()) {
                                val item = rawlist.getJSONObject(i)

                                val event = MqttEvent.DrawPointEvent(
                                    title = item.getString("title"),
                                    position = LatLng(
                                        item.getDouble("lat"),
                                        item.getDouble("lang")
                                    ),
                                    topic = topic.orEmpty(),
                                    timestamp_sent = item.getString("timestamp")
                                )
                                events.add(event)
                            }

                            val batchEvent = MqttEvent.DrawPointEventBatch(events = events, timestamp_sent = payload.getString("timestamp"))

                            mqttEventSharedFlow.emit(batchEvent)
                        }
                    }

                    Topics.MoveMap -> {
                        lifecycleScope.launch {
                            val event = MqttEvent.MoveMapEvent(
                                position = LatLng(
                                    payload.getDouble("lat"),
                                    payload.getDouble("lang")
                                ),
                                topic = topic.orEmpty(),
                                timestamp_sent = payload.getString("timestamp")
                            )
                            mqttEventSharedFlow.emit(event)
                        }
                    }

                    Topics.InAppNotification -> {
                        lifecycleScope.launch {
                            val event = MqttEvent.InAppNotificationEvent(
                                title = payload.getString("title"),
                                text = payload.getString("text"),
                                topic = topic.orEmpty(),
                                timestamp_sent = payload.getString("timestamp")
                            )
                            sendNotification(this@MainActivity,event)
                        }
                    }
                    Topics.InAppAlert -> {
                        lifecycleScope.launch {
                            val event = MqttEvent.InAppAlertEvent(
                                text = payload.getString("text"),
                                topic = topic.orEmpty(),
                                timestamp_sent = payload.getString("timestamp")
                            )
                            mqttEventSharedFlow.emit(event)
                        }
                    }

                    Topics.Unmanaged -> {}
                }
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(MQTTManager.TAG, "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(MQTTManager.TAG, "Delivery Complete")
            }

        })

        mqttManager.subscribe("AndroidKotlin${Topics.InAppAlert}Receive")
        mqttManager.subscribe("AndroidKotlin${Topics.InAppNotification}Receive")

    }

    fun sendNotification(context: Context, event: MqttEvent.InAppNotificationEvent) {
        val startTime = System.nanoTime()
        val builder = NotificationCompat.Builder(context, "0")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(event.title)
            .setContentText(event.text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(parseInt(event.timestamp_sent.takeLast(5)), builder.build())
        }
        val elapsedTime = System.nanoTime() - startTime
        val mqttPayload = "${event.timestamp_sent},Android,Kotlin,-,${Topics.InAppNotification},0,0,$elapsedTime"
        mqttManager.publish("AndroidKotlin${Topics.InAppNotification}Complete", mqttPayload)
        Log.d("Performance", "event sent: $mqttPayload")
    }
}

@OptIn(ExperimentalCoroutinesApi::class,ExperimentalMaterial3Api::class)
@Composable
fun MenuSheetScaffold(mqttEventSharedFlow: MutableSharedFlow<MqttEvent>, mqttManager: MQTTManager) {

    val snackbarHostState = remember { SnackbarHostState() }

    val scaffoldState = rememberBottomSheetScaffoldState(
        SheetState(
            skipHiddenState = true,
            skipPartiallyExpanded = false,
            initialValue = SheetValue.PartiallyExpanded
        )
    )
    val scope = rememberCoroutineScope()

    var mapData by remember { mutableStateOf<List<DataBeam>>(emptyList()) }

    val onButtonClicked: (Category) -> Unit = { category ->
        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
        mapData = DataRepository.getData(category)
        Log.d(MainActivity.TAG, mapData.toString())
    }

    val mqttEvents = mqttEventSharedFlow.asSharedFlow()

    LaunchedEffect(key1 = mqttEvents) {
        mqttEvents.collect { mqttEvent ->

            when (mqttEvent) {
                is MqttEvent.InAppAlertEvent -> {
                    val startTime = System.nanoTime()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "${mqttEvent.text}${mqttEvent.timestamp_sent}",
                            duration = SnackbarDuration.Short
                        )
                    }
                    val elapsedTime = System.nanoTime() - startTime
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val mqttPayload = "${mqttEvent.timestamp_sent},Android,Kotlin,-,${Topics.InAppAlert},0,0,$elapsedTime"
                    mqttManager.publish("AndroidKotlin${Topics.InAppAlert}Complete",mqttPayload)
                    Log.d("Performance", "payload: $mqttPayload")
                }
                else -> {}
            }
        }
    }


    BottomSheetScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        scaffoldState = scaffoldState,
        sheetContent = {
            MenuScaffoldContent(onButtonClicked = onButtonClicked)
        },
        sheetPeekHeight = 100.dp
    ) {
        // Main content of your screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp)
        ) {
            MapBoxComponent(mapData, mqttEventSharedFlow, mqttManager)
        }
    }
}

@Composable
fun MenuScaffoldContent(onButtonClicked: (Category) -> Unit) {
    val categories = CategoryDataProvider.getCategories()


    Column(modifier = Modifier.fillMaxWidth()) {

        categories.forEach { category ->
            Text(
                text = category.title,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(8.dp)
            )
            CategoryButtons(buttons = category.buttons, onButtonClicked = onButtonClicked)
        }
    }
}

@Composable
fun CategoryButtons(
    buttons: List<Category>,
    maxButtonsPerRow: Int = 3,
    onButtonClicked: (Category) -> Unit
) {
    val rows = buttons.chunked(maxButtonsPerRow)
    Column {
        rows.forEach { rowButtons ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                rowButtons.forEach { buttonItem ->
                    Button(onClick = {
                        onButtonClicked(buttonItem)
                    }) {
                        Text(text = buttonItem.label)
                    }
                }
            }
        }
    }

}

