package net.joth.grabby.client

import java.util.UUID

object GrabbyHoldingPlayers {
    @JvmField val players: MutableSet<UUID> = HashSet()
}
