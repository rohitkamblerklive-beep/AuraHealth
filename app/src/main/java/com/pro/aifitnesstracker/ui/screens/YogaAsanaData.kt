package com.pro.aifitnesstracker.ui.screens

// ── Category Constants ────────────────────────────────────────────────────
const val ASANA_CAT_MORNING     = "Morning Yoga"
const val ASANA_CAT_POWER       = "Power Yoga"
const val ASANA_CAT_FLEXIBILITY = "Flexibility"
const val ASANA_CAT_SLEEP       = "Sleep Yoga"
const val ASANA_CAT_STRESS      = "Stress Relief"
const val ASANA_CAT_CORE        = "Core & Strength"
const val ASANA_CAT_THERAPY     = "Therapeutic"

// ── Data Classes ──────────────────────────────────────────────────────────
data class YogaAsana(
    val id: String,
    val name: String,
    val sanskritName: String,
    val category: String,
    val level: String,          // "Beginner" | "Intermediate" | "Advanced"
    val holdSeconds: Int,
    val emoji: String,
    val steps: List<String>,
    val benefits: List<String>,
    val precautions: List<String>,
    val targetBodyPart: String,
    val caloriesPerMin: Float
)

data class YogaProgram(
    val id: String,
    val name: String,
    val description: String,
    val durationDays: Int,
    val emoji: String,
    val colorHex: Long,
    val poseIds: List<String>,
    val level: String
)

// ── 30 Yoga Asanas ────────────────────────────────────────────────────────
val allYogaAsanas = listOf(

    // ══════════════════ BEGINNER (10) ══════════════════

    YogaAsana(
        id = "tadasana", name = "Mountain Pose", sanskritName = "Tadasana",
        category = ASANA_CAT_MORNING, level = "Beginner", holdSeconds = 60, emoji = "🧍",
        steps = listOf(
            "Stand with feet together or hip-width apart.",
            "Press all four corners of your feet firmly into the mat.",
            "Engage thigh muscles and gently lift kneecaps.",
            "Draw tailbone down slightly, engage core lightly.",
            "Roll shoulders back and down, arms alongside body.",
            "Lengthen through the crown of your head and breathe steadily."
        ),
        benefits = listOf("Improves posture & alignment", "Strengthens thighs, knees & ankles", "Increases body awareness", "Improves balance"),
        precautions = listOf("Keep eyes open with low blood pressure", "Avoid prolonged holding with headaches"),
        targetBodyPart = "Full Body", caloriesPerMin = 1.5f
    ),

    YogaAsana(
        id = "balasana", name = "Child's Pose", sanskritName = "Balasana",
        category = ASANA_CAT_SLEEP, level = "Beginner", holdSeconds = 90, emoji = "🛐",
        steps = listOf(
            "Kneel on the mat with toes together, knees hip-width apart.",
            "Exhale and lower torso between thighs.",
            "Extend arms forward or rest them alongside body.",
            "Let forehead rest on the mat.",
            "Breathe deeply, feeling the stretch in lower back.",
            "Hold for 1-3 minutes, releasing tension with each exhale."
        ),
        benefits = listOf("Gently stretches hips & lower back", "Calms the nervous system", "Relieves stress and fatigue", "Great for bedtime routine"),
        precautions = listOf("Avoid with serious knee injuries", "Widen knees if pregnant"),
        targetBodyPart = "Back & Hips", caloriesPerMin = 1.2f
    ),

    YogaAsana(
        id = "adho_mukha", name = "Downward Dog", sanskritName = "Adho Mukha Svanasana",
        category = ASANA_CAT_MORNING, level = "Beginner", holdSeconds = 60, emoji = "🐕",
        steps = listOf(
            "Start on hands and knees, wrists below shoulders.",
            "Spread fingers wide, press palms firmly into the mat.",
            "Tuck toes and lift hips up and back forming an inverted V.",
            "Straighten legs as much as comfortable.",
            "Let head hang between arms, ears aligned with upper arms.",
            "Press heels toward floor and hold, breathing deeply."
        ),
        benefits = listOf("Stretches hamstrings & calves", "Strengthens arms & legs", "Energizes the whole body", "Relieves back pain"),
        precautions = listOf("Avoid with wrist or shoulder injuries", "Bend knees if hamstrings are tight"),
        targetBodyPart = "Full Body", caloriesPerMin = 3.0f
    ),

    YogaAsana(
        id = "cat_cow", name = "Cat-Cow Stretch", sanskritName = "Marjaryasana-Bitilasana",
        category = ASANA_CAT_THERAPY, level = "Beginner", holdSeconds = 60, emoji = "🐈",
        steps = listOf(
            "Begin on all fours — wrists under shoulders, knees under hips.",
            "Inhale: drop belly, lift chest and tailbone (Cow).",
            "Exhale: round spine toward ceiling, tuck chin (Cat).",
            "Continue flowing between the two positions with breath.",
            "Move slowly and gently for 1-2 minutes.",
            "Feel each vertebra move with each breath."
        ),
        benefits = listOf("Improves spinal flexibility", "Relieves back & neck pain", "Massages abdominal organs", "Warms up the spine"),
        precautions = listOf("Move gently with existing back injuries", "Avoid if wrists are painful"),
        targetBodyPart = "Spine & Back", caloriesPerMin = 2.0f
    ),

    YogaAsana(
        id = "vrksasana", name = "Tree Pose", sanskritName = "Vrksasana",
        category = ASANA_CAT_MORNING, level = "Beginner", holdSeconds = 45, emoji = "🌳",
        steps = listOf(
            "Stand in Mountain Pose. Shift weight onto your left foot.",
            "Place sole of right foot on inner left thigh or calf (not knee).",
            "Press foot and leg together for stability.",
            "Bring palms together at heart center or raise arms overhead.",
            "Fix your gaze on a still point ahead for balance.",
            "Hold 30-60 seconds, then switch sides."
        ),
        benefits = listOf("Improves balance & stability", "Strengthens ankles & calves", "Stretches inner thighs", "Improves mental focus"),
        precautions = listOf("Use a wall for support if needed", "Avoid with serious ankle injuries"),
        targetBodyPart = "Legs & Core", caloriesPerMin = 2.0f
    ),

    YogaAsana(
        id = "bhujangasana", name = "Cobra Pose", sanskritName = "Bhujangasana",
        category = ASANA_CAT_MORNING, level = "Beginner", holdSeconds = 30, emoji = "🐍",
        steps = listOf(
            "Lie face down, legs extended, tops of feet on the mat.",
            "Place palms flat on mat under your shoulders.",
            "Hug elbows toward your body.",
            "Inhale: press hands down and slowly lift chest.",
            "Keep a slight bend in elbows, shoulders away from ears.",
            "Hold 15-30 seconds, then exhale and lower down."
        ),
        benefits = listOf("Strengthens the spine", "Stretches chest & shoulders", "Firms the glutes", "Stimulates abdominal organs"),
        precautions = listOf("Avoid with back injury or pregnancy", "Don't strain the neck"),
        targetBodyPart = "Back & Chest", caloriesPerMin = 2.5f
    ),

    YogaAsana(
        id = "setu_bandha", name = "Bridge Pose", sanskritName = "Setu Bandhasana",
        category = ASANA_CAT_STRESS, level = "Beginner", holdSeconds = 60, emoji = "🌉",
        steps = listOf(
            "Lie on back with knees bent, feet flat, hip-width apart.",
            "Arms alongside body, palms facing down.",
            "Press feet firmly into floor on an inhale.",
            "Lift hips toward ceiling, engaging thighs and glutes.",
            "Optionally clasp hands beneath back and press arms down.",
            "Hold 30-60 seconds, then slowly lower down on exhale."
        ),
        benefits = listOf("Strengthens back & glutes", "Stretches chest, neck & spine", "Reduces stress & anxiety", "Improves digestion"),
        precautions = listOf("Avoid with neck injuries", "Reduce range if knee pain is present"),
        targetBodyPart = "Back & Glutes", caloriesPerMin = 3.5f
    ),

    YogaAsana(
        id = "viparita_karani", name = "Legs Up the Wall", sanskritName = "Viparita Karani",
        category = ASANA_CAT_SLEEP, level = "Beginner", holdSeconds = 300, emoji = "🦵",
        steps = listOf(
            "Sit sideways close to a wall.",
            "Swing legs up the wall as you lower back to floor.",
            "Scoot sitting bones as close to the wall as comfortable.",
            "Let arms rest at sides or on belly.",
            "Close eyes and breathe naturally.",
            "Hold for 5-10 minutes for maximum relaxation."
        ),
        benefits = listOf("Drains fluid from swollen legs", "Calms the nervous system", "Relieves lower back fatigue", "Reduces anxiety & insomnia"),
        precautions = listOf("Avoid with serious eye conditions (glaucoma)", "Not recommended during menstruation"),
        targetBodyPart = "Legs & Back", caloriesPerMin = 1.0f
    ),

    YogaAsana(
        id = "savasana", name = "Corpse Pose", sanskritName = "Savasana",
        category = ASANA_CAT_SLEEP, level = "Beginner", holdSeconds = 300, emoji = "🧘",
        steps = listOf(
            "Lie flat on back, legs slightly apart.",
            "Let feet fall open naturally.",
            "Arms slightly away from body, palms facing up.",
            "Close your eyes and relax every muscle.",
            "Breathe naturally and release all tension.",
            "Stay for 5-10 minutes in complete stillness."
        ),
        benefits = listOf("Deep relaxation for body & mind", "Reduces stress & anxiety", "Lowers blood pressure", "Improves sleep quality"),
        precautions = listOf("Use a pillow under knees if lower back is uncomfortable", "Eye pillow helps block light"),
        targetBodyPart = "Full Body", caloriesPerMin = 1.0f
    ),

    YogaAsana(
        id = "paschimottanasana", name = "Seated Forward Bend", sanskritName = "Paschimottanasana",
        category = ASANA_CAT_FLEXIBILITY, level = "Beginner", holdSeconds = 60, emoji = "🤸",
        steps = listOf(
            "Sit on floor with legs extended straight ahead.",
            "Flex feet so toes point up toward ceiling.",
            "Inhale: lengthen spine, sitting tall.",
            "Exhale: hinge at hips and reach forward toward feet.",
            "Hold shins, ankles, or feet wherever you reach.",
            "Hold for 1-3 minutes, breathing into the stretch."
        ),
        benefits = listOf("Stretches spine, hamstrings & calves", "Calms the mind", "Stimulates kidneys & liver", "Improves digestion"),
        precautions = listOf("Never aggressively round the spine", "Use a strap if you can't reach feet"),
        targetBodyPart = "Back & Hamstrings", caloriesPerMin = 2.0f
    ),

    // ══════════════════ INTERMEDIATE (12) ══════════════════

    YogaAsana(
        id = "warrior_1", name = "Warrior I", sanskritName = "Virabhadrasana I",
        category = ASANA_CAT_POWER, level = "Intermediate", holdSeconds = 45, emoji = "🥷",
        steps = listOf(
            "From Mountain Pose, step left foot back 3-4 feet.",
            "Turn left foot out 45°, right foot pointing forward.",
            "Bend right knee to 90° over right ankle.",
            "Square hips to face forward as much as possible.",
            "Raise arms overhead, palms facing each other.",
            "Hold 30-60 seconds, then switch sides."
        ),
        benefits = listOf("Strengthens legs, core & arms", "Stretches chest, shoulders & hip flexors", "Improves stamina & balance", "Builds focus & determination"),
        precautions = listOf("Keep arms at shoulder level with high blood pressure", "Reduce bend angle with knee issues"),
        targetBodyPart = "Legs & Core", caloriesPerMin = 4.0f
    ),

    YogaAsana(
        id = "warrior_2", name = "Warrior II", sanskritName = "Virabhadrasana II",
        category = ASANA_CAT_POWER, level = "Intermediate", holdSeconds = 45, emoji = "⚔️",
        steps = listOf(
            "Stand with feet 3-4 feet apart.",
            "Turn right foot out 90°, left foot in slightly.",
            "Bend right knee to 90°, knee over ankle.",
            "Extend arms to sides at shoulder height, parallel to floor.",
            "Gaze over right hand, keep shoulders relaxed.",
            "Hold 30-60 seconds, then switch sides."
        ),
        benefits = listOf("Strengthens legs & ankles", "Opens chest & shoulders", "Increases stamina", "Stimulates abdominal organs"),
        precautions = listOf("Avoid with diarrhea", "Look forward (not sideways) with neck issues"),
        targetBodyPart = "Legs & Shoulders", caloriesPerMin = 4.5f
    ),

    YogaAsana(
        id = "trikonasana", name = "Triangle Pose", sanskritName = "Trikonasana",
        category = ASANA_CAT_FLEXIBILITY, level = "Intermediate", holdSeconds = 45, emoji = "📐",
        steps = listOf(
            "Stand with feet 3 feet apart, right foot turned out 90°.",
            "Extend arms out to sides at shoulder height.",
            "Shift hips left as you reach right hand toward right foot.",
            "Rest right hand on shin, ankle, or the floor.",
            "Extend left arm straight up, gaze up at left hand.",
            "Hold 30-60 seconds, then switch sides."
        ),
        benefits = listOf("Stretches legs, hips & spine", "Strengthens core & legs", "Improves digestion", "Relieves stress"),
        precautions = listOf("Look down or straight ahead with neck problems", "Avoid with low blood pressure"),
        targetBodyPart = "Legs & Spine", caloriesPerMin = 3.5f
    ),

    YogaAsana(
        id = "navasana", name = "Boat Pose", sanskritName = "Navasana",
        category = ASANA_CAT_CORE, level = "Intermediate", holdSeconds = 30, emoji = "⛵",
        steps = listOf(
            "Sit on floor with knees bent, feet flat.",
            "Lean back slightly while keeping spine long.",
            "Lift feet off the floor, shins parallel to the ground.",
            "Extend arms forward, parallel to the floor.",
            "For more challenge, straighten legs to form a V-shape.",
            "Hold 20-30 seconds, breathing steadily."
        ),
        benefits = listOf("Strengthens core & hip flexors", "Tones the abdominals", "Improves balance", "Stimulates kidneys & thyroid"),
        precautions = listOf("Avoid with low blood pressure or headache", "Bend knees if back rounds excessively"),
        targetBodyPart = "Core & Abs", caloriesPerMin = 5.0f
    ),

    YogaAsana(
        id = "eka_pada_rajakapotasana", name = "Pigeon Pose", sanskritName = "Eka Pada Rajakapotasana",
        category = ASANA_CAT_FLEXIBILITY, level = "Intermediate", holdSeconds = 90, emoji = "🕊️",
        steps = listOf(
            "From Downward Dog, bring right knee forward behind right wrist.",
            "Extend left leg straight behind you, knee facing down.",
            "Square hips to the front as much as possible.",
            "Walk hands forward and lower torso toward the mat.",
            "Rest forehead on mat or on stacked fists.",
            "Hold 1-3 minutes then switch sides."
        ),
        benefits = listOf("Deep hip opener", "Stretches hip rotators & flexors", "Releases stress stored in hips", "Improves posture"),
        precautions = listOf("Avoid with knee or hip injuries", "Use a folded blanket under hip for support"),
        targetBodyPart = "Hips & Glutes", caloriesPerMin = 2.5f
    ),

    YogaAsana(
        id = "plank", name = "Plank Pose", sanskritName = "Kumbhakasana",
        category = ASANA_CAT_CORE, level = "Intermediate", holdSeconds = 60, emoji = "💪",
        steps = listOf(
            "Begin in a push-up position, hands directly under shoulders.",
            "Spread fingers wide and press into all 10 fingers.",
            "Straighten legs, engaging quadriceps.",
            "Keep body in a straight line from head to heels.",
            "Draw navel to spine, squeeze glutes.",
            "Hold 30-60 seconds, breathing steadily."
        ),
        benefits = listOf("Strengthens entire core", "Tones arms & wrists", "Improves posture", "Builds full-body strength"),
        precautions = listOf("Avoid with wrist or shoulder injuries", "Drop to forearms if wrists hurt"),
        targetBodyPart = "Core & Arms", caloriesPerMin = 5.0f
    ),

    YogaAsana(
        id = "vasisthasana", name = "Side Plank", sanskritName = "Vasisthasana",
        category = ASANA_CAT_CORE, level = "Intermediate", holdSeconds = 30, emoji = "🏋️",
        steps = listOf(
            "Start in Plank pose.",
            "Shift weight to right hand and outer edge of right foot.",
            "Stack left foot on top of right, or stagger for balance.",
            "Lift hips to keep body in one straight line.",
            "Extend left arm straight up toward ceiling.",
            "Hold 20-30 seconds, then switch sides."
        ),
        benefits = listOf("Strengthens obliques & core", "Tones arms & legs", "Improves balance", "Builds wrist strength"),
        precautions = listOf("Drop bottom knee to mat for modification", "Avoid with wrist injuries"),
        targetBodyPart = "Core & Obliques", caloriesPerMin = 5.5f
    ),

    YogaAsana(
        id = "ustrasana", name = "Camel Pose", sanskritName = "Ustrasana",
        category = ASANA_CAT_FLEXIBILITY, level = "Intermediate", holdSeconds = 30, emoji = "🐪",
        steps = listOf(
            "Kneel on mat, knees hip-width apart.",
            "Place hands on hips, fingers pointing down.",
            "Inhale: press hips forward and lift chest.",
            "Slowly reach back for heels, one hand at a time.",
            "Keep hips over knees, chin slightly tucked.",
            "Hold 20-30 seconds, then slowly come up on an inhale."
        ),
        benefits = listOf("Deep backbend that opens the chest", "Stretches hip flexors", "Improves spinal flexibility", "Stimulates thyroid gland"),
        precautions = listOf("Avoid with back or neck injuries", "Keep toes tucked for a gentler version"),
        targetBodyPart = "Chest & Spine", caloriesPerMin = 3.0f
    ),

    YogaAsana(
        id = "ardha_chandrasana", name = "Half Moon Pose", sanskritName = "Ardha Chandrasana",
        category = ASANA_CAT_POWER, level = "Intermediate", holdSeconds = 30, emoji = "🌙",
        steps = listOf(
            "From Triangle Pose (right side), bend right knee.",
            "Place right hand on floor 12 inches in front of right foot.",
            "Shift weight to right hand and right foot.",
            "Lift left leg parallel to floor, flex left foot.",
            "Stack left hip over right hip, extend left arm up.",
            "Hold 20-30 seconds, then switch sides."
        ),
        benefits = listOf("Improves balance & coordination", "Strengthens legs, ankles & core", "Opens the chest", "Builds mental focus"),
        precautions = listOf("Use a block under your hand for support", "Look down (not up) with neck issues"),
        targetBodyPart = "Legs & Balance", caloriesPerMin = 4.0f
    ),

    YogaAsana(
        id = "garudasana", name = "Eagle Pose", sanskritName = "Garudasana",
        category = ASANA_CAT_POWER, level = "Intermediate", holdSeconds = 30, emoji = "🦅",
        steps = listOf(
            "Stand in Mountain Pose. Bend both knees slightly.",
            "Cross right thigh over left, tuck right foot behind left calf.",
            "Cross right arm under left arm at elbows.",
            "Wrap forearms around each other, press palms together.",
            "Lift elbows to shoulder height, sink hips down.",
            "Hold 20-30 seconds, then switch sides."
        ),
        benefits = listOf("Strengthens legs & ankles", "Stretches shoulders & upper back", "Improves balance & focus", "Opens hips"),
        precautions = listOf("Avoid with knee or ankle injuries", "Wrap only at shins if full wrap isn't possible"),
        targetBodyPart = "Legs & Shoulders", caloriesPerMin = 3.5f
    ),

    YogaAsana(
        id = "dhanurasana", name = "Bow Pose", sanskritName = "Dhanurasana",
        category = ASANA_CAT_FLEXIBILITY, level = "Intermediate", holdSeconds = 30, emoji = "🏹",
        steps = listOf(
            "Lie face down on mat, arms alongside body.",
            "Bend knees and bring heels toward buttocks.",
            "Reach back with both hands and grasp ankles.",
            "Inhale: kick feet away from body, lifting thighs and chest.",
            "Keep weight on abdomen, look forward.",
            "Hold 20-30 seconds, breathing evenly."
        ),
        benefits = listOf("Opens entire front of body", "Strengthens back muscles", "Improves posture", "Stimulates digestive organs"),
        precautions = listOf("Avoid with back injury, hernia or pregnancy", "Don't strain the neck"),
        targetBodyPart = "Back & Chest", caloriesPerMin = 3.5f
    ),

    YogaAsana(
        id = "chakrasana", name = "Wheel Pose", sanskritName = "Urdhva Dhanurasana",
        category = ASANA_CAT_FLEXIBILITY, level = "Intermediate", holdSeconds = 30, emoji = "🎡",
        steps = listOf(
            "Lie on back, bend knees with feet flat near hips.",
            "Place palms by ears, fingers pointing toward shoulders.",
            "Inhale: press hands and feet to lift hips and chest.",
            "Straighten arms and create a full arch with the body.",
            "Keep feet parallel and press hips up.",
            "Hold 15-30 seconds, then lower down slowly."
        ),
        benefits = listOf("Full spinal backbend", "Strengthens arms, legs & spine", "Opens chest & lungs", "Energizes the entire body"),
        precautions = listOf("Avoid with back, neck or wrist injuries", "Warm up thoroughly before attempting"),
        targetBodyPart = "Full Body", caloriesPerMin = 5.0f
    ),

    // ══════════════════ ADVANCED (8) ══════════════════

    YogaAsana(
        id = "sirsasana", name = "Headstand", sanskritName = "Sirsasana",
        category = ASANA_CAT_POWER, level = "Advanced", holdSeconds = 60, emoji = "🤸",
        steps = listOf(
            "Kneel and interlace fingers, creating a base with forearms.",
            "Place the crown of head on the mat, cupped by hands.",
            "Straighten legs and walk feet toward your head.",
            "Engage core and slowly lift both knees to chest.",
            "Carefully extend legs straight up toward the ceiling.",
            "Hold 30-60 seconds. Practice near a wall initially."
        ),
        benefits = listOf("Strengthens shoulders & arms", "Improves blood circulation to brain", "Builds core stability", "Increases mental focus"),
        precautions = listOf("Only practice with proper instruction", "Avoid with neck issues or high blood pressure", "Always use a wall as support"),
        targetBodyPart = "Full Body", caloriesPerMin = 5.0f
    ),

    YogaAsana(
        id = "sarvangasana", name = "Shoulder Stand", sanskritName = "Sarvangasana",
        category = ASANA_CAT_THERAPY, level = "Advanced", holdSeconds = 60, emoji = "🕴️",
        steps = listOf(
            "Lie on back. Bend knees and lift legs overhead.",
            "Support lower back with hands, fingers pointing up.",
            "Walk hands closer to shoulder blades for support.",
            "Lift legs straight up toward the ceiling.",
            "Keep weight on shoulders and upper arms, NOT the neck.",
            "Hold 30-60 seconds, then slowly lower down."
        ),
        benefits = listOf("Stimulates thyroid & parathyroid glands", "Calms the nervous system", "Improves circulation", "Relieves stress"),
        precautions = listOf("Avoid with neck, shoulder or heart conditions", "Avoid during menstruation", "Use folded blankets under shoulders"),
        targetBodyPart = "Thyroid & Circulation", caloriesPerMin = 4.0f
    ),

    YogaAsana(
        id = "bakasana", name = "Crow Pose", sanskritName = "Bakasana",
        category = ASANA_CAT_CORE, level = "Advanced", holdSeconds = 20, emoji = "🦢",
        steps = listOf(
            "Squat down with feet slightly wider than hip-width.",
            "Plant palms firmly on mat shoulder-width apart.",
            "Bend elbows and place knees on backs of upper arms.",
            "Lean forward, shifting weight to hands.",
            "Lift feet off the ground, one at a time if needed.",
            "Hold 10-20 seconds, engage core throughout."
        ),
        benefits = listOf("Strengthens arms, wrists & core", "Improves balance & body coordination", "Builds focus & confidence", "Tones the abdominals"),
        precautions = listOf("Place a cushion under head as a safety net", "Avoid with wrist or carpal tunnel issues"),
        targetBodyPart = "Arms & Core", caloriesPerMin = 5.5f
    ),

    YogaAsana(
        id = "mayurasana", name = "Peacock Pose", sanskritName = "Mayurasana",
        category = ASANA_CAT_CORE, level = "Advanced", holdSeconds = 15, emoji = "🦚",
        steps = listOf(
            "Kneel and place hands on the floor, fingers pointing backward.",
            "Bend elbows and press them into the abdomen.",
            "Lean torso forward, placing it on the upper arms.",
            "Extend legs straight behind you.",
            "Shift weight forward until legs lift off the mat.",
            "Hold 10-15 seconds, keeping body parallel to floor."
        ),
        benefits = listOf("Extreme core & arm strength", "Stimulates digestive organs", "Detoxifies the body", "Improves wrist strength"),
        precautions = listOf("Avoid with wrist, elbow or shoulder injuries", "Requires significant strength to attempt safely"),
        targetBodyPart = "Arms & Core", caloriesPerMin = 6.0f
    ),

    YogaAsana(
        id = "padmasana", name = "Lotus Pose", sanskritName = "Padmasana",
        category = ASANA_CAT_STRESS, level = "Advanced", holdSeconds = 300, emoji = "🪷",
        steps = listOf(
            "Sit on the floor with legs extended.",
            "Bend right knee and place right foot on left thigh.",
            "Bend left knee and place left foot on right thigh.",
            "Sit tall, spine erect. Place hands on knees.",
            "Close eyes and breathe naturally.",
            "Hold 5-10 minutes for meditation."
        ),
        benefits = listOf("Opens hips & knees", "Calms the mind for deep meditation", "Improves posture", "Stimulates pelvis & spine"),
        precautions = listOf("Avoid with knee or ankle injuries", "Work up gradually — never force the knees"),
        targetBodyPart = "Hips & Knees", caloriesPerMin = 1.5f
    ),

    YogaAsana(
        id = "vrschikasana", name = "Scorpion Pose", sanskritName = "Vrschikasana",
        category = ASANA_CAT_POWER, level = "Advanced", holdSeconds = 15, emoji = "🦂",
        steps = listOf(
            "Begin in Dolphin Pose (forearms on mat, hips high).",
            "Walk feet toward elbows until hips are over shoulders.",
            "Shift weight to forearms and lift one leg at a time.",
            "Balance in Forearm Stand (Pincha Mayurasana) first.",
            "Slowly curve spine and lower legs toward head.",
            "Hold 10-15 seconds with extreme core control."
        ),
        benefits = listOf("Ultimate balance & full-body strength", "Strengthens entire body", "Improves spine flexibility", "Builds mental focus"),
        precautions = listOf("Only after mastering Headstand & Forearm Stand", "Practice near a wall at all times", "Requires expert guidance"),
        targetBodyPart = "Full Body", caloriesPerMin = 6.0f
    ),

    YogaAsana(
        id = "kapotasana", name = "King Pigeon", sanskritName = "Kapotasana",
        category = ASANA_CAT_FLEXIBILITY, level = "Advanced", holdSeconds = 30, emoji = "👑",
        steps = listOf(
            "Start in Camel Pose (Ustrasana) kneeling position.",
            "Slowly walk hands back toward your feet.",
            "Curl spine into a deep backbend.",
            "Try to place top of head on mat or reach for the feet.",
            "Keep thighs perpendicular to the floor.",
            "Hold 15-30 seconds, then come up very carefully."
        ),
        benefits = listOf("Extreme spinal flexibility", "Opens chest & throat deeply", "Stretches hip flexors", "Advanced backbend mastery"),
        precautions = listOf("Only for very experienced practitioners", "Never force this pose", "Always warm up fully with lighter backbends first"),
        targetBodyPart = "Spine & Chest", caloriesPerMin = 3.5f
    ),

    YogaAsana(
        id = "tittibhasana", name = "Firefly Pose", sanskritName = "Tittibhasana",
        category = ASANA_CAT_CORE, level = "Advanced", holdSeconds = 15, emoji = "🦟",
        steps = listOf(
            "Stand with feet wider than shoulder-width apart.",
            "Bend knees and place hands on the floor between legs.",
            "Walk hands back and place upper arms on backs of thighs.",
            "Lean forward, shifting weight to hands.",
            "Straighten legs and extend them to the sides.",
            "Hold 10-15 seconds with full core engagement."
        ),
        benefits = listOf("Builds arm & core strength", "Improves hip flexibility", "Challenges balance & coordination", "Builds body confidence"),
        precautions = listOf("Avoid with wrist or shoulder injuries", "Requires strong arms and open hips"),
        targetBodyPart = "Arms & Hips", caloriesPerMin = 5.5f
    )
)

// ── 4 Yoga Programs ───────────────────────────────────────────────────────
val allYogaPrograms = listOf(
    YogaProgram(
        id = "beginner_7day",
        name = "7-Day Beginner Starter",
        description = "Perfect for yoga newcomers. Build a strong foundation with fundamental poses and proper breathing. Just 15 minutes per day.",
        durationDays = 7,
        emoji = "🌱",
        colorHex = 0xFF81C784,
        poseIds = listOf("tadasana","adho_mukha","bhujangasana","cat_cow","setu_bandha","balasana","savasana"),
        level = "Beginner"
    ),
    YogaProgram(
        id = "flexibility_14day",
        name = "14-Day Flexibility Challenge",
        description = "Gradually open your hips, hamstrings and spine. Designed for those who want to dramatically increase their range of motion.",
        durationDays = 14,
        emoji = "🤸",
        colorHex = 0xFFFFB74D,
        poseIds = listOf("adho_mukha","paschimottanasana","trikonasana","eka_pada_rajakapotasana","ustrasana","dhanurasana","chakrasana",
            "vrksasana","ardha_chandrasana","garudasana","navasana","plank","bhujangasana","balasana"),
        level = "Intermediate"
    ),
    YogaProgram(
        id = "morning_30day",
        name = "30-Day Morning Yoga",
        description = "Start every day with energy and clarity. A complete morning routine that progressively builds strength, flexibility and mindfulness.",
        durationDays = 30,
        emoji = "🌅",
        colorHex = 0xFF9FA8DA,
        poseIds = listOf(
            "tadasana","adho_mukha","bhujangasana","warrior_1","warrior_2","trikonasana","vrksasana",
            "navasana","plank","vasisthasana","setu_bandha","cat_cow","paschimottanasana","balasana","savasana",
            "garudasana","ardha_chandrasana","ustrasana","dhanurasana","eka_pada_rajakapotasana",
            "tadasana","adho_mukha","warrior_1","warrior_2","plank","navasana","setu_bandha","bhujangasana","balasana","savasana"
        ),
        level = "Intermediate"
    ),
    YogaProgram(
        id = "stress_sleep",
        name = "Stress Relief & Sleep",
        description = "Wind down each evening with calming poses. Reduces cortisol, eases anxiety and prepares the body and mind for deep restful sleep.",
        durationDays = 7,
        emoji = "😴",
        colorHex = 0xFF4DB6AC,
        poseIds = listOf("balasana","setu_bandha","viparita_karani","savasana","cat_cow","paschimottanasana","padmasana"),
        level = "Beginner"
    )
)
