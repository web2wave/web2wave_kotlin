package com.web2wave

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.web2wave.lib.Web2Wave
import com.web2wave.lib.Web2WaveWebListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity(), Web2WaveWebListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Web2Wave.initWith("your api key")
        runBlocking(Dispatchers.IO) {
            val status = Web2Wave.fetchSubscriptionStatus("your user id")
            println("W2W_status: $status")

            val activeSubs = Web2Wave.hasActiveSubscription("your user id")
            println("W2W_activeSubs: $activeSubs")

            val subscriptions = Web2Wave.fetchSubscriptions("your user id")
            println("W2W_subs: $subscriptions")

            val updateResult = Web2Wave.updateUserProperty(
                "your user id",
                "age",
                "11"
            )

            println("W2W_update_result: $updateResult")

            val userProp = Web2Wave.fetchUserProperties("your user id")
            println("W2W_properties: $userProp")

            val cancelResult = Web2Wave.cancelSubscription(
                "pay system id",
                "no_money"
            )
            println("W2W_cancel_result: $cancelResult")

            val chargeResult = Web2Wave.chargeUser("your user id", 22057)
            println("W2W_charge_result: $chargeResult")
        }

        Web2Wave.showWebView(
            supportFragmentManager,
            "https://app.web2wave.com/",
            this
        )
    }

    override fun onEvent(event: String, data: Map<String, Any>?) {
        println("W2W_event_$event")
    }

    override fun onClose(data: Map<String, Any>?) {
        println("W2W_on_close_$data")
    }

    override fun onQuizFinished(data: Map<String, Any>?) {
        println("W2W_on_quiz_finished$data")
    }
}