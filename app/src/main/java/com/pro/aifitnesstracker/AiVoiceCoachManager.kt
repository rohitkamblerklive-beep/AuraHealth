package com.pro.aifitnesstracker

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.Locale

enum class VoiceCoachPersona(
    val id: String,
    val title: String,
    val hindiTitle: String,
    val tagLine: String,
    val avatarEmoji: String,
    val speechPitch: Float,
    val speechRate: Float
) {
    DESI_TRAINER(
        id = "DESI_TRAINER",
        title = "Desi Gym Trainer",
        hindiTitle = "देसी उस्ताद / जिम ट्रेनर",
        tagLine = "शाबाश चीते! दम लगा के, आज पूरा पसीना बहाना है!",
        avatarEmoji = "🏋️",
        speechPitch = 0.95f,
        speechRate = 1.0f
    ),
    DRILL_SERGEANT(
        id = "DRILL_SERGEANT",
        title = "Military Drill Sergeant",
        hindiTitle = "कड़क कमांडो सार्जेंट",
        tagLine = "No excuses, soldier! Push past your limits!",
        avatarEmoji = "🎖️",
        speechPitch = 0.85f,
        speechRate = 1.15f
    ),
    ZEN_MONK(
        id = "ZEN_MONK",
        title = "Zen Mindfulness Guide",
        hindiTitle = "शांत ज़ेन योग गुरु",
        tagLine = "Breathe in peace, exhale tension. Find your rhythm.",
        avatarEmoji = "🧘",
        speechPitch = 0.95f,
        speechRate = 0.85f
    ),
    UPBEAT_PRO(
        id = "UPBEAT_PRO",
        title = "Upbeat Pro Coach",
        hindiTitle = "एनर्जेटिक प्रो एथलीट",
        tagLine = "Crushing it, champion! Keep that high momentum alive!",
        avatarEmoji = "⚡",
        speechPitch = 1.10f,
        speechRate = 1.05f
    );

    companion object {
        fun fromId(id: String): VoiceCoachPersona {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: DESI_TRAINER
        }
    }
}

class AiVoiceCoachManager private constructor(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val prefsManager = SharedPrefsManager(context)

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                applyPersonaSettings(getCurrentPersona())
            }
        }
    }

    fun getCurrentPersona(): VoiceCoachPersona {
        return VoiceCoachPersona.fromId(prefsManager.getVoiceCoachPersona())
    }

    fun setPersona(persona: VoiceCoachPersona) {
        prefsManager.saveVoiceCoachPersona(persona.id)
        applyPersonaSettings(persona)
    }

    fun isEnabled(): Boolean {
        return prefsManager.isVoiceCoachEnabled()
    }

    fun setEnabled(enabled: Boolean) {
        prefsManager.setVoiceCoachEnabled(enabled)
    }

    private fun applyPersonaSettings(persona: VoiceCoachPersona) {
        val t = tts ?: return
        try {
            t.setPitch(persona.speechPitch)
            t.setSpeechRate(persona.speechRate)

            if (persona == VoiceCoachPersona.DESI_TRAINER) {
                val hiLocale = Locale.Builder().setLanguage("hi").setRegion("IN").build()
                val langResult = t.isLanguageAvailable(hiLocale)
                if (langResult >= TextToSpeech.LANG_AVAILABLE) {
                    t.language = hiLocale
                } else {
                    val inLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()
                    if (t.isLanguageAvailable(inLocale) >= TextToSpeech.LANG_AVAILABLE) {
                        t.language = inLocale
                    } else {
                        t.language = Locale.US
                    }
                }
            } else {
                t.language = Locale.US
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun speak(text: String, flush: Boolean = true, forced: Boolean = false) {
        if (!forced && !isEnabled()) return

        val t = tts ?: return
        if (!isTtsReady) return

        try {
            applyPersonaSettings(getCurrentPersona())
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val utteranceId = "coach_${System.currentTimeMillis()}"
            t.speak(text, queueMode, null, utteranceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isTtsReady = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Signature Audio Preview ──────────────────────────────────────────
    fun previewPersona(persona: VoiceCoachPersona) {
        val t = tts ?: return
        try {
            t.stop()
            t.setPitch(persona.speechPitch)
            t.setSpeechRate(persona.speechRate)

            val previewText = when (persona) {
                VoiceCoachPersona.DESI_TRAINER -> {
                    val hiLocale = Locale.Builder().setLanguage("hi").setRegion("IN").build()
                    if (t.isLanguageAvailable(hiLocale) >= TextToSpeech.LANG_AVAILABLE) {
                        t.language = hiLocale
                        "शाबाश चीते! मैं हूँ तुम्हारा देसी कोच! आज पूरा पसीना बहाएंगे, रुकना मना है!"
                    } else {
                        t.language = Locale.US
                        "Shaabash cheete! Main hoon tumhara Desi Coach! Aaj poora paseena bahayenge, rukna mana hai!"
                    }
                }
                VoiceCoachPersona.DRILL_SERGEANT -> {
                    t.language = Locale.US
                    "Listen up, recruit! I am your Drill Sergeant. Excuses burn zero calories, so let's move!"
                }
                VoiceCoachPersona.ZEN_MONK -> {
                    t.language = Locale.US
                    "Peace be with you. I am your Zen Guide. Breathe deeply, ground your mind, and let's flow."
                }
                VoiceCoachPersona.UPBEAT_PRO -> {
                    t.language = Locale.US
                    "Hey champ! Ready to crush today's session? Let's bring that elite energy and do this!"
                }
            }

            val utteranceId = "preview_${persona.id}"
            t.speak(previewText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── GPS Outdoor Run / Walk Audio Triggers ────────────────────────────
    fun onRunStart(isWalking: Boolean) {
        val activity = if (isWalking) "walk" else "run"
        val message = when (getCurrentPersona()) {
            VoiceCoachPersona.DESI_TRAINER -> {
                if (isTtsSupportingHindi()) {
                    if (isWalking) "चलो चीते, वॉक शुरू! अपनी रफ़्तार बनाओ और आज का कदम लक्ष्य पूरा करो!"
                    else "चलो चीते, रनिंग शुरू! पूरी ताक़त से दौड़ो, आज का टारगेट फतह करना है!"
                } else {
                    if (isWalking) "Chalo cheete, walk shuru! Apni raftaar banao aur kadam lakshya poora karo!"
                    else "Chalo cheete, running shuru! Poori taakat se daudo, target fateh karna hai!"
                }
            }
            VoiceCoachPersona.DRILL_SERGEANT -> {
                "Move out, soldier! Begin your $activity! Keep your posture erect and maintain momentum!"
            }
            VoiceCoachPersona.ZEN_MONK -> {
                "Begin your $activity. Feel each step grounding you to the earth. Inhale peace, exhale tension."
            }
            VoiceCoachPersona.UPBEAT_PRO -> {
                "Let's go, champion! Your outdoor $activity has started. Find your groove and let's make it count!"
            }
        }
        speak(message)
    }

    fun onRunSplitDistance(km: Int, paceFormatted: String, isWalking: Boolean) {
        val message = when (getCurrentPersona()) {
            VoiceCoachPersona.DESI_TRAINER -> {
                if (isTtsSupportingHindi()) {
                    if (km == 1) "शाबाश चीते! 1 किलोमीटर पूरा! बहुत बढ़िया, यही पेस पकड़ के रखो!"
                    else "$km किलोमीटर फतह! शेर जैसी ताक़त दिखा रहे हो, रुकना नहीं है!"
                } else {
                    if (km == 1) "Shaabash cheete! 1 kilometer poora! Yahi pace pakad ke rakho!"
                    else "$km kilometer fateh! Sher jaisi taakat dikha rahe ho, rukna nahi hai!"
                }
            }
            VoiceCoachPersona.DRILL_SERGEANT -> {
                "$km kilometers completed, soldier! Current pace is $paceFormatted per kilometer. Don't you dare slow down!"
            }
            VoiceCoachPersona.ZEN_MONK -> {
                "$km kilometers covered with grace. Heart rate steady, breathing calm. Honor your journey."
            }
            VoiceCoachPersona.UPBEAT_PRO -> {
                "$km kilometers in the books! Pacing at $paceFormatted. You are in peak form, keep shining!"
            }
        }
        speak(message)
    }

    fun onRunTimeElapsed(minutes: Int) {
        val message = when (getCurrentPersona()) {
            VoiceCoachPersona.DESI_TRAINER -> {
                if (isTtsSupportingHindi()) {
                    "$minutes मिनट हो गए भाई! पसीना बह रहा है, फैट बर्न हो रहा है! जोश कम मत होने देना!"
                } else {
                    "$minutes minute ho gaye bhai! Paseena bah raha hai, fat burn ho raha hai! Josh high rakho!"
                }
            }
            VoiceCoachPersona.DRILL_SERGEANT -> {
                "$minutes minutes on the clock! Pain is temporary, pride is forever. Push through it, soldier!"
            }
            VoiceCoachPersona.ZEN_MONK -> {
                "$minutes minutes of mindful flow. Relax your shoulders and breathe gently through the rhythm."
            }
            VoiceCoachPersona.UPBEAT_PRO -> {
                "$minutes minutes strong! Your endurance is incredible today, love the dedication!"
            }
        }
        speak(message)
    }

    fun onRunPause() {
        val message = when (getCurrentPersona()) {
            VoiceCoachPersona.DESI_TRAINER -> {
                if (isTtsSupportingHindi()) "वर्कआउट पॉज़ किया गया है। थोड़ा सांस ले लो चीते, पानी का घूंट पियो!"
                else "Workout pause kiya gaya hai. Thoda saans le lo cheete, paani piyo!"
            }
            VoiceCoachPersona.DRILL_SERGEANT -> {
                "Hold position! Quick breather, but keep your head in the game soldier!"
            }
            VoiceCoachPersona.ZEN_MONK -> {
                "Pausing your journey. Take three deep breaths and recharge your inner calm."
            }
            VoiceCoachPersona.UPBEAT_PRO -> {
                "Taking a quick pit stop! Shake it out, we'll dive right back in."
            }
        }
        speak(message)
    }

    fun onRunResume() {
        val message = when (getCurrentPersona()) {
            VoiceCoachPersona.DESI_TRAINER -> {
                if (isTtsSupportingHindi()) "चलो फिर से शुरू! जोश पूरा हाई होना चाहिए, लेट्स गो!"
                else "Chalo fir se shuru! Josh poora high hona chahiye, let's go!"
            }
            VoiceCoachPersona.DRILL_SERGEANT -> {
                "Break over! Back on your feet, double time, move move move!"
            }
            VoiceCoachPersona.ZEN_MONK -> {
                "Resuming our flow. Step forward with presence and balance."
            }
            VoiceCoachPersona.UPBEAT_PRO -> {
                "We're back! Let's bring the energy and finish strong!"
            }
        }
        speak(message)
    }

    fun onRunFinish(distanceKm: Float, minutes: Int, calories: Int) {
        val distFormatted = String.format(Locale.US, "%.2f", distanceKm)
        val message = when (getCurrentPersona()) {
            VoiceCoachPersona.DESI_TRAINER -> {
                if (isTtsSupportingHindi()) {
                    "क्या बात है मेरे शेर! रनिंग खत्म! $distFormatted किलोमीटर और $calories कैलरी बर्न! आज तो तुमने कमाल कर दिया!"
                } else {
                    "Kya baat hai mere sher! Running complete! $distFormatted kilometer aur $calories calorie burn! Kamaal kar diya!"
                }
            }
            VoiceCoachPersona.DRILL_SERGEANT -> {
                "Mission accomplished, soldier! $distFormatted kilometers down and $calories calories destroyed. Excellent discipline!"
            }
            VoiceCoachPersona.ZEN_MONK -> {
                "Journey completed. $distFormatted kilometers traversed in harmony. Be proud of what your body accomplished today."
            }
            VoiceCoachPersona.UPBEAT_PRO -> {
                "Workout complete! $distFormatted kilometers and $calories calories crushed! You are an absolute rockstar!"
            }
        }
        speak(message)
    }

    // ── Gym & AI Workout Session Audio Triggers ──────────────────────────
    fun onWorkoutStart(workoutName: String, firstExercise: String) {
        val message = when (getCurrentPersona()) {
            VoiceCoachPersona.DESI_TRAINER -> {
                if (isTtsSupportingHindi()) {
                    "चलो भाई, $workoutName शुरू! पहला सेट है $firstExercise. दम लगा के करना, कोई ढील नहीं!"
                } else {
                    "Chalo bhai, $workoutName shuru! Pehla exercise hai $firstExercise. Dam laga ke karna!"
                }
            }
            VoiceCoachPersona.DRILL_SERGEANT -> {
                "Listen up! Initiating $workoutName session! First objective: $firstExercise. Give me 100 percent!"
            }
            VoiceCoachPersona.ZEN_MONK -> {
                "Welcome to your $workoutName practice. Beginning gently with $firstExercise. Focus on mindful form."
            }
            VoiceCoachPersona.UPBEAT_PRO -> {
                "Let's get this pump! Starting $workoutName with $firstExercise. Let's make every single rep count!"
            }
        }
        speak(message)
    }

    fun onExerciseCompleted(nextExercise: String?, remainingCount: Int) {
        val message = if (nextExercise != null) {
            when (getCurrentPersona()) {
                VoiceCoachPersona.DESI_TRAINER -> {
                    if (isTtsSupportingHindi()) {
                        "शाबाश! ये सेट पूरा! अगला अभ्यास है $nextExercise, बिल्कुल ढीले नहीं पड़ना चीते!"
                    } else {
                        "Shaabash! Set complete! Agla exercise hai $nextExercise, dheele nahi padna cheete!"
                    }
                }
                VoiceCoachPersona.DRILL_SERGEANT -> {
                    "Set completed! Next mission: $nextExercise. Catch your breath and lock in!"
                }
                VoiceCoachPersona.ZEN_MONK -> {
                    "Mindful completion. Transition gently into $nextExercise. Maintain your calm breathing."
                }
                VoiceCoachPersona.UPBEAT_PRO -> {
                    "Crushed that set! Moving right along into $nextExercise. You are in total control!"
                }
            }
        } else {
            when (getCurrentPersona()) {
                VoiceCoachPersona.DESI_TRAINER -> {
                    if (isTtsSupportingHindi()) {
                        "अरे वाह चीते! सारे सेट्स पूरे! अब फिनिश बटन दबाओ और अपना रिजल्ट देखो!"
                    } else {
                        "Wah cheete! Saare exercises complete! Finish dabao aur apna result dekho!"
                    }
                }
                VoiceCoachPersona.DRILL_SERGEANT -> {
                    "All tactical exercises neutralized! Outstanding performance, recruit. Report completion!"
                }
                VoiceCoachPersona.ZEN_MONK -> {
                    "All postures fulfilled with beauty. Your spirit and body are aligned. Well done."
                }
                VoiceCoachPersona.UPBEAT_PRO -> {
                    "Boom! You conquered the entire workout! Hit finish and celebrate your victory!"
                }
            }
        }
        speak(message)
    }

    fun onWorkoutPause() {
        val message = when (getCurrentPersona()) {
            VoiceCoachPersona.DESI_TRAINER -> {
                if (isTtsSupportingHindi()) "वर्कआउट पॉज़ हुआ। गहरी सांस लो और पानी पियो!"
                else "Workout paused. Gehri saans lo aur paani piyo!"
            }
            VoiceCoachPersona.DRILL_SERGEANT -> "Session on hold! Stay focused, soldier!"
            VoiceCoachPersona.ZEN_MONK -> "Rest and breathe deeply. Restore your energy."
            VoiceCoachPersona.UPBEAT_PRO -> "Quick break! Take a sip of water, we'll get right back to it."
        }
        speak(message)
    }

    fun onWorkoutResume() {
        val message = when (getCurrentPersona()) {
            VoiceCoachPersona.DESI_TRAINER -> {
                if (isTtsSupportingHindi()) "चलो वापस मैदान में! अगले सेट के लिए तैयार!"
                else "Chalo wapas maidaan mein! Agle set ke liye taiyaar!"
            }
            VoiceCoachPersona.DRILL_SERGEANT -> "Resume training! No slacking allowed!"
            VoiceCoachPersona.ZEN_MONK -> "Re-entering your flow with renewed energy."
            VoiceCoachPersona.UPBEAT_PRO -> "Back to it! Let's keep that fire burning!"
        }
        speak(message)
    }

    private fun isTtsSupportingHindi(): Boolean {
        val t = tts ?: return false
        val hiLocale = Locale.Builder().setLanguage("hi").setRegion("IN").build()
        return try {
            t.isLanguageAvailable(hiLocale) >= TextToSpeech.LANG_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AiVoiceCoachManager? = null

        fun getInstance(context: Context): AiVoiceCoachManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AiVoiceCoachManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
