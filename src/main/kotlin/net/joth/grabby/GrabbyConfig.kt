package net.joth.grabby

import net.neoforged.neoforge.common.ModConfigSpec

object GrabbyConfig {
    private val builder = ModConfigSpec.Builder()

    val springConstant: ModConfigSpec.DoubleValue
    val dampingConstant: ModConfigSpec.DoubleValue
    val angularDamping: ModConfigSpec.DoubleValue
    val maxForce: ModConfigSpec.DoubleValue
    val maxDisassemblySize: ModConfigSpec.IntValue
    val disassemblyPositionTolerance: ModConfigSpec.DoubleValue
    val disassemblyRotationTolerance: ModConfigSpec.DoubleValue
    val alignmentMaxTicks: ModConfigSpec.IntValue

    val SPEC: ModConfigSpec

    init {
        builder.comment("Physics constants for Grabby's grab mechanic.").push("physics")

        springConstant = builder
            .comment("Constraint stiffness — how aggressively the grabbed block tracks your hand.")
            .defineInRange("springConstant", 240.0, 0.01, 10000.0)

        dampingConstant = builder
            .comment("Constraint damping — how quickly oscillation settles.")
            .defineInRange("dampingConstant", 30.0, 0.0, 10000.0)

        angularDamping = builder
            .comment("Angular damping — how quickly the block stops spinning.")
            .defineInRange("angularDamping", 4.5, 0.0, 1000.0)

        maxForce = builder
            .comment("Maximum force the constraint can apply. Lower values make heavy sub-levels feel heavier.")
            .defineInRange("maxForce", 150.0, 0.0, 100000.0)

        builder.comment("Disassembly settings").push("disassembly")

        maxDisassemblySize = builder
            .comment("Maximum number of blocks in a sub-level that can be disassembled.")
            .defineInRange("maxDisassemblySize", 6, 1, 64)

        disassemblyPositionTolerance = builder
            .comment("How close to the block grid the sub-level must be before it can be disassembled (in blocks).")
            .defineInRange("disassemblyPositionTolerance", 0.3, 0.0, 1.0)

        disassemblyRotationTolerance = builder
            .comment("How close to a 90-degree rotation the sub-level must be before it can be disassembled (in degrees).")
            .defineInRange("disassemblyRotationTolerance", 15.0, 0.0, 45.0)

        alignmentMaxTicks = builder
            .comment("How many ticks the mod will try to align a sub-level before giving up (20 ticks = 1 second).")
            .defineInRange("alignmentMaxTicks", 100, 20, 600)

        builder.pop()
        builder.pop()
        SPEC = builder.build()
    }
}