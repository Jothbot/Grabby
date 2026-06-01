package net.joth.grabby.physics

import net.joth.grabby.Grabby
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf

object GrabbyAssemblyHelper {

    private const val MAX_BLOCKS = 64

    fun gatherConnectedBlocks(level: Level, startPos: BlockPos): Set<BlockPos> {
        val collected = mutableSetOf(startPos)
        val queue = ArrayDeque<BlockPos>()
        queue.add(startPos)

        while (queue.isNotEmpty() && collected.size < MAX_BLOCKS) {
            val pos = queue.removeFirst()
            val state = level.getBlockState(pos)

            for (partner in findMultiBlockPartners(state, pos)) {
                if (!level.getBlockState(partner).isAir && collected.add(partner)) {
                    Grabby.LOGGER.info("Partner found: $partner (${level.getBlockState(partner).block}) for $pos (${state.block})")
                    queue.add(partner)
                }
            }

            val virtualLevel = VirtualLevelReader(level, pos)
            for (dir in Direction.entries) {
                val neighbourPos = pos.relative(dir)
                val neighbourState = level.getBlockState(neighbourPos)
                if (!neighbourState.isAir && !neighbourState.canSurvive(virtualLevel, neighbourPos)) {
                    if (collected.add(neighbourPos)) {
                        queue.add(neighbourPos)
                    }
                }
            }
        }

        Grabby.LOGGER.info("Total blocks gathered: ${collected.size} — $collected")
        return collected
    }

    private fun findMultiBlockPartners(state: BlockState, pos: BlockPos): List<BlockPos> {
        val partners = mutableListOf<BlockPos>()

        state.getOptionalValue(BlockStateProperties.DOUBLE_BLOCK_HALF).ifPresent { half ->
            partners.add(if (half == DoubleBlockHalf.LOWER) pos.above() else pos.below())
        }

        state.getOptionalValue(BlockStateProperties.BED_PART).ifPresent { part ->
            val facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING)
            partners.add(if (part == BedPart.FOOT) pos.relative(facing) else pos.relative(facing.opposite))
        }

        state.getOptionalValue(BlockStateProperties.CHEST_TYPE).ifPresent { type ->
            if (type != ChestType.SINGLE) {
                val facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                partners.add(pos.relative(if (type == ChestType.LEFT) facing.clockWise else facing.counterClockWise))
            }
        }

        return partners
    }

    private class VirtualLevelReader(
        private val delegate: Level,
        private val removedPos: BlockPos
    ) : LevelReader by delegate {
        override fun getBlockState(pos: BlockPos): BlockState =
            if (pos == removedPos) Blocks.AIR.defaultBlockState()
            else delegate.getBlockState(pos)
    }

}