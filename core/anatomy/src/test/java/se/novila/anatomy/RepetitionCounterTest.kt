package se.novila.anatomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class RepetitionCounterTest {
    private val moving = ExerciseLibrary.all.filterNot { it.isHold }

    @Test fun `counts every exercise through noisy observations`() {
        moving.forEach { motion ->
            val counter = RepetitionCounter(motion)
            val repetitions = observe(counter, repetitions = 10, noise = NOISE)
            assertEquals(motion.id, 10, counter.count)
            assertTrue(motion.id, repetitions.all { it.complete && !it.rushed })
        }
    }

    @Test fun `holds have nothing to count`() {
        val failure = runCatching { RepetitionCounter(ExerciseLibrary.plank) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test fun `standing still counts nothing`() {
        val counter = RepetitionCounter(ExerciseLibrary.squat)
        val random = Random(SEED)
        val repetitions = (0 until 300).mapNotNull {
            counter.add(it * FRAME_MILLIS, noisy(Pose.STANDING, random, NOISE))
        }
        assertEquals(0, counter.count)
        assertTrue(repetitions.isEmpty())
    }

    @Test fun `half squats are attempts, not repetitions`() {
        val squat = ExerciseLibrary.squat
        val bottom = squat.movement.poseAt(0.5f)
        val half = ExerciseMotion(
            id = "half_squat",
            movement = Movement(
                listOf(
                    Keyframe(0f, Pose.STANDING),
                    Keyframe(0.5f, Pose.STANDING.lerpTo(bottom, 0.6f)),
                    Keyframe(1f, Pose.STANDING),
                ),
            ),
            primary = squat.primary,
            secondary = squat.secondary,
            contacts = squat.contacts,
            anchor = squat.anchor,
            camera = squat.camera,
        )
        val counter = RepetitionCounter(squat)
        val repetitions = observe(counter, repetitions = 5, noise = NOISE, playing = half)
        assertEquals(0, counter.count)
        assertEquals(5, repetitions.size)
        assertTrue(repetitions.all { !it.complete && it.depth in 0.5f..0.7f })
    }

    @Test fun `fast repetitions are rushed`() {
        val squat = ExerciseLibrary.squat
        val counter = RepetitionCounter(squat)
        val repetitions = observe(counter, repetitions = 6, noise = NOISE, speed = 3f)
        assertEquals(6, counter.count)
        assertTrue(repetitions.all { it.rushed })
    }

    @Test fun `another exercise is not counted`() {
        val counter = RepetitionCounter(ExerciseLibrary.squat)
        observe(counter, repetitions = 10, noise = NOISE, playing = ExerciseLibrary.jumpingJack)
        assertEquals(0, counter.count)
    }

    @Test fun `poses off the movement are ignored`() {
        val counter = RepetitionCounter(ExerciseLibrary.squat)
        assertNull(counter.progressOf(Pose.symmetric(leg = LimbPose(raise = -60f), arm = LimbPose(raise = 170f))))
        assertEquals(0f, counter.progressOf(Pose.STANDING)!!, 1e-4f)
        assertEquals(1f, counter.progressOf(ExerciseLibrary.squat.movement.poseAt(0.5f))!!, 1e-4f)
    }

    @Test fun `mountain climbers count once per left and right`() {
        val counter = RepetitionCounter(ExerciseLibrary.mountainClimber)
        observe(counter, repetitions = 8, noise = NOISE)
        assertEquals(8, counter.count)
        val rightKnee = ExerciseLibrary.mountainClimber.movement.poseAt(0.75f)
        assertNull("the right knee is off the left knee's line", counter.progressOf(rightKnee))
    }

    /** Plays [playing] for [repetitions] at 30 frames a second, with uniform noise on every angle. */
    private fun observe(
        counter: RepetitionCounter,
        repetitions: Int,
        noise: Float,
        playing: ExerciseMotion = counter.motion,
        speed: Float = 1f,
    ): List<Repetition> {
        val random = Random(SEED)
        val millis = playing.repetitionMillis / speed
        val frames = (repetitions * millis / FRAME_MILLIS).toInt()
        return (0..frames + REST_FRAMES).mapNotNull { frame ->
            val time = frame * FRAME_MILLIS
            val progress = minOf(time / millis, repetitions.toFloat())
            counter.add(time, noisy(playing.movement.poseAt(progress), random, noise))
        }
    }

    private fun noisy(pose: Pose, random: Random, noise: Float): Pose {
        fun n(value: Float) = value + (random.nextFloat() * 2f - 1f) * noise
        fun limb(limb: LimbPose) = LimbPose(n(limb.raise), n(limb.spread), n(limb.bend))
        return pose.copy(
            lean = n(pose.lean),
            trunk = n(pose.trunk),
            neck = n(pose.neck),
            leftLeg = limb(pose.leftLeg),
            rightLeg = limb(pose.rightLeg),
            leftArm = limb(pose.leftArm),
            rightArm = limb(pose.rightArm),
            leftFoot = n(pose.leftFoot),
            rightFoot = n(pose.rightFoot),
        )
    }

    private companion object {
        const val FRAME_MILLIS = 33L
        const val REST_FRAMES = 15
        const val NOISE = 5f
        const val SEED = 7L
    }
}
