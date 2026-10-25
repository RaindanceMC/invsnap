package de.raindancer.modules.invsnap.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One player's inventory, at one moment.
 *
 * <h2>Why every list keeps its empty slots</h2>
 * Each string is a slot, not an item — an empty slot is recorded as {@link #EMPTY_SLOT} rather than
 * being left out. Dropping empty slots would compact the list, and restoring a compacted list back
 * into a real inventory shifts every item after the first gap into the wrong slot: a sword typed to
 * slot 4 lands in slot 0 instead, in the hotbar of whoever it was fixed for. Position is the entire
 * point of a snapshot, so it is never optimised away.
 *
 * <p>{@link #mainInventory()} is {@code PlayerInventory#getContents()} (36 slots), {@link #armor()}
 * is {@code getArmorContents()} (4 slots, boots through helmet), and {@link #offHand()} is the one
 * remaining slot a main-hand-and-armour snapshot would otherwise miss.
 */
public record Snapshot(UUID playerId, String playerName, Instant takenAt,
                       List<String> mainInventory, List<String> armor, String offHand) {

    /** What an empty slot is recorded as — never a valid encoded item, so it cannot be confused for one. */
    public static final String EMPTY_SLOT = "";

    /** Main (36), armour (4, boots first), off hand (1): every slot a snapshot covers, in restore order. */
    public static final int SLOTS = 41;

    public Snapshot {
        Objects.requireNonNull(playerId, "a snapshot needs whose inventory it is");
        Objects.requireNonNull(takenAt, "a snapshot needs when it was taken");
        playerName = playerName == null || playerName.isBlank() ? playerId.toString() : playerName;
        mainInventory = mainInventory == null ? List.of() : List.copyOf(mainInventory);
        armor = armor == null ? List.of() : List.copyOf(armor);
        offHand = offHand == null ? EMPTY_SLOT : offHand;
    }

    /** One slot by its place in restore order (see {@link #SLOTS}); anything this snapshot never recorded is empty. */
    public String slot(int index) {
        if (index >= 0 && index < 36) {
            return index < mainInventory.size() ? mainInventory.get(index) : EMPTY_SLOT;
        }
        if (index >= 36 && index < 40) {
            return index - 36 < armor.size() ? armor.get(index - 36) : EMPTY_SLOT;
        }
        return index == 40 ? offHand : EMPTY_SLOT;
    }
}
