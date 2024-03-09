package com.urbanguide

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject


class MainActivity : ComponentActivity()  {
    private lateinit var mqttManager: MQTTManager
    private val mqttEventChannel = Channel<MqttEvent>(Channel.BUFFERED)

    companion object {
        const val TAG = "Raoul"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UrbanGuideTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MenuSheetScaffold(mqttEventChannel,mqttManager)
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
                        Topics.MoveMap -> {
                            lifecycleScope.launch {
                                val event = MqttEvent.MoveMapEvent(
                                    position = LatLng(payload.getDouble("lat"),payload.getDouble("lang")),
                                    topic = topic.orEmpty(),
                                    timestamp_sent = payload.getString("timestamp")
                                )
                                mqttEventChannel.send(event)
                            }
                        }
                        Topics.InAppNotification -> {
                            // Handle humidity message
                        }
                        Topics.InAppAlert -> {
                            // Handle light message
                        }
                        Topics.DrawPoint -> {
                            lifecycleScope.launch {
                                val event = MqttEvent.DrawPointEvent(
                                    title = payload.getString("title"),
                                    position = LatLng(payload.getDouble("lat"),payload.getDouble("lang")),
                                    topic = topic.orEmpty(),
                                    timestamp_sent = payload.getString("timestamp")
                                )
                                mqttEventChannel.send(event)
                            }
                        }
                        Topics.Unmanaged -> {}
                    }


                val currentTimeMillis = System.currentTimeMillis()
                Log.d(MQTTManager.TAG, "Receive message: ${message.toString()} from topic: $topic at time $currentTimeMillis")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(MQTTManager.TAG, "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(MQTTManager.TAG, "Delivery Complete")
            }

        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSheetScaffold(mqttEventChannel: Channel<MqttEvent>, mqttManager: MQTTManager) {

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
        scope.launch { scaffoldState.bottomSheetState.partialExpand()}
        mapData = DataRepository.getData(category)
        Log.d(MainActivity.TAG,mapData.toString())
    }



    BottomSheetScaffold(
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
            MapBoxComponent(mapData,mqttEventChannel,mqttManager)
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
fun CategoryButtons(buttons: List<Category>, maxButtonsPerRow: Int = 3, onButtonClicked: (Category) -> Unit) {
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

