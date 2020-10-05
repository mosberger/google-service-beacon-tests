package ch.mosberger.new_b

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.*
import io.reactivex.rxjava3.subjects.BehaviorSubject
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import timber.log.Timber
import java.util.*

@RuntimePermissions
class MainActivity : AppCompatActivity() {
    private val data = BehaviorSubject.create<Iterable<String>>()
    private val listener = SimpleMessageListener {
        data.onNext(
            it.plus(if (data.hasValue()) data.value else emptyList()).take(10)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        Timber.plant(Timber.DebugTree())

        data.subscribe {
            findViewById<TextView>(R.id.logList)?.text = it.joinToString(separator = "\n\n")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startScanningWithBackgroundWithPermissionCheck()
        } else {
            startScanningWithPermissionCheck()
        }
    }

    @NeedsPermission(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )
    fun startScanningWithBackground() {
        startScanningInternal()
    }

    @NeedsPermission(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    fun startScanning() {
        startScanningInternal()
    }

    private fun startScanningInternal() {
        val filter = MessageFilter.Builder()
            .includeIBeaconIds(
                UUID.fromString("ururi"),
                null,
                null
            )
            .build()

        val options = SubscribeOptions.Builder()
            .setStrategy(Strategy.BLE_ONLY)
            .setFilter(filter)
            .build()

        Nearby.getMessagesClient(this).subscribe(listener, options)
    }
}

class SimpleMessageListener(val onChange: (List<String>) -> Unit) : MessageListener() {
    override fun onDistanceChanged(p0: Message?, p1: Distance?) {
        super.onDistanceChanged(p0, p1)
        Timber.i("onDistanceChanged($p0, $p1)")
        onChange(listOf("onDistanceChanged(${parseBeacon(p0)}, $p1)"))
    }

    override fun onLost(p0: Message?) {
        super.onLost(p0)
        Timber.i("onLost($p0)")
        onChange(listOf("onLost(${parseBeacon(p0)})"))
    }

    override fun onFound(p0: Message?) {
        super.onFound(p0)
        Timber.i("onFound($p0)")
        onChange(listOf("onFound(${parseBeacon(p0)})"))
    }

    private fun parseBeacon(p0: Message?): Any? {
        if (Message.MESSAGE_TYPE_I_BEACON_ID == p0?.type) {
            return IBeaconId.from(p0)
        } else if (Message.MESSAGE_TYPE_EDDYSTONE_UID == p0?.type) {
            return EddystoneUid.from(p0)
        }
        return null
    }
}