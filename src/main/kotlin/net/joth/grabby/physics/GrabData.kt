package net.joth.grabby.physics

import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle
import dev.ryanhcode.sable.companion.SubLevelAccess
import dev.ryanhcode.sable.sublevel.ServerSubLevel
import net.minecraft.world.phys.Vec3

// Okay so this whole new approach needs so explaining. Originally I just grabbed the sublevel, reset its velocity
// and applied a new one to move it to where the player is looking. This *worked*, but it felt like the player was
// using a gravity gun. What I want is to replicate the Handlebar motion. So instead we do this.
// First, the old method just grabbed the *whole* sublevel. Here we instead grab it by a block in it, and for that
// we need to know where that block is. That's basically what this GrabData class is for. It gives us not just the sublevel,
// but also what *block* the player is looking at
// TODO: in a future rewrite, try making all this logic serverside. The server decides what the player is looking at, not the client
class GrabData(
    val subLevel: ServerSubLevel,
    val subLevelAccess: SubLevelAccess,
    val grabPointLocal: Vec3,  // position of the grabbed block in plot space
    val grabDistance: Double, // how far from the player's eyes when grabbed
    var constraintHandle: PhysicsConstraintHandle? = null
)