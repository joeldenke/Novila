package se.novila.anatomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnatomyTest {
    private val samples = (0..48).map { it / 48f }

    @Test fun `repetitions loop seamlessly`() {
        ExerciseLibrary.all.forEach { motion ->
            val keyframes = motion.movement.keyframes
            assertEquals(motion.id, keyframes.first().pose, keyframes.last().pose)
            assertEquals(motion.id, keyframes.first().pose, motion.movement.poseAt(1f))
        }
    }

    @Test fun `poses ease between keyframes`() {
        val movement = Movement(listOf(Keyframe(0f, Pose()), Keyframe(0.5f, Pose(lean = 90f)), Keyframe(1f, Pose())))
        assertEquals(0f, movement.poseAt(0f).lean, 1e-4f)
        assertEquals(45f, movement.poseAt(0.25f).lean, 1e-4f)
        assertEquals(90f, movement.poseAt(0.5f).lean, 1e-4f)
        assertTrue("eases in", movement.poseAt(0.05f).lean < 9f)
        assertEquals("next repetition", movement.poseAt(0.25f), movement.poseAt(2.25f))
    }

    @Test fun `planted hands and feet stay put`() {
        ExerciseLibrary.all.filter { it.anchor != null }.forEach { motion ->
            val start = motion.bodyAt(0f).landmarks
            motion.contacts.forEach { contact ->
                val drift = samples.maxOf {
                    (motion.bodyAt(it).landmarks.getValue(contact) - start.getValue(contact)).length
                }
                assertTrue("${motion.id} $contact drifts ${"%.3f".format(drift)}", drift < MAX_DRIFT)
            }
        }
    }

    @Test fun `contacts never sink into the floor`() {
        ExerciseLibrary.all.forEach { motion ->
            val floor = SceneBuilder.floorHeight(motion.bodyAt(0f), motion.contacts)
            samples.forEach { progress ->
                val body = motion.bodyAt(progress)
                motion.contacts.forEach { contact ->
                    val bottom = body.landmarks.getValue(contact).y - Skeleton.CONTACT_RADIUS.getValue(contact)
                    assertTrue("${motion.id} $contact at $progress", bottom > floor - 0.008f)
                }
            }
        }
    }

    @Test fun `each exercise is shown from its best view`() {
        ExerciseLibrary.all.forEach { motion ->
            val best = ViewSelector.best(motion)
            assertEquals("${motion.id}: set camera = Camera(${best.yaw}f, pitch = ${best.pitch}f)", best, motion.camera)
        }
    }

    @Test fun `the chosen view shows the main muscles at the peak`() {
        ExerciseLibrary.all.forEach { motion ->
            val visible = ViewSelector.visibleArea(motion.sceneAt(motion.peak))
            assertTrue(motion.id, (visible[Emphasis.Primary] ?: 0f) > 0.0005f)
        }
    }

    @Test fun `exercises have unique ids and train something`() {
        assertEquals(ExerciseLibrary.all.size, ExerciseLibrary.all.map { it.id }.toSet().size)
        ExerciseLibrary.all.forEach { motion ->
            assertTrue(motion.id, motion.primary.isNotEmpty())
            assertTrue(motion.id, motion.primary.intersect(motion.secondary).isEmpty())
            assertEquals(motion, ExerciseLibrary.byId(motion.id))
        }
    }

    @Test fun `gear is drawn in the hands of exercises that need it`() {
        ExerciseLibrary.all.forEach { motion ->
            val gear = motion.sceneAt(motion.peak).layers.filterIsInstance<Layer.Gear>()
            assertEquals(motion.id, motion.gear != null, gear.isNotEmpty())
        }
        Equipment.entries.forEach { equipment ->
            assertTrue("$equipment", ExerciseLibrary.all.any { it.equipment == equipment })
        }
        val curl = ExerciseLibrary.byId("biceps_curl")!!
        samples.forEach { progress ->
            val body = curl.bodyAt(progress)
            val hands = listOf(Landmark.LeftHand, Landmark.RightHand).map { body.landmarks.getValue(it) }
            curl.gear!!.props(body).filter { it.kind == PropKind.Bar }.forEach { handle ->
                val middle = (handle.start + handle.end) * 0.5f
                assertTrue("handle in a hand at $progress", hands.any { (it - middle).length < 0.06f })
            }
        }
    }

    @Test fun `a band stays tied to its anchor`() {
        ExerciseLibrary.all.forEach { motion ->
            val band = motion.gear as? Gear.Band ?: return@forEach
            samples.forEach { progress ->
                val ends = band.props(motion.bodyAt(progress)).map { it.end }
                assertEquals(motion.id, Side.entries.map { band.anchorFor(it) }, ends)
            }
        }
    }

    @Test fun `a weight plate seen face-on is round and edge-on is thin`() {
        val plate = Prop(PropKind.Weight, Vec3(-0.01f, 0f, 0f), Vec3(0.01f, 0f, 0f), 0.06f)
        val faceOn = Bounds.of(propOutline(plate, Camera(90f, pitch = 0f)))
        assertEquals(faceOn.width, faceOn.height, 0.005f)
        val edgeOn = Bounds.of(propOutline(plate, Camera(0f, pitch = 0f)))
        assertTrue(edgeOn.width < edgeOn.height / 2f)
    }

    @Test fun `every muscle group has a place on the body`() {
        val mapped = MuscleMap.regions.map { it.group }.toSet()
        assertEquals(MuscleGroup.entries.toSet(), mapped)
    }

    @Test fun `standing body has average proportions`() {
        val body = ExerciseLibrary.squat.bodyAt(0f)
        val floor = SceneBuilder.floorHeight(body, ExerciseLibrary.squat.contacts)
        val crown = body.segments.first { it.part == BodyPart.Head }.let { it.end.y + it.endRadii.depth }
        assertEquals("one body height tall", 1f, crown - floor, 0.03f)
    }

    @Test fun `body shape widens shoulders and hips`() {
        fun width(shape: BodyShape, part: BodyPart) = Skeleton.pose(Pose.STANDING, shape).segments
            .filter { it.part == part }
            .let { (left, right) -> left.start.x - right.start.x }
        val broad = BodyShape(shoulders = 1.2f, hips = 1.2f)
        assertTrue(width(broad, BodyPart.UpperArm) > width(BodyShape.NEUTRAL, BodyPart.UpperArm))
        assertTrue(width(broad, BodyPart.Thigh) > width(BodyShape.NEUTRAL, BodyPart.Thigh))
    }

    @Test fun `the near side is drawn over the far side`() {
        // Seen from the right (yaw 90), the right leg is nearer and must come later.
        val scene = SceneBuilder.build(Skeleton.pose(Pose.STANDING), Camera(90f), emptyMap())
        val parts = scene.layers.filterIsInstance<Layer.Part>().map { it.part }
        val thighs = parts.withIndex().filter { it.value == BodyPart.Thigh }
        assertEquals(2, thighs.size)
        val shades = scene.layers.filterIsInstance<Layer.Part>().filter { it.part == BodyPart.Thigh }.map { it.shade }
        assertTrue("far thigh is shaded", shades.first() > shades.last())
    }

    @Test fun `muscles turn out of sight`() {
        val body = Skeleton.pose(Pose.STANDING)
        val quads = mapOf(MuscleGroup.Quads to Emphasis.Primary)
        fun quadLayers(camera: Camera) = SceneBuilder.build(body, camera, quads).layers.count { it is Layer.Muscle }
        assertEquals("both quads from the front", 2, quadLayers(Camera(0f)))
        assertEquals("none from behind", 0, quadLayers(Camera(180f)))
    }

    @Test fun `rotations keep lengths`() {
        val v = Vec3(0.3f, -0.4f, 0.5f)
        val r = Rotation.aboutX(33f) * Rotation.aboutY(-71f) * Rotation.aboutZ(12f)
        assertEquals(v.length, (r * v).length, 1e-5f)
        assertEquals(1f, (Rotation.aboutX(90f) * Vec3(0f, 1f, 0f)).z, 1e-5f)
    }

    private companion object {
        const val MAX_DRIFT = 0.02f
    }
}
