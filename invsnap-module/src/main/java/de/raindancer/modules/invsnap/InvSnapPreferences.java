package de.raindancer.modules.invsnap;

import de.raindancer.core.social.desk.ServiceDesk;
import de.raindancer.core.social.desk.ServiceDesks;
import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.invsnap.command.InsuranceCommand;
import de.raindancer.modules.invsnap.util.PermissionNodes;

import java.util.function.Supplier;

/** What /preferences offers for insurance: the same death insurance switch {@code /insurance on|off} flips. */
public final class InvSnapPreferences {

    private InvSnapPreferences() {
    }

    /** Offered to whoever may insure, where death insurance exists and is not arranged at an agent only. */
    public static Preference insurance(Supplier<InvSnapServices> services) {
        return Preference.toggle("invsnap:insurance", "Insurance", "Death insurance",
                        "If you die you pay a premium and keep your inventory.",
                        player -> services.get().insurance().isInsured(player.getUniqueId()),
                        (player, on) -> {
                            if (!ServiceDesks.refuseRemote(player, ServiceDesk.INSURANCE)) {
                                InsuranceCommand.setDeath(services.get(), player, on);
                            }
                        })
                .shownTo(player -> player.hasPermission(PermissionNodes.INSURE)
                        && services.get().insurance().enabled()
                        && !ServiceDesks.inPersonOnly(ServiceDesk.INSURANCE));
    }
}
