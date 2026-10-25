package de.raindancer.modules.invsnap.screen;

import de.raindancer.core.data.nbt.ItemText;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.modules.invsnap.InvSnapServices;
import de.raindancer.modules.invsnap.model.Snapshot;
import de.raindancer.modules.invsnap.service.SnapshotService;
import de.raindancer.modules.invsnap.util.PermissionNodes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * One snapshot's whole inventory, drawn with the real items rather than icons standing in for them
 * — a durability bar, an enchantment, a written book's own pages are exactly what they would be if
 * this were restored. No slot can be taken or moved, so looking closely at what somebody had never
 * risks moving it; staff who may restore can click one item to give just that one back (after an
 * "are you sure?"), on top of what the player carries now.
 *
 * <h2>Compare mode</h2>
 * Opened either plain ({@code live} is {@code null}) or against a snapshot of what the target is
 * carrying right now, slot for slot in restore order. A slot whose encoded text does not match gets
 * one more line of lore saying so and what is there instead — the snapshot item itself is never
 * altered beyond that lore, so what this shows is always exactly what a restore would put there.
 */
public final class SnapshotDetailScreen extends Menu implements IInvSnapScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /** Main (36) then armour (4, boots→helmet) then off hand (1) — one flat list, in restore order. */
    private final InvSnapServices services;
    private final Snapshot snapshot;
    private final UUID targetId;
    private final List<String> slots;
    private final List<String> liveSlots;
    private final String targetName;
    private final Instant takenAt;

    public SnapshotDetailScreen(InvSnapServices services, Player viewer, Menu parent,
                                Snapshot snapshot, Snapshot live, UUID targetId, String targetName) {
        super(viewer, services.brand(), parent);
        this.services = services;
        this.snapshot = snapshot;
        this.targetId = targetId;
        this.slots = flatten(snapshot);
        this.liveSlots = live == null ? null : flatten(live);
        this.targetName = targetName == null ? "them" : targetName;
        this.takenAt = snapshot.takenAt();
    }

    private static List<String> flatten(Snapshot snapshot) {
        List<String> flat = new ArrayList<>(41);
        flat.addAll(snapshot.mainInventory());
        flat.addAll(snapshot.armor());
        flat.add(snapshot.offHand());
        return flat;
    }

    @Override
    protected Component title() {
        String mode = liveSlots == null ? "Snapshot" : "Compare";
        return MINI.deserialize("<dark_gray>" + mode + " — " + targetName + " — " + STAMP.format(takenAt));
    }

    @Override
    public String breadcrumb() {
        return "This snapshot";
    }

    @Override
    protected void render() {
        for (int index = 0; index < 36; index++) {
            slot(index, index, mainLabel(index));
        }
        // Boots→helmet is getArmorContents()'s own order, indices 36..39 here; laid out top to
        // bottom the way a player expects to see their own equipment, then off hand set apart.
        slot(38, 39, "Helmet");
        slot(39, 38, "Chestplate");
        slot(40, 37, "Leggings");
        slot(41, 36, "Boots");
        slot(43, 40, "Off hand");
    }

    private void slot(int at, int index, String label) {
        if (mayGiveBack() && ItemText.decode(slots.get(index)) != null) {
            set(at, iconFor(index, label), event -> giveBack(index));
        } else {
            set(at, iconFor(index, label));
        }
    }

    private boolean mayGiveBack() {
        return targetId != null && viewer.hasPermission(PermissionNodes.RESTORE);
    }

    private void giveBack(int index) {
        ItemStack item = ItemText.decode(slots.get(index));
        if (item == null) {
            return;
        }
        String what = describe(item);
        new ConfirmScreen(services, viewer, this,
                "<yellow>Give " + MINI.escapeTags(what) + " back to " + targetName + "?",
                List.of("<gray>Only this item, from the snapshot of <white>" + STAMP.format(takenAt) + "</white>.",
                        "<gray>It is added to what " + targetName + " carries now;",
                        "<gray>nothing they have is replaced."),
                () -> {
                    Player target = Bukkit.getPlayer(targetId);
                    if (target == null) {
                        services.messages().send(viewer, "invsnap.restore.offline", "player", targetName);
                        return;
                    }
                    SnapshotService.ItemRestore done = services.snapshots().restoreItem(target, snapshot, index);
                    String key = switch (done) {
                        case GIVEN -> "invsnap.restore.item-given";
                        case NO_ROOM -> "invsnap.restore.item-no-room";
                        case EMPTY_SLOT -> "invsnap.restore.item-gone";
                    };
                    services.messages().send(viewer, key, "item", what, "player", target.getName());
                }).open();
    }

    private static String mainLabel(int index) {
        return index < 9 ? "Hotbar slot " + (index + 1) : "Inventory slot " + (index - 8);
    }

    private ItemStack iconFor(int index, String label) {
        String encoded = slots.get(index);
        ItemStack decoded = ItemText.decode(encoded);
        boolean empty = decoded == null;
        ItemStack shown = empty ? Icons.filler(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                : decoded.clone();

        ItemMeta meta = shown.getItemMeta();
        if (meta == null) {
            return shown;
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(Icons.loreLine("<dark_gray>" + label));
        if (liveSlots != null) {
            lore.add(Icons.loreLine(liveNoteFor(index, encoded)));
        }
        if (!empty && mayGiveBack()) {
            lore.add(Icons.loreLine("<yellow>Click to give only this back to " + targetName));
        }
        meta.lore(lore);
        if (empty) {
            meta.displayName(Icons.name("<gray>" + label + " — empty"));
        }
        shown.setItemMeta(meta);
        return shown;
    }

    private String liveNoteFor(int index, String encoded) {
        String liveEncoded = liveSlots.get(index);
        if (Objects.equals(nullToEmpty(encoded), nullToEmpty(liveEncoded))) {
            return "<green>Matches what " + targetName + " carries now";
        }
        ItemStack live = ItemText.decode(liveEncoded);
        String description = live == null ? "empty" : describe(live);
        return "<red>Different now — " + targetName + " carries: " + description;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String describe(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        String name = meta != null && meta.hasDisplayName()
                ? PlainTextComponentSerializer.plainText().serialize(meta.displayName())
                : item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return item.getAmount() + "x " + name;
    }

    @Override
    protected List<String> helpLines() {
        String click = mayGiveBack() ? "Click an item to give just that one back." : "Nothing here can be changed.";
        if (liveSlots == null) {
            return List.of("Exactly what " + targetName + " was carrying",
                    "at this moment. " + click);
        }
        return List.of("Every slot, compared to what " + targetName + " is",
                "carrying right now. Green lore means unchanged;",
                "red lore means different, and says what is there now.", click);
    }

    @Override
    public String describe() {
        return liveSlots == null ? "one snapshot's full inventory, with the real items, read-only"
                : "one snapshot's inventory compared slot by slot to the live one";
    }
}
