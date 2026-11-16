package com.pro.aifitnesstracker.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Calendar
import com.pro.aifitnesstracker.R

data class Quote(
    val english: String,
    val hindi: String,
    val marathi: String,
    val author: String,
    val icon: ImageVector,
    val bgResId: Int
)

object MotivationalQuotes {
    val quotes = listOf(
        // Sunday (Rest & Recharge)
        Quote(
            "True health is not only about working hard, it is also about letting your body rest and recover.",
            "आराम भी स्वास्थ्य का हिस्सा है। आज अपने शरीर को रिचार्ज करें और नए सप्ताह के लिए तैयार हों।",
            "आराम देखील आरोग्याचा एक भाग आहे. आज आपल्या शरीराला रिचार्ज करा आणि नवीन आठवड्यासाठी तयार व्हा.",
            "Unknown",
            Icons.Rounded.Bedtime,
            R.drawable.quote_bg_sun
        ),
        // Monday (Starting & Momentum)
        Quote(
            "The secret of getting ahead is getting started. Make this Monday count!",
            "आगे बढ़ने का रहस्य है शुरुआत करना। इस सोमवार को अपने स्वास्थ्य के लिए एक कदम बढ़ाएं!",
            "पुढे जाण्याचे रहस्य म्हणजे सुरुवात करणे. या सोमवारी आपल्या आरोग्यासाठी एक पाऊल पुढे टाका!",
            "Mark Twain",
            Icons.Rounded.DirectionsRun,
            R.drawable.quote_bg_mon
        ),
        // Tuesday (Commitment & Strength)
        Quote(
            "Your body can stand almost anything. It's your mind that you have to convince.",
            "आपका शरीर लगभग कुछ भी सहन कर सकता है। आपको बस अपने दिमाग को लगातार प्रयास के लिए राजी करना है।",
            "तुमचे शरीर काहीही सहन करू शकते. तुम्हाला फक्त तुमच्या मनाला तयार करावे लागेल.",
            "Unknown",
            Icons.Rounded.FitnessCenter,
            R.drawable.quote_bg_tue
        ),
        // Wednesday (Nutrition & Care)
        Quote(
            "Good health is a relationship between you and your body. Nourish and hydrate it daily.",
            "अच्छा स्वास्थ्य आपके और आपके शरीर के बीच का रिश्ता है। इसे रोज़ संतुलित आहार और पानी से सींचें।",
            "उत्तम आरोग्य म्हणजे तुमच्या आणि तुमच्या शरीरामधील नाते आहे. रोज त्याला संतुलित आहार आणि पाण्याने समृद्ध करा.",
            "Unknown",
            Icons.Rounded.LocalDrink,
            R.drawable.quote_bg_wed
        ),
        // Thursday (Consistency & Progress)
        Quote(
            "Every step you take is a vote for the healthy and energetic person you want to become.",
            "आपका हर एक कदम आपके आने वाले बेहतर और ऊर्जावान स्वास्थ्य के लिए एक vote है।",
            "तुमचे प्रत्येक पाऊल हे तुम्हाला भविष्यात बनायचे असलेल्या निरोगी आणि ऊर्जावान व्यक्तीसाठी एक मत आहे.",
            "James Clear",
            Icons.Rounded.DirectionsWalk,
            R.drawable.quote_bg_thu
        ),
        // Friday (Persistence)
        Quote(
            "Slow progress is still progress. Don't stop when you are tired, stop when you are done.",
            "धीमी प्रगति भी प्रगति है। थकने पर मत रुकें, अपने आज के लक्ष्य को पूरा करके ही दम लें।",
            "हळूहळू होणारी प्रगती ही देखील प्रगतीच आहे. थकल्यावर थांबू नका, तुमचे ध्येय पूर्ण झाल्यावरच थांबा.",
            "Unknown",
            Icons.Rounded.TrendingUp,
            R.drawable.quote_bg_fri
        ),
        // Saturday (Lifestyle & Habits)
        Quote(
            "Health is the greatest wealth. An active weekend leads to an energetic and successful week.",
            "स्वास्थ्य ही सबसे बड़ा धन है। एक सक्रिय सप्ताहांत आपको पूरे सप्ताह ऊर्जावान रखेगा।",
            "आरोग्य हीच खरी संपत्ती आहे. एक सक्रिय वीकेंड तुम्हाला संपूर्ण आठवडाभर ऊर्जावान ठेवेल.",
            "Virgil",
            Icons.Rounded.Favorite,
            R.drawable.quote_bg_sat
        )
    )

    fun getQuoteForToday(): Quote {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 7 = Saturday
        val index = (day - 1).coerceIn(0, quotes.size - 1)
        return quotes[index]
    }
}
