package com.pro.aifitnesstracker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import kotlin.random.Random

/**
 * BroadcastReceiver triggered daily to check for unused app features
 * and send customized, localized notifications with content-specific icons.
 */
class FeatureRecommendationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefsManager = SharedPrefsManager(context)
        val unusedFeatures = getUnusedFeatures(prefsManager)

        if (unusedFeatures.isNotEmpty()) {
            // Pick exactly one unused feature at random
            val selectedFeature = unusedFeatures.random()
            
            // Pick one of three template styles at random
            val templateIndex = Random.nextInt(3)
            val lang = prefsManager.getLanguage()

            val title = getLocalizedTitle(lang)
            val message = getLocalizedMessage(selectedFeature, templateIndex, lang)
            val iconResId = getFeatureIconResource(selectedFeature)

            val targetRoute = when (selectedFeature) {
                "yoga" -> "YOGA"
                "fasting" -> "FASTING_TRACKER"
                "diet" -> "DIET_TRACKER"
                "workout" -> "WORKOUT_PLANNER"
                "run" -> "RUN_TRACKER"
                "breathing" -> "BREATHING"
                "heart_rate" -> "HEART_RATE"
                "sleep" -> "SLEEP"
                else -> "DASHBOARD"
            }
            showNotification(context, title, message, iconResId, targetRoute)
        }

        // Schedule next check for tomorrow
        scheduleRecommendationAlarm(context)
    }

    private fun getUnusedFeatures(prefsManager: SharedPrefsManager): List<String> {
        val features = listOf("yoga", "fasting", "diet", "workout", "run", "breathing", "heart_rate", "sleep")
        return features.filter { !prefsManager.isFeatureUsed(it) }
    }

    private fun getFeatureIconResource(feature: String): Int {
        return when (feature) {
            "yoga" -> R.drawable.ic_feature_yoga
            "fasting" -> R.drawable.ic_feature_fasting
            "diet" -> R.drawable.ic_feature_diet
            "workout" -> R.drawable.ic_feature_workout
            "run" -> R.drawable.ic_feature_run
            "breathing" -> R.drawable.ic_feature_breathing
            "heart_rate" -> R.drawable.ic_feature_heart_rate
            "sleep" -> R.drawable.ic_feature_sleep
            else -> R.drawable.app_logo
        }
    }

    private fun getLocalizedTitle(lang: String): String {
        return when (lang) {
            "hi" -> "✨ कुछ नया और स्वस्थ आज़माएं!"
            "mr" -> "✨ एक नवीन निरोगी सवय सुरू करा!"
            else -> "✨ Try a New Healthy Habit!"
        }
    }

    private fun getLocalizedMessage(feature: String, templateIndex: Int, lang: String): String {
                val translations = mapOf(
            "yoga" to listOf(
                // English templates
                listOf(
                    "Stretch and relax! Have you tried our Yoga sessions yet?",
                    "Improve your flexibility and posture today. Give Yoga a try!",
                    "Take 10 minutes for mindfulness. Explore peaceful Yoga poses now."
                ),
                // Hindi templates
                listOf(
                    "स्ट्रेच करें और तनाव मुक्त हों! क्या आपने हमारे योगा सत्र देखे हैं?",
                    "लचीलापन और मुद्रा सुधारें. आज ही योगासन आज़माएं!",
                    "मानसिक शांति के लिए 10 मिनट निकालें. आसान योग मुद्राओं को एक्सप्लोर करें."
                ),
                // Marathi templates
                listOf(
                    "स्ट्रेच करा आणि तणावमुक्त व्हा! आमचे योगा सत्र नक्की ट्राय करा.",
                    "शरीराची लवचिकता वाढवा. आजच योगासने करून पहा!",
                    "मन शांत ठेवण्यासाठी 10 मिनिटे द्या. योगासनांचा सराव करा."
                )
            ),
            "fasting" to listOf(
                // English
                listOf(
                    "Fasting is great for energy. Check out the Intermittent Fasting tracker!",
                    "Set your fasting goals today and track your progress live.",
                    "Give your digestive system a break. Start a simple fast now."
                ),
                // Hindi
                listOf(
                    "फास्टिंग ऊर्जा बढ़ाने के लिए बेहतरीन है. इंटरमिटेंट फास्टिंग ट्रैकर देखें!",
                    "आज ही अपने फास्टिंग गोल्स सेट करें और लाइव प्रोग्रेस ट्रैक करें.",
                    "पाचन तंत्र को आराम दें. अभी एक साधारण फास्टिंग शुरू करें."
                ),
                // Marathi
                listOf(
                    "उपवास आरोग्यासाठी आणि ऊर्जेसाठी उत्तम आहे. फास्टिंग ट्रॅकर पहा!",
                    "आजच तुमचे उपवासाचे ध्येय ठरवा आणि प्रगती ट्रॅक करा.",
                    "पचनसंस्थेला विश्रांती द्या. आजच फास्टिंग सुरू करा."
                )
            ),
            "diet" to listOf(
                // English
                listOf(
                    "Log meals to understand your calorie intake and hit your macro targets.",
                    "Healthy eating fuels your workouts! Record your meals in the Diet Tracker.",
                    "Track your nutrition today! It only takes 30 seconds to log food."
                ),
                // Hindi
                listOf(
                    "कैलोरी और मैक्रोज़ समझने के लिए भोजन लॉग करें.",
                    "सही पोषण से वर्कआउट बेहतर होता है! डाइट ट्रैकर में खाना दर्ज करें.",
                    "आज ही अपना पोषण ट्रैक करें! खाना लॉग करने में सिर्फ 30 सेकंड लगते हैं."
                ),
                // Marathi
                listOf(
                    "कॅलरी आणि पोषण समजण्यासाठी जेवणाची नोंद ठेवा.",
                    "चांगला आहार व्यायाम अधिक प्रभावी करतो! डाएट ट्रॅकर वापरा.",
                    "आजच तुमचे पोषण ट्रॅक करा! जेवण नोंदवायला फक्त 30 सेकंद लागतात."
                )
            ),
            "workout" to listOf(
                // English
                listOf(
                    "Ready to sweat? Use the Workout Planner to get customized fitness sets.",
                    "Build muscles and burn calories! Explore our workout routine library.",
                    "Create your personalized custom workout plan today."
                ),
                // Hindi
                listOf(
                    "पसीना बहाने के लिए तैयार? वर्कआउट प्लानर से कस्टमाइज्ड फिटनेस सेट्स पाएं.",
                    "मसल्स बनाएं और कैलोरी बर्न करें! वर्कआउट लाइब्रेरी एक्सप्लोर करें.",
                    "आज ही अपना पर्सनलाइज्ड वर्कआउट प्लान तैयार करें."
                ),
                // Marathi
                listOf(
                    "व्यायामासाठी तयार आहात? वर्कआउट प्लॅनरसह सानुकूलित व्यायाम मिळवा.",
                    "स्नायू मजबूत करा आणि कॅलरीज बर्न करा! वर्कआउट रूटीन पहा.",
                    "आजच तुमचा वैयक्तिक वर्कआउट प्लॅन तयार करा."
                )
            ),
            "run" to listOf(
                // English
                listOf(
                    "Map your route and track speed. Start the Run Tracker today!",
                    "Let's step outside! Run, walk, or jog with real-time GPS tracking.",
                    "Cardio boosts heart health. Launch a run tracking session now!"
                ),
                // Hindi
                listOf(
                    "रूट मैप करें और स्पीड मापें. आज ही रन ट्रैकर शुरू करें!",
                    "बाहर निकलें! रियल-टाइम जीपीएस ट्रैकिंग के साथ दौड़ें या टहलें.",
                    "कार्डियो दिल को स्वस्थ रखता है. अभी रन ट्रैकिंग शुरू करें!"
                ),
                // Marathi
                listOf(
                    "तुमचा मार्ग आणि वेग मोजा. आजच रन ट्रॅकर सुरू करा!",
                    "बाहेर पडा! रिअल-टाइम GPS ट्रॅकिंगसह चाला किंवा धावा.",
                    "कार्डिओ हृदयाचे आरोग्य सुधारते. आताच रनिंग सुरू करा!"
                )
            ),
            "breathing" to listOf(
                // English
                listOf(
                    "Take a deep breath. A 3-minute breathing exercise can reduce stress.",
                    "Pause and reset. Follow the calming breathing animation for instant peace.",
                    "Boost lung capacity and focus. Start a quick breathing session!"
                ),
                // Hindi
                listOf(
                    "गहरी सांस लें. 3 मिनट का ब्रीदिंग व्यायाम तनाव को कम कर सकता है.",
                    "रुकें और मन शांत करें. शांति के लिए ब्रीदिंग एनीमेशन फॉलो करें.",
                    "फेफड़ों की क्षमता और फोकस बढ़ाएं. त्वरित ब्रीदिंग सत्र शुरू करें!"
                ),
                // Marathi
                listOf(
                    "दीर्घ श्वास घ्या. 3 मिनिटांचा श्वासोच्छ्वास व्यायाम तणाव कमी करतो.",
                    "शांत व्हा. झटपट मनःशांतीसाठी ब्रीदिंग अ‍ॅनिमेशन वापरा.",
                    "फुफ्फुसांची क्षमता वाढवा. आत्ताच ब्रीदिंग सेशन सुरू करा!"
                )
            ),
            "heart_rate" to listOf(
                // English
                listOf(
                    "Track your pulse! Use the Heart Rate monitor to check your vitals.",
                    "Keep your cardio in check. Measure your resting heart rate now.",
                    "Check your heart rate zones after activity to optimize endurance."
                ),
                // Hindi
                listOf(
                    "अपनी धड़कन जांचें! हार्ट रेट मॉनिटर से अपने वाइटल्स चेक करें.",
                    "कार्डियो पर नज़र रखें. अभी अपना रेस्टिंग हार्ट रेट मापें.",
                    "व्यायाम के बाद अपने हार्ट रेट ज़ोन को चेक करें."
                ),
                // Marathi
                listOf(
                    "हृदयाचे ठोके तपासा! हार्ट रेट मॉनिटरने आरोग्य तपासा.",
                    "तुमचे रेस्टिंग हार्ट रेट आताच मोजा.",
                    "व्यायामानंतर तुमचे हार्ट रेट झोन तपासा."
                )
            ),
            "sleep" to listOf(
                // English
                listOf(
                    "Sleep well to recover faster. Try the Sleep Optimizer tonight!",
                    "Set your sleep goals and track your bedtime consistency patterns.",
                    "Get insights into your sleep quality and improve your rest cycles."
                ),
                // Hindi
                listOf(
                    "अच्छी रिकवरी के लिए अच्छी नींद लें. आज रात स्लीप ऑप्टिमाइज़र आज़माएं!",
                    "अपने स्लीप गोल्स सेट करें और सोने के समय की निरंतरता ट्रैक करें.",
                    "अपनी नींद की गुणवत्ता की जानकारी पाएं और बेहतर आराम करें."
                ),
                // Marathi
                listOf(
                    "चांगल्या आरोग्यासाठी शांत झोप घ्या. आज रात्री स्लीप ऑप्टिमाइझर वापरा!",
                    "झोपेचे ध्येय निश्चित करा आणि झोपण्याच्या वेळेचा नमुना ट्रॅक करा.",
                    "झोपेच्या गुणवत्तेबद्दल माहिती मिळवा आणि विश्रांती सुधारा."
                )
            )
        )

        val langGroup = translations[feature] ?: return ""
        val langIndex = when (lang) {
            "hi" -> 1
            "mr" -> 2
            else -> 0
        }
        val templates = langGroup.getOrNull(langIndex) ?: langGroup[0] // Fallback to English if index out of bounds
        return templates.getOrNull(templateIndex) ?: templates[0]
    }

    private fun showNotification(context: Context, title: String, message: String, iconResId: Int, targetRoute: String) {
        val helper = NotificationHelper(context)
        helper.showCustomNotification(
            title = title,
            message = message,
            badgeText = "HABIT",
            notificationId = 3003,
            channelId = "feature_recommendations",
            channelName = "Feature Recommendations",
            navigateTo = targetRoute
        )
    }

    private fun getVectorBitmap(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(96),
            drawable.intrinsicHeight.coerceAtLeast(96),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        private const val REQUEST_CODE = 8888

        /**
         * Schedule the next check alarm at 6:30 PM (18:30:00).
         */
        fun scheduleRecommendationAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, FeatureRecommendationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If 6:30 PM has already passed today, schedule for tomorrow
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }
}
