package net.joth.grabby.client


// [@JvmField] is what makes this accessible to mixins as far as i understand it

object GrabbyRotateState {
    @JvmField var active: Boolean = false
    @JvmField var pendingYaw: Double = 0.0
    @JvmField var pendingPitch: Double = 0.0
}
