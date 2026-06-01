package net.joth.grabby.physics

import java.util.UUID

object GrabbyState {
    // map of UUIDs of players and what sublevel they're holding
    private val heldSubLevels = mutableMapOf<UUID, GrabData>()
    private val aligningSubLevels = mutableMapOf<UUID, AlignmentData>()

    // adding a pair of UUID - Sublevel to that map
    fun setHeld(playerUUID: UUID, data: GrabData) {
        heldSubLevels[playerUUID] = data
    }

    // returns what (if anything) the player is holding
    fun getHeld(playerUUID: UUID): GrabData? = heldSubLevels[playerUUID]

    // removes an entry
    fun clearHeld(playerUUID: UUID) {
        heldSubLevels.remove(playerUUID)?.constraintHandle?.remove()
    }

    // makes a new map of the actual map so that you're not interfacing with the map directly
    fun getAllHeld(): Map<UUID, GrabData> = heldSubLevels.toMap()

    fun setAligning(playerUUID: UUID, data: AlignmentData) {
        aligningSubLevels[playerUUID] = data
    }

    fun clearAligning(playerUUID: UUID) {
        aligningSubLevels.remove(playerUUID)?.alignmentConstraint?.remove()
    }

    fun getAllAligning(): Map<UUID, AlignmentData> = aligningSubLevels.toMap()
}