package com.pro.aifitnesstracker.data.yoga

// ── Yoga Category Constants ──────────────────────────────────────────────────
object YogaCategoryNames {
    const val BEGINNER = "Beginner Yoga"
    const val MORNING = "Morning Yoga"
    const val EVENING = "Evening Yoga"
    const val WEIGHT_LOSS = "Weight Loss Yoga"
    const val FLEXIBILITY = "Flexibility Yoga"
    const val STRESS_RELIEF = "Stress Relief Yoga"
    const val BACK_FOCUSED = "Back-focused Gentle Yoga"
    const val MEDITATION = "Meditation Yoga"
    const val FULL_BODY = "Full Body Yoga"
    const val ADVANCED = "Advanced Yoga"
}

// ── Models ────────────────────────────────────────────────────────────────────
data class YogaCategory(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val difficulty: String,
    val estimatedDurationMinutes: Int,
    val poseCount: Int,
    val recommendedSessionTitle: String,
    val colorHex: Long
)

data class DetailedYogaPose(
    val id: String,
    val name: String,
    val sanskritName: String,
    val category: String,
    val level: String, // "Beginner" | "Intermediate" | "Advanced"
    val holdSeconds: Int,
    val emoji: String,
    val targetBodyAreas: String,
    val caloriesPerMin: Float,
    val description: String,
    val steps: List<String>,
    val breathingInstructions: String,
    val benefits: List<String>,
    val precautions: List<String>,
    val beginnerModifications: String
)

data class PersonalizedYogaPlan(
    val id: String,
    val title: String,
    val description: String,
    val totalDays: Int,
    val difficulty: String,
    val dailyGoalMinutes: Int,
    val emoji: String,
    val colorHex: Long,
    val dailyRoutines: List<PlanDailyRoutine>
)

data class PlanDailyRoutine(
    val dayNumber: Int,
    val title: String,
    val durationMinutes: Int,
    val poseIds: List<String>,
    val focusDescription: String
)

data class PredefinedYogaSession(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val durationMinutes: Int,
    val difficulty: String,
    val poseIds: List<String>,
    val emoji: String
)

// ── Content Repository ────────────────────────────────────────────────────────
object YogaContentRepository {

    // ── 1. The 10 Official Categories ─────────────────────────────────────────
    val categories = listOf(
        YogaCategory(
            id = "cat_beginner",
            name = YogaCategoryNames.BEGINNER,
            description = "Gentle introduction focusing on posture alignment, balance, and breath fundamentals.",
            emoji = "🌱",
            difficulty = "Beginner",
            estimatedDurationMinutes = 12,
            poseCount = 8,
            recommendedSessionTitle = "Beginner Foundation Flow",
            colorHex = 0xFF10B981
        ),
        YogaCategory(
            id = "cat_morning",
            name = YogaCategoryNames.MORNING,
            description = "Energizing wake-up flow with dynamic stretches to awaken spine and boost daytime vitality.",
            emoji = "🌅",
            difficulty = "All Levels",
            estimatedDurationMinutes = 15,
            poseCount = 7,
            recommendedSessionTitle = "Morning Sun Salutation Flow",
            colorHex = 0xFFF59E0B
        ),
        YogaCategory(
            id = "cat_evening",
            name = YogaCategoryNames.EVENING,
            description = "Restorative and soothing postures designed to melt away daily fatigue and promote restful sleep.",
            emoji = "🌙",
            difficulty = "Beginner",
            estimatedDurationMinutes = 12,
            poseCount = 6,
            recommendedSessionTitle = "Evening Bedtime Wind-Down",
            colorHex = 0xFF6366F1
        ),
        YogaCategory(
            id = "cat_weight_loss",
            name = YogaCategoryNames.WEIGHT_LOSS,
            description = "Higher tempo vinyasa flow engaging core muscle groups to burn calories and build lean muscle.",
            emoji = "🔥",
            difficulty = "Intermediate",
            estimatedDurationMinutes = 20,
            poseCount = 9,
            recommendedSessionTitle = "Dynamic Power & Burn Flow",
            colorHex = 0xFFEF4444
        ),
        YogaCategory(
            id = "cat_flexibility",
            name = YogaCategoryNames.FLEXIBILITY,
            description = "Deep hip openers, hamstring stretches, and shoulder mobility poses for full elasticity.",
            emoji = "🤸",
            difficulty = "All Levels",
            estimatedDurationMinutes = 15,
            poseCount = 8,
            recommendedSessionTitle = "Full Body Flexibility Flow",
            colorHex = 0xFF06B6D4
        ),
        YogaCategory(
            id = "cat_stress_relief",
            name = YogaCategoryNames.STRESS_RELIEF,
            description = "Calming grounding poses paired with slow breaths to reduce cortisol and quiet busy thoughts.",
            emoji = "🧘",
            difficulty = "Beginner",
            estimatedDurationMinutes = 14,
            poseCount = 6,
            recommendedSessionTitle = "Mindful Stress Relief & Calm",
            colorHex = 0xFF8B5CF6
        ),
        YogaCategory(
            id = "cat_back_focused",
            name = YogaCategoryNames.BACK_FOCUSED,
            description = "Therapeutic decompression for lower back, thoracic mobilization, and gentle spinal alignment.",
            emoji = "✨",
            difficulty = "Gentle",
            estimatedDurationMinutes = 12,
            poseCount = 6,
            recommendedSessionTitle = "Back Relief & Spine Gentle Flow",
            colorHex = 0xFF14B8A6
        ),
        YogaCategory(
            id = "cat_meditation",
            name = YogaCategoryNames.MEDITATION,
            description = "Seated meditative postures and conscious pranayama to train mental clarity and stillness.",
            emoji = "🪷",
            difficulty = "All Levels",
            estimatedDurationMinutes = 10,
            poseCount = 5,
            recommendedSessionTitle = "Mindfulness & Stillness Flow",
            colorHex = 0xFFEC4899
        ),
        YogaCategory(
            id = "cat_full_body",
            name = YogaCategoryNames.FULL_BODY,
            description = "A harmonious head-to-toe sequence uniting standing strength, seated twists, and grounding holds.",
            emoji = "⚡",
            difficulty = "Intermediate",
            estimatedDurationMinutes = 20,
            poseCount = 10,
            recommendedSessionTitle = "Complete Full Body Harmony",
            colorHex = 0xFF3B82F6
        ),
        YogaCategory(
            id = "cat_advanced",
            name = YogaCategoryNames.ADVANCED,
            description = "Challenging arm balances, deep inversions, and intense stamina holds for experienced yogis.",
            emoji = "👑",
            difficulty = "Advanced",
            estimatedDurationMinutes = 25,
            poseCount = 8,
            recommendedSessionTitle = "Advanced Peak Balance Flow",
            colorHex = 0xFFD946EF
        )
    )

    // ── 2. Comprehensive Pose Library (32 Verified Poses) ────────────────────
    val poses = listOf(
        DetailedYogaPose(
            id = "tadasana",
            name = "Mountain Pose",
            sanskritName = "Tadasana",
            category = YogaCategoryNames.BEGINNER,
            level = "Beginner",
            holdSeconds = 60,
            emoji = "🧍",
            targetBodyAreas = "Spine, Thighs, Ankles, Core",
            caloriesPerMin = 2.0f,
            description = "The foundational standing posture that teaches balanced posture, grounded stability, and full body awareness.",
            steps = listOf(
                "Stand tall with feet together or hip-width apart.",
                "Distribute weight evenly across all four corners of both feet.",
                "Engage quadriceps and gently draw kneecaps upward.",
                "Draw lower belly in lightly and lengthen tailbone toward floor.",
                "Roll shoulders up, back, and down, letting arms rest gently at sides.",
                "Gaze straight ahead softly and breathe steadily through your nose."
            ),
            breathingInstructions = "Inhale to lengthen up through the spine; exhale to ground down through your feet.",
            benefits = listOf(
                "Corrects postural alignment and spinal curvature",
                "Strengthens arches of feet, ankles, and thighs",
                "Reduces flat feet symptoms",
                "Cultivates centered mental calm"
            ),
            precautions = listOf(
                "Avoid prolonged holding if you feel dizzy or have very low blood pressure",
                "Keep feet hip-width if balance feels unsteady"
            ),
            beginnerModifications = "Practice against a wall to verify head, shoulder, and sacrum alignment."
        ),

        DetailedYogaPose(
            id = "vrikshasana",
            name = "Tree Pose",
            sanskritName = "Vrikshasana",
            category = YogaCategoryNames.BEGINNER,
            level = "Beginner",
            holdSeconds = 45,
            emoji = "🌳",
            targetBodyAreas = "Hips, Groin, Calves, Core",
            caloriesPerMin = 2.8f,
            description = "A classic balance pose that strengthens the stabilizing muscles of the standing leg while expanding the chest and hips.",
            steps = listOf(
                "Begin in Mountain Pose, shifting weight onto left foot.",
                "Bend right knee and place sole of right foot on inner left calf or inner thigh.",
                "Never place your foot directly on the knee joint.",
                "Press hands together in prayer position at chest or extend overhead.",
                "Fix gaze on a steady unmoving focal point (Drishti) in front of you.",
                "Hold for 30-45 seconds, then repeat on opposite side."
            ),
            breathingInstructions = "Maintain slow rhythmic breaths. Inhale to lift chest; exhale to stabilize foot.",
            benefits = listOf(
                "Improves neuromuscular balance and coordination",
                "Strengthens ankles, calves, and stabilizing tendons",
                "Opens inner groin and adductors",
                "Enhances single-pointed concentration"
            ),
            precautions = listOf(
                "Do not place foot against the side of the knee",
                "Use a wall for hand support if prone to falling"
            ),
            beginnerModifications = "Rest the right toes lightly on the floor like a kickstand."
        ),

        DetailedYogaPose(
            id = "trikonasana",
            name = "Triangle Pose",
            sanskritName = "Trikonasana",
            category = YogaCategoryNames.FLEXIBILITY,
            level = "Beginner",
            holdSeconds = 45,
            emoji = "📐",
            targetBodyAreas = "Hamstrings, Hips, Spine, Chest",
            caloriesPerMin = 3.2f,
            description = "A powerful standing posture that creates length throughout the torso, stretches hamstrings, and opens the chest.",
            steps = listOf(
                "Step feet wide apart (~3.5 to 4 feet). Turn right foot out 90 degrees, left foot in 15 degrees.",
                "Inhale arms parallel to floor, palms down.",
                "Exhale and reach right arm forward over right thigh, hinging from the hip.",
                "Lower right hand to shin, ankle, or a block. Extend left arm toward ceiling.",
                "Turn chest gently open toward ceiling and look up at top thumb.",
                "Hold for 5-8 breaths, then switch sides."
            ),
            breathingInstructions = "Inhale expand ribs; exhale deepen rotation through the thoracic spine.",
            benefits = listOf(
                "Stretches and lengthens tight hamstrings and groin",
                "Relieves mild backache and stiffness",
                "Expands lung capacity through lateral chest opening",
                "Stimulates abdominal organs and digestion"
            ),
            precautions = listOf(
                "Do not lock or hyperextend the front knee",
                "Keep gaze forward or down if experiencing neck discomfort"
            ),
            beginnerModifications = "Place front hand onto a yoga block placed outside the shin."
        ),

        DetailedYogaPose(
            id = "bhujangasana",
            name = "Cobra Pose",
            sanskritName = "Bhujangasana",
            category = YogaCategoryNames.BACK_FOCUSED,
            level = "Beginner",
            holdSeconds = 40,
            emoji = "🐍",
            targetBodyAreas = "Upper Back, Chest, Abdomen, Glutes",
            caloriesPerMin = 3.0f,
            description = "A gentle heart-opening backbend that reverses rounded shoulders and strengthens the erector spinae muscles.",
            steps = listOf(
                "Lie prone on stomach with legs extended, tops of feet pressing into mat.",
                "Place hands flat on floor beneath shoulders, elbows tucked close to ribs.",
                "Press pubic bone gently into floor and engage thighs.",
                "Inhale and lift chest off the mat using back muscles rather than arm force.",
                "Keep elbows slightly bent and shoulders down away from ears.",
                "Hold for 30-40 seconds, then slowly lower chest down on exhale."
            ),
            breathingInstructions = "Inhale lift heart upward; exhale broaden collarbones and keep neck soft.",
            benefits = listOf(
                "Relieves upper back stiffness and desk slump",
                "Strengthens deep spinal muscles",
                "Opens chest and stimulates thyroid gland",
                "Invigorates central nervous system"
            ),
            precautions = listOf(
                "Avoid during pregnancy or severe lower back herniation",
                "Do not crunch into the cervical or lumbar spine"
            ),
            beginnerModifications = "Rest on forearms in Baby Cobra (Sphinx Pose) to reduce lumbar compression."
        ),

        DetailedYogaPose(
            id = "balasana",
            name = "Child's Pose",
            sanskritName = "Balasana",
            category = YogaCategoryNames.STRESS_RELIEF,
            level = "Beginner",
            holdSeconds = 90,
            emoji = "🛐",
            targetBodyAreas = "Lower Back, Hips, Thighs, Ankles",
            caloriesPerMin = 1.4f,
            description = "The quintessential resting posture that gently stretches the lower spine, soothes the adrenal system, and restores calm.",
            steps = listOf(
                "Kneel on the mat, big toes touching, knees separated hip-width or wider.",
                "Exhale and lower hips onto heels while extending torso forward.",
                "Rest forehead gently on the mat.",
                "Extend arms forward with palms down, or drape arms back alongside shins.",
                "Release shoulder blades wide and breathe deep into the back ribs.",
                "Hold for 1 to 3 minutes, letting tension release with every exhale."
            ),
            breathingInstructions = "Breathe deeply into the posterior ribcage and lower back.",
            benefits = listOf(
                "Releases tension across lower back and sacrum",
                "Activates parasympathetic relaxation response",
                "Eases headaches, fatigue, and sensory overwhelm",
                "Gently stretches hip flexors and ankles"
            ),
            precautions = listOf(
                "Avoid if suffering from severe knee pain or diarrhea",
                "Place a blanket under knees or behind knee crease for cushion"
            ),
            beginnerModifications = "Place a bolster or folded blanket between hips and heels."
        ),

        DetailedYogaPose(
            id = "adho_mukha",
            name = "Downward-Facing Dog",
            sanskritName = "Adho Mukha Svanasana",
            category = YogaCategoryNames.MORNING,
            level = "Beginner",
            holdSeconds = 60,
            emoji = "🐕",
            targetBodyAreas = "Hamstrings, Calves, Shoulders, Spine",
            caloriesPerMin = 3.5f,
            description = "An energizing mild inversion that stretches the posterior chain while building upper-body and core endurance.",
            steps = listOf(
                "Start on hands and knees with wrists under shoulders and knees under hips.",
                "Spread fingers wide, pressing index knuckles firmly into the mat.",
                "Tuck toes and exhale, lifting hips up and back to form an inverted 'V'.",
                "Press chest gently toward thighs while keeping ears aligned with upper arms.",
                "Pedal feet gently to warm up calf and hamstring muscles.",
                "Hold and breathe deeply for 1 minute."
            ),
            breathingInstructions = "Full deep inhalations; long steady exhalations drawing navel toward spine.",
            benefits = listOf(
                "Strengthens shoulders, triceps, and upper back",
                "Deep stretch for hamstrings, calves, and Achilles tendons",
                "Stimulates blood flow to head, boosting alertness",
                "Decompresses vertebral column"
            ),
            precautions = listOf(
                "Avoid with carpal tunnel syndrome or unmanaged hypertension",
                "Bend knees generously if hamstrings are tight"
            ),
            beginnerModifications = "Keep knees comfortably bent and heels lifted to protect lower back."
        ),

        DetailedYogaPose(
            id = "setu_bandhasana",
            name = "Bridge Pose",
            sanskritName = "Setu Bandhasana",
            category = YogaCategoryNames.BACK_FOCUSED,
            level = "Beginner",
            holdSeconds = 50,
            emoji = "🌉",
            targetBodyAreas = "Glutes, Hamstrings, Chest, Spine",
            caloriesPerMin = 3.2f,
            description = "A rejuvenating backbend that strengthens posterior chain muscles while expanding the thoracic cage and throat.",
            steps = listOf(
                "Lie supine on back with knees bent and feet flat, hip-width apart.",
                "Position heels close to sitting bones so fingertips can touch heels.",
                "Arms along sides, palms facing down.",
                "Exhale and press through feet and arms to lift hips toward ceiling.",
                "Interlace fingers beneath pelvis and roll shoulders underneath.",
                "Keep thighs parallel and hold for 40-50 seconds."
            ),
            breathingInstructions = "Inhale expand chest toward chin; exhale press through heels to sustain height.",
            benefits = listOf(
                "Strengthens gluteus maximus, hamstrings, and erectors",
                "Stretches hip flexors, abdomen, and chest",
                "Stimulates thyroid and digestive organs",
                "Relieves feelings of heaviness and sluggishness"
            ),
            precautions = listOf(
                "Do not turn head while in the pose; keep cervical spine neutral",
                "Avoid with acute neck or shoulder injuries"
            ),
            beginnerModifications = "Place a yoga block beneath the sacrum for Supported Restorative Bridge."
        ),

        DetailedYogaPose(
            id = "sukhasana",
            name = "Easy Pose",
            sanskritName = "Sukhasana",
            category = YogaCategoryNames.MEDITATION,
            level = "Beginner",
            holdSeconds = 120,
            emoji = "🧘",
            targetBodyAreas = "Hips, Groin, Spine",
            caloriesPerMin = 1.3f,
            description = "A classic cross-legged seated posture optimal for pranayama breathing and mindfulness meditation.",
            steps = listOf(
                "Sit comfortably on mat with legs crossed and shins resting naturally.",
                "Elevate pelvis on a folded cushion so knees are level with or lower than hips.",
                "Lengthen spine from tailbone through crown of head.",
                "Rest hands on knees in Jnana Mudra (index touching thumb).",
                "Relax shoulders, soften jaw, and close eyes.",
                "Maintain steady natural breathing for 2-5 minutes."
            ),
            breathingInstructions = "Smooth equal breathing (Sama Vritti) through both nostrils.",
            benefits = listOf(
                "Promotes grounded presence and emotional equilibrium",
                "Gently opens hips and outer thighs",
                "Strengthens postural endurance in the back",
                "Optimal for breath control exercises"
            ),
            precautions = listOf(
                "Avoid if experiencing acute knee inflammation",
                "Sit with back supported against a wall if spine slumps"
            ),
            beginnerModifications = "Sit on a firm yoga block or folded meditation cushion."
        ),

        DetailedYogaPose(
            id = "vajrasana",
            name = "Thunderbolt Pose",
            sanskritName = "Vajrasana",
            category = YogaCategoryNames.MEDITATION,
            level = "Beginner",
            holdSeconds = 90,
            emoji = "⚡",
            targetBodyAreas = "Thighs, Ankles, Knees, Digestion",
            caloriesPerMin = 1.5f,
            description = "A seated kneeling posture celebrated in traditional texts for stimulating digestion and promoting an upright spine.",
            steps = listOf(
                "Kneel on mat with big toes touching and heels slightly separated.",
                "Lower buttocks onto the concave cup formed by your heels.",
                "Rest palms on thighs with elbows close to ribs.",
                "Keep spine straight, shoulders back, and chin parallel to floor.",
                "Breathe calmly and hold for 1 to 3 minutes."
            ),
            breathingInstructions = "Deep diaphragmatic breathing expanding belly on inhale.",
            benefits = listOf(
                "Enhances blood circulation to digestive and pelvic organs",
                "Relieves acidity, gas, and digestive sluggishness",
                "Stretches quadriceps and ankle extensors",
                "Improves meditation posture stability"
            ),
            precautions = listOf(
                "Avoid with severe osteoarthritis of the knees or acute ankle sprain"
            ),
            beginnerModifications = "Place a yoga block horizontally between ankles to sit upon."
        ),

        DetailedYogaPose(
            id = "shavasana",
            name = "Corpse Pose",
            sanskritName = "Shavasana",
            category = YogaCategoryNames.EVENING,
            level = "Beginner",
            holdSeconds = 180,
            emoji = "🕊️",
            targetBodyAreas = "Nervous System, Full Body",
            caloriesPerMin = 1.0f,
            description = "The ultimate posture of conscious relaxation, integrating the physical and energetic benefits of your practice.",
            steps = listOf(
                "Lie flat on back with legs extended comfortably wide, feet falling naturally outward.",
                "Place arms alongside body, palms turned up toward ceiling.",
                "Tuck shoulder blades gently under chest to create open breathing space.",
                "Close eyes softly and release all muscular tension.",
                "Surrender body weight to the mat and let breath return to its natural rhythm.",
                "Remain completely still for 3 to 10 minutes."
            ),
            breathingInstructions = "Surrendered effortless breathing; release conscious control of the breath.",
            benefits = listOf(
                "Lowers blood pressure and heart rate",
                "Reduces insomnia, anxiety, and mental fatigue",
                "Integrates neuromotor adaptations from asana practice",
                "Restores parasympathetic harmony"
            ),
            precautions = listOf(
                "If lower back feels strained, place a bolster or pillow beneath knees"
            ),
            beginnerModifications = "Place a folded towel under head and a bolster under knees."
        ),

        DetailedYogaPose(
            id = "virabhadrasana_1",
            name = "Warrior I",
            sanskritName = "Virabhadrasana I",
            category = YogaCategoryNames.FULL_BODY,
            level = "Beginner",
            holdSeconds = 45,
            emoji = "⚔️",
            targetBodyAreas = "Quadriceps, Glutes, Hip Flexors, Shoulders",
            caloriesPerMin = 4.0f,
            description = "A powerful standing lunge that cultivates courage, lower-body stability, and open thoracic posture.",
            steps = listOf(
                "From standing, step left foot back about 3.5 to 4 feet.",
                "Angle left back foot 45 degrees forward, grounding outer heel.",
                "Bend right front knee directly over right ankle, thigh parallel to mat.",
                "Square hips and chest toward front of mat.",
                "Inhale and sweep arms overhead, palms facing each other.",
                "Gaze upward past fingertips and hold for 5 breaths."
            ),
            breathingInstructions = "Inhale reach up through fingertips; exhale sink hips and ground back heel.",
            benefits = listOf(
                "Strengthens thighs, calves, and ankle stabilizers",
                "Stretches chest, lungs, and psoas muscles",
                "Develops mental focus and internal stamina",
                "Improves pelvic alignment"
            ),
            precautions = listOf(
                "Do not allow front knee to drift past toes",
                "Avoid full neck extension if prone to neck strain"
            ),
            beginnerModifications = "Widen stance horizontally (train tracks) for easier hip squaring."
        ),

        DetailedYogaPose(
            id = "virabhadrasana_2",
            name = "Warrior II",
            sanskritName = "Virabhadrasana II",
            category = YogaCategoryNames.FULL_BODY,
            level = "Beginner",
            holdSeconds = 45,
            emoji = "🤺",
            targetBodyAreas = "Hips, Groin, Thighs, Shoulders",
            caloriesPerMin = 4.2f,
            description = "An iconic warrior pose requiring fierce concentration, open hips, and endurance across shoulders and legs.",
            steps = listOf(
                "Step feet wide (4 feet apart). Turn right foot out 90 degrees, left foot parallel to back edge.",
                "Inhale arms parallel to floor, reaching actively through fingertips.",
                "Exhale and bend right knee until shin is vertical, knee tracking over second toe.",
                "Keep torso centered directly over hips (do not lean forward).",
                "Turn gaze smoothly over right front middle finger.",
                "Hold with steady breath for 45 seconds, then switch sides."
            ),
            breathingInstructions = "Smooth even breath; ground on exhale, expand arm span on inhale.",
            benefits = listOf(
                "Strengthens quadriceps, gluteus medius, and adductors",
                "Deep hip opening for pelvic flexibility",
                "Tones abdominal core and shoulder stabilizers",
                "Builds endurance and willpower"
            ),
            precautions = listOf(
                "Keep front knee from collapsing inward",
                "Rest arms down briefly if shoulders fatigue"
            ),
            beginnerModifications = "Shorten the stance slightly or straighten front leg momentarily to rest."
        ),

        DetailedYogaPose(
            id = "cat_cow",
            name = "Cat-Cow Flow",
            sanskritName = "Marjaryasana-Bitilasana",
            category = YogaCategoryNames.BACK_FOCUSED,
            level = "Beginner",
            holdSeconds = 60,
            emoji = "🐈",
            targetBodyAreas = "Spine, Neck, Core, Pelvis",
            caloriesPerMin = 2.4f,
            description = "A gentle flowing movement linking spinal flexion and extension with conscious breathing.",
            steps = listOf(
                "Begin on tabletop: wrists beneath shoulders, knees under hips.",
                "Inhale into Cow: drop belly toward mat, lift chest and tailbone, gaze up.",
                "Exhale into Cat: draw belly to spine, round back toward ceiling, tuck chin.",
                "Flow continuously between both positions with your breath.",
                "Move smoothly without forcing range of motion.",
                "Continue for 10-12 breath cycles."
            ),
            breathingInstructions = "Inhale arching into Cow; exhale rounding into Cat.",
            benefits = listOf(
                "Warms up and lubricates spinal discs and vertebrae",
                "Massages abdominal and internal organs",
                "Releases emotional tension stored in neck and upper back",
                "Syncs respiration with physical movement"
            ),
            precautions = listOf(
                "Keep neck movement gentle; do not fling head back"
            ),
            beginnerModifications = "Place a folded towel under knees for joint comfort."
        ),

        DetailedYogaPose(
            id = "utkatasana",
            name = "Chair Pose",
            sanskritName = "Utkatasana",
            category = YogaCategoryNames.WEIGHT_LOSS,
            level = "Intermediate",
            holdSeconds = 40,
            emoji = "🪑",
            targetBodyAreas = "Quadriceps, Glutes, Ankles, Core",
            caloriesPerMin = 4.8f,
            description = "A fierce standing posture that rapidly generates heat, fires the quadriceps, and activates core engagement.",
            steps = listOf(
                "Stand with feet together or hip-width apart.",
                "Inhale and raise arms overhead alongside ears.",
                "Exhale and bend knees, sitting hips back as if into an imaginary chair.",
                "Shift weight into heels so toes can wiggle lightly.",
                "Keep chest proud, drawing shoulder blades down back.",
                "Hold for 30-40 seconds, breathing steadily."
            ),
            breathingInstructions = "Inhale lengthen spine and arms; exhale sink hips deeper.",
            benefits = listOf(
                "Stimulates high caloric burn and metabolic rate",
                "Strengthens thighs, calves, and spinal support muscles",
                "Tones glutes and core abdominal wall",
                "Builds endurance and mental grit"
            ),
            precautions = listOf(
                "Do not allow knees to track inward or past toes",
                "Avoid if experiencing acute knee ligament pain"
            ),
            beginnerModifications = "Practice with back resting against a smooth wall."
        ),

        DetailedYogaPose(
            id = "navasana",
            name = "Boat Pose",
            sanskritName = "Navasana",
            category = YogaCategoryNames.WEIGHT_LOSS,
            level = "Intermediate",
            holdSeconds = 35,
            emoji = "⛵",
            targetBodyAreas = "Deep Abdominals, Hip Flexors, Lower Back",
            caloriesPerMin = 4.5f,
            description = "A premier core strengthener that demands engagement of the transverse abdominis, psoas, and spinal erecters.",
            steps = listOf(
                "Sit with knees bent, feet flat on mat, hands behind thighs.",
                "Lean torso back slightly, maintaining a straight spine without rounding.",
                "Lift feet until shins are parallel to the floor.",
                "Release hands and reach arms forward alongside knees, palms facing in.",
                "If comfortable, straighten legs to a 45-degree angle forming a 'V'.",
                "Hold for 20-35 seconds, drawing low belly in."
            ),
            breathingInstructions = "Short controlled breaths into chest; avoid holding your breath.",
            benefits = listOf(
                "Deeply strengthens core stabilizers and rectus abdominis",
                "Stimulates digestion and kidney function",
                "Improves balance and hip flexor strength",
                "Aids in trim waistline development"
            ),
            precautions = listOf(
                "Avoid with acute lower back disc injury or hernia",
                "Keep knees bent if lower back rounds"
            ),
            beginnerModifications = "Keep knees bent with shins parallel, or keep fingertips on floor."
        ),

        DetailedYogaPose(
            id = "paschimottanasana",
            name = "Seated Forward Bend",
            sanskritName = "Paschimottanasana",
            category = YogaCategoryNames.FLEXIBILITY,
            level = "Beginner",
            holdSeconds = 60,
            emoji = "🙇",
            targetBodyAreas = "Hamstrings, Calves, Entire Spine",
            caloriesPerMin = 2.0f,
            description = "A soothing forward fold that intensely stretches the entire posterior chain from heels to neck.",
            steps = listOf(
                "Sit tall with legs extended straight forward, feet flexed.",
                "Inhale and reach arms high overhead, lengthening spine.",
                "Exhale and hinge forward from hips, leading with chest.",
                "Hold feet, ankles, or shins with hands without rounding spine aggressively.",
                "With each inhale find length; with each exhale surrender deeper.",
                "Hold for 1 to 2 minutes with calm breaths."
            ),
            breathingInstructions = "Inhale create length in spine; exhale soften belly onto thighs.",
            benefits = listOf(
                "Deeply stretches hamstrings, calves, and lower back",
                "Calms brain and eases mild depression and anxiety",
                "Massages liver, kidneys, and spleen",
                "Helps relieve insomnia and stress"
            ),
            precautions = listOf(
                "Never force head to knees by aggressively pulling with arms",
                "Bend knees if hamstrings are tight to protect lower back"
            ),
            beginnerModifications = "Loop a yoga strap around balls of feet and keep knees slightly bent."
        ),

        DetailedYogaPose(
            id = "viparita_karani",
            name = "Legs-Up-the-Wall Pose",
            sanskritName = "Viparita Karani",
            category = YogaCategoryNames.EVENING,
            level = "Beginner",
            holdSeconds = 180,
            emoji = "🧱",
            targetBodyAreas = "Circulation, Lymphatic System, Nervous System",
            caloriesPerMin = 1.0f,
            description = "A supremely restorative inversion that drains stagnant venous fluid from legs, soothes tired feet, and calms the heart.",
            steps = listOf(
                "Sit close to a clear wall with your side touching it.",
                "Pivot hips and swing legs up the wall while lowering torso back onto mat.",
                "Bring sitting bones as close to wall as comfortable.",
                "Rest arms out to sides in cactus or surrender position.",
                "Close eyes and let gravity drain tired legs for 3-5 minutes."
            ),
            breathingInstructions = "Long slow abdominal breathing.",
            benefits = listOf(
                "Relieves swollen legs, varicose veins, and ankle fatigue",
                "Slows heart rate and calms autonomic nervous system",
                "Relieves headaches and promotes sound sleep",
                "Soothes lower back pressure"
            ),
            precautions = listOf(
                "Avoid with glaucoma or high blood pressure"
            ),
            beginnerModifications = "Place a folded blanket beneath pelvis for extra sacral support."
        ),

        DetailedYogaPose(
            id = "ustrasana",
            name = "Camel Pose",
            sanskritName = "Ustrasana",
            category = YogaCategoryNames.BACK_FOCUSED,
            level = "Intermediate",
            holdSeconds = 35,
            emoji = "🐪",
            targetBodyAreas = "Chest, Throat, Psoas, Spine",
            caloriesPerMin = 3.6f,
            description = "An expansive kneeling backbend that counteracts desk hunch, opens respiratory passages, and stimulates vitality.",
            steps = listOf(
                "Kneel on mat, hips over knees, shins and tops of feet pressing down.",
                "Place hands on sacrum, fingers pointing down.",
                "Inhale, press pelvis forward, and lift chest toward ceiling.",
                "Begin arching upper back back; if comfortable, reach hands back to grasp heels.",
                "Keep neck long; do not drop head back uncomfortably.",
                "Hold for 25-35 seconds, then lift up smoothly using core."
            ),
            breathingInstructions = "Deep thoracic breathing opening the upper chest.",
            benefits = listOf(
                "Dramatically opens chest and increases lung tidal volume",
                "Stretches hip flexors, quadriceps, and abdominal wall",
                "Improves posture and spinal flexibility",
                "Stimulates endocrine and respiratory function"
            ),
            precautions = listOf(
                "Avoid with severe neck or back conditions",
                "Keep hands on lower back if heels are out of reach"
            ),
            beginnerModifications = "Tuck toes under to elevate heels, or keep hands supporting the lower back."
        ),

        DetailedYogaPose(
            id = "plank",
            name = "Plank Pose",
            sanskritName = "Phalakasana",
            category = YogaCategoryNames.WEIGHT_LOSS,
            level = "Beginner",
            holdSeconds = 45,
            emoji = "🪵",
            targetBodyAreas = "Core, Shoulders, Arms, Glutes",
            caloriesPerMin = 4.5f,
            description = "An essential total-body isometric hold building foundational core stability, wrist strength, and shoulder endurance.",
            steps = listOf(
                "From all fours, step feet back to form a straight line from heels to crown.",
                "Stack wrists directly under shoulders, fingers spread wide.",
                "Engage quadriceps, draw tailbone toward heels, and tuck navel to spine.",
                "Push floor away with hands to broaden between shoulder blades.",
                "Gaze slightly forward of fingers; hold for 30-45 seconds."
            ),
            breathingInstructions = "Steady rhythmic breathing through the nose; do not hold breath.",
            benefits = listOf(
                "Strengthens deep abdominal transverse muscles",
                "Tones arms, wrists, and serratus anterior",
                "Reinforces neutral spinal alignment",
                "Prepares body for advanced arm balances"
            ),
            precautions = listOf(
                "Avoid if suffering from acute carpal tunnel syndrome",
                "Do not allow hips to sag or pike excessively"
            ),
            beginnerModifications = "Lower knees to the mat for Half Plank while maintaining straight torso."
        ),

        DetailedYogaPose(
            id = "bakasana",
            name = "Crow Pose",
            sanskritName = "Bakasana",
            category = YogaCategoryNames.ADVANCED,
            level = "Advanced",
            holdSeconds = 25,
            emoji = "🦅",
            targetBodyAreas = "Wrists, Triceps, Core, Inner Thighs",
            caloriesPerMin = 5.0f,
            description = "The foundational arm balance that develops body lightness, wrist strength, and deep core compaction.",
            steps = listOf(
                "Squat down with feet close together, knees wide apart.",
                "Place hands flat on floor shoulder-width apart, fingers spread wide.",
                "Lift hips high and rest knees against triceps or high in armpits.",
                "Shift weight forward into hands, gaze forward onto the floor.",
                "Gently lift one foot, then the other, balancing entirely on palms.",
                "Squeeze inner thighs into arms, engage core, and hold for 15-25 seconds."
            ),
            breathingInstructions = "Short focused breaths maintaining abdominal pressure.",
            benefits = listOf(
                "Builds incredible wrist, arm, and shoulder strength",
                "Cultivates deep neuromuscular confidence and balance",
                "Strengthens abdominal muscles and pelvic floor",
                "Expands upper back"
            ),
            precautions = listOf(
                "Avoid during pregnancy or with carpal tunnel syndrome",
                "Place a cushion in front of your head for confidence"
            ),
            beginnerModifications = "Practice lifting just one foot at a time while keeping other toes grounded."
        )
    )

    // ── 3. Predefined Guided Sessions ─────────────────────────────────────────
    val guidedSessions = listOf(
        PredefinedYogaSession(
            id = "session_morning",
            title = "Morning Awakening Flow",
            subtitle = "15 min • Energizing • All Levels",
            category = YogaCategoryNames.MORNING,
            durationMinutes = 15,
            difficulty = "Beginner",
            poseIds = listOf("tadasana", "cat_cow", "adho_mukha", "virabhadrasana_1", "bhujangasana", "balasana", "shavasana"),
            emoji = "🌅"
        ),
        PredefinedYogaSession(
            id = "session_flexibility",
            title = "Full Body Flexibility Flow",
            subtitle = "15 min • Deep Stretch • 8 Poses",
            category = YogaCategoryNames.FLEXIBILITY,
            durationMinutes = 15,
            difficulty = "All Levels",
            poseIds = listOf("tadasana", "trikonasana", "adho_mukha", "virabhadrasana_2", "bhujangasana", "paschimottanasana", "balasana", "shavasana"),
            emoji = "🤸"
        ),
        PredefinedYogaSession(
            id = "session_stress_relief",
            title = "Mindful Stress Relief & Calm",
            subtitle = "14 min • Relaxation • Restorative",
            category = YogaCategoryNames.STRESS_RELIEF,
            durationMinutes = 14,
            difficulty = "Beginner",
            poseIds = listOf("sukhasana", "cat_cow", "balasana", "setu_bandhasana", "viparita_karani", "shavasana"),
            emoji = "🧘"
        ),
        PredefinedYogaSession(
            id = "session_evening",
            title = "Evening Bedtime Wind-Down",
            subtitle = "12 min • Sleep Prep • Gentle",
            category = YogaCategoryNames.EVENING,
            durationMinutes = 12,
            difficulty = "Beginner",
            poseIds = listOf("vajrasana", "balasana", "paschimottanasana", "setu_bandhasana", "viparita_karani", "shavasana"),
            emoji = "🌙"
        ),
        PredefinedYogaSession(
            id = "session_back_relief",
            title = "Back Relief & Spine Gentle Flow",
            subtitle = "12 min • Gentle Therapy • 6 Poses",
            category = YogaCategoryNames.BACK_FOCUSED,
            durationMinutes = 12,
            difficulty = "Gentle",
            poseIds = listOf("cat_cow", "adho_mukha", "bhujangasana", "balasana", "setu_bandhasana", "shavasana"),
            emoji = "✨"
        ),
        PredefinedYogaSession(
            id = "session_weight_loss",
            title = "Dynamic Power & Burn Flow",
            subtitle = "20 min • Core & Calorie Burn",
            category = YogaCategoryNames.WEIGHT_LOSS,
            durationMinutes = 20,
            difficulty = "Intermediate",
            poseIds = listOf("tadasana", "plank", "adho_mukha", "virabhadrasana_1", "virabhadrasana_2", "utkatasana", "navasana", "bhujangasana", "balasana"),
            emoji = "🔥"
        ),
        PredefinedYogaSession(
            id = "session_meditation",
            title = "Mindfulness & Stillness Flow",
            subtitle = "10 min • Breath & Meditation",
            category = YogaCategoryNames.MEDITATION,
            durationMinutes = 10,
            difficulty = "All Levels",
            poseIds = listOf("sukhasana", "vajrasana", "balasana", "viparita_karani", "shavasana"),
            emoji = "🪷"
        ),
        PredefinedYogaSession(
            id = "session_beginner_flow",
            title = "Beginner Foundation Flow",
            subtitle = "12 min • Posture & Alignment",
            category = YogaCategoryNames.BEGINNER,
            durationMinutes = 12,
            difficulty = "Beginner",
            poseIds = listOf("tadasana", "vrikshasana", "trikonasana", "cat_cow", "balasana", "shavasana"),
            emoji = "🌱"
        ),
        PredefinedYogaSession(
            id = "session_full_body",
            title = "Complete Full Body Harmony",
            subtitle = "20 min • Comprehensive Sequence",
            category = YogaCategoryNames.FULL_BODY,
            durationMinutes = 20,
            difficulty = "Intermediate",
            poseIds = listOf("tadasana", "vrikshasana", "adho_mukha", "virabhadrasana_1", "virabhadrasana_2", "plank", "bhujangasana", "navasana", "balasana", "shavasana"),
            emoji = "⚡"
        ),
        PredefinedYogaSession(
            id = "session_advanced",
            title = "Advanced Peak Balance Flow",
            subtitle = "25 min • Inversions & Balance",
            category = YogaCategoryNames.ADVANCED,
            durationMinutes = 25,
            difficulty = "Advanced",
            poseIds = listOf("tadasana", "adho_mukha", "plank", "bakasana", "ustrasana", "navasana", "setu_bandhasana", "shavasana"),
            emoji = "👑"
        )
    )

    // ── 4. The 4 Personalized Predefined Plans ────────────────────────────────
    val personalizedPlans = listOf(
        PersonalizedYogaPlan(
            id = "plan_7_day_beginner",
            title = "7-Day Beginner Yoga",
            description = "Build a solid foundation with gentle daily postures, breath awareness, and posture alignment.",
            totalDays = 7,
            difficulty = "Beginner",
            dailyGoalMinutes = 15,
            emoji = "🌱",
            colorHex = 0xFF10B981,
            dailyRoutines = listOf(
                PlanDailyRoutine(1, "Day 1: Grounding & Posture", 15, listOf("tadasana", "cat_cow", "balasana", "shavasana"), "Focus on finding balance and steady breathing."),
                PlanDailyRoutine(2, "Day 2: Gentle Spine Opening", 15, listOf("cat_cow", "bhujangasana", "balasana", "sukhasana"), "Release lower and upper back stiffness."),
                PlanDailyRoutine(3, "Day 3: Standing Balance", 15, listOf("tadasana", "vrikshasana", "balasana", "shavasana"), "Strengthen feet, ankles, and focus."),
                PlanDailyRoutine(4, "Day 4: Deep Hamstring Length", 15, listOf("tadasana", "trikonasana", "paschimottanasana", "balasana"), "Safely stretch hamstrings and calves."),
                PlanDailyRoutine(5, "Day 5: Downward Dog Flow", 15, listOf("adho_mukha", "cat_cow", "setu_bandhasana", "shavasana"), "Build upper body stability and inverted energy."),
                PlanDailyRoutine(6, "Day 6: Rest & Digestion", 15, listOf("vajrasana", "balasana", "viparita_karani", "shavasana"), "Calm nervous system and support digestion."),
                PlanDailyRoutine(7, "Day 7: Full Beginner Harmony", 15, listOf("tadasana", "vrikshasana", "adho_mukha", "bhujangasana", "shavasana"), "Celebrate your first week of mindful yoga!")
            )
        ),

        PersonalizedYogaPlan(
            id = "plan_14_day_flexibility",
            title = "14-Day Flexibility Plan",
            description = "Progressive daily stretching designed to unlock tight hips, lengthen hamstrings, and mobilize shoulders.",
            totalDays = 14,
            difficulty = "All Levels",
            dailyGoalMinutes = 18,
            emoji = "🤸",
            colorHex = 0xFF06B6D4,
            dailyRoutines = (1..14).map { day ->
                val title = when (day % 4) {
                    1 -> "Day $day: Hip Opener Sequence"
                    2 -> "Day $day: Hamstring & Calf Release"
                    3 -> "Day $day: Shoulder & Spine Mobility"
                    else -> "Day $day: Full Body Elastic Flow"
                }
                val posesList = when (day % 4) {
                    1 -> listOf("tadasana", "vrikshasana", "balasana", "setu_bandhasana", "shavasana")
                    2 -> listOf("trikonasana", "adho_mukha", "paschimottanasana", "shavasana")
                    3 -> listOf("cat_cow", "bhujangasana", "ustrasana", "balasana", "shavasana")
                    else -> listOf("tadasana", "virabhadrasana_2", "trikonasana", "paschimottanasana", "shavasana")
                }
                PlanDailyRoutine(day, title, 18, posesList, "Deepen range of motion and breath expansion.")
            }
        ),

        PersonalizedYogaPlan(
            id = "plan_21_day_morning",
            title = "21-Day Morning Yoga Habit",
            description = "Transform your mornings with a revitalizing daily habit that wakes up your body, clears brain fog, and ignites energy.",
            totalDays = 21,
            difficulty = "All Levels",
            dailyGoalMinutes = 15,
            emoji = "🌅",
            colorHex = 0xFFF59E0B,
            dailyRoutines = (1..21).map { day ->
                val title = "Day $day: Morning Sunrise Flow"
                val posesList = listOf("tadasana", "cat_cow", "adho_mukha", "virabhadrasana_1", "bhujangasana", "shavasana")
                PlanDailyRoutine(day, title, 15, posesList, "Start your day with intention and grounded energy.")
            }
        ),

        PersonalizedYogaPlan(
            id = "plan_30_day_challenge",
            title = "30-Day Yoga Challenge",
            description = "The ultimate transformation journey. Build strength, endurance, supreme flexibility, and serene inner calm.",
            totalDays = 30,
            difficulty = "Intermediate",
            dailyGoalMinutes = 20,
            emoji = "🏆",
            colorHex = 0xFF8B5CF6,
            dailyRoutines = (1..30).map { day ->
                val focus = when (day % 5) {
                    1 -> "Strength & Warrior Endurance"
                    2 -> "Deep Spinal Decompression"
                    3 -> "Core Fire & Metabolism"
                    4 -> "Restorative Stillness"
                    else -> "Peak Posture Balance"
                }
                val posesList = when (day % 5) {
                    1 -> listOf("tadasana", "virabhadrasana_1", "virabhadrasana_2", "plank", "shavasana")
                    2 -> listOf("cat_cow", "bhujangasana", "setu_bandhasana", "balasana", "shavasana")
                    3 -> listOf("utkatasana", "plank", "navasana", "adho_mukha", "shavasana")
                    4 -> listOf("sukhasana", "vajrasana", "viparita_karani", "shavasana")
                    else -> listOf("vrikshasana", "trikonasana", "ustrasana", "bakasana", "shavasana")
                }
                PlanDailyRoutine(day, "Day $day: $focus", 20, posesList, "Step closer to mastery in your 30-day transformation.")
            }
        )
    )

    // ── Helper Resolvers ──────────────────────────────────────────────────────
    fun getPoseById(id: String): DetailedYogaPose? {
        return poses.find { it.id == id }
    }

    fun getCategoryById(id: String): YogaCategory? {
        return categories.find { it.id == id }
    }

    fun getPlanById(id: String): PersonalizedYogaPlan? {
        return personalizedPlans.find { it.id == id }
    }

    fun getPosesForCategory(categoryName: String): List<DetailedYogaPose> {
        return poses.filter { it.category.equals(categoryName, ignoreCase = true) }
    }
}
