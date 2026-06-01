package net.joth.grabby.physics

import dev.ryanhcode.sable.api.physics.constraint.free.FreeConstraintHandle
import dev.ryanhcode.sable.sublevel.ServerSubLevel
import org.joml.Quaterniond
import org.joml.Vector3d

class AlignmentData(
    val subLevel: ServerSubLevel,
    val subLevelAccess: dev.ryanhcode.sable.companion.SubLevelAccess,
    val alignmentConstraint: FreeConstraintHandle,
    val disassemblyOrientation: Quaterniond,
    val disassemblyAngle: Int,
    val blocks: List<net.minecraft.core.BlockPos>,
    var aligningTicks: Int = 0,
    var readyTicks: Int = 0
)