package de.raindancer.modules.invsnap.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("a snapshot, and what it normalises on construction")
class SnapshotTest {

    @Test
    @DisplayName("a null name falls back to the id, never a blank display")
    void nullNameFallsBackToId() {
        UUID id = UUID.randomUUID();
        Snapshot snapshot = new Snapshot(id, null, Instant.now(), List.of(), List.of(), null);

        assertThat(snapshot.playerName()).isEqualTo(id.toString());
    }

    @Test
    @DisplayName("null lists become empty, not an exception, and null off-hand becomes the empty-slot marker")
    void nullsBecomeEmptyRatherThanThrowing() {
        assertThatCode(() -> new Snapshot(UUID.randomUUID(), "Someone", Instant.now(), null, null, null))
                .doesNotThrowAnyException();

        Snapshot snapshot = new Snapshot(UUID.randomUUID(), "Someone", Instant.now(), null, null, null);
        assertThat(snapshot.mainInventory()).isEmpty();
        assertThat(snapshot.armor()).isEmpty();
        assertThat(snapshot.offHand()).isEqualTo(Snapshot.EMPTY_SLOT);
    }

    @Test
    @DisplayName("a missing player id is refused rather than silently accepted")
    void missingPlayerIdIsRefused() {
        assertThatCode(() -> new Snapshot(null, "Someone", Instant.now(), List.of(), List.of(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("one slot by its place in restore order: main 0-35, armour 36-39 (boots first), off hand 40")
    void slotInRestoreOrder() {
        List<String> main = new java.util.ArrayList<>(java.util.Collections.nCopies(36, Snapshot.EMPTY_SLOT));
        main.set(3, "sword");
        Snapshot snapshot = new Snapshot(UUID.randomUUID(), "Someone", Instant.now(), main,
                List.of("boots", "", "", "helmet"), "shield");

        assertThat(snapshot.slot(3)).isEqualTo("sword");
        assertThat(snapshot.slot(0)).isEqualTo(Snapshot.EMPTY_SLOT);
        assertThat(snapshot.slot(36)).isEqualTo("boots");
        assertThat(snapshot.slot(39)).isEqualTo("helmet");
        assertThat(snapshot.slot(40)).isEqualTo("shield");
    }

    @Test
    @DisplayName("a slot a short or old snapshot never recorded, or one past the end, is empty rather than an error")
    void missingSlotsAreEmpty() {
        Snapshot snapshot = new Snapshot(UUID.randomUUID(), "Someone", Instant.now(), List.of("a"), List.of(), null);

        assertThat(snapshot.slot(5)).isEqualTo(Snapshot.EMPTY_SLOT);
        assertThat(snapshot.slot(37)).isEqualTo(Snapshot.EMPTY_SLOT);
        assertThat(snapshot.slot(-1)).isEqualTo(Snapshot.EMPTY_SLOT);
        assertThat(snapshot.slot(Snapshot.SLOTS)).isEqualTo(Snapshot.EMPTY_SLOT);
    }

    @Test
    @DisplayName("a missing timestamp is refused too")
    void missingTimestampIsRefused() {
        assertThatCode(() -> new Snapshot(UUID.randomUUID(), "Someone", null, List.of(), List.of(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
