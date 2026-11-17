package de.raindancer.modules.invsnap;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.invsnap.service.InsuranceService;
import de.raindancer.modules.invsnap.util.PermissionNodes;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvSnapPreferencesTest {

    private final UUID id = UUID.randomUUID();
    private final Player player = mock(Player.class);
    private final Messages messages = mock(Messages.class);
    private final InsuranceService insurance = mock(InsuranceService.class);
    private final InvSnapServices services = mock(InvSnapServices.class);
    private Preference preference;

    @BeforeEach
    void wire() {
        when(player.getUniqueId()).thenReturn(id);
        when(services.insurance()).thenReturn(insurance);
        when(services.messages()).thenReturn(messages);
        when(insurance.enabled()).thenReturn(true);
        preference = InvSnapPreferences.insurance(() -> services);
    }

    @Test
    void isTheDeathInsuranceSwitch() {
        when(insurance.isInsured(id)).thenReturn(true);
        assertThat(preference.id()).isEqualTo("invsnap:insurance");
        assertThat(preference.group()).isEqualTo("Insurance");
        assertThat(preference.state(player)).isEqualTo("On");
    }

    @Test
    void switchingOffGoesThroughTheServiceAndSaysSo() {
        when(insurance.setInsured(id, false)).thenReturn(true);
        preference.choose().accept(player, 1);
        verify(insurance).setInsured(id, false);
        verify(messages).send(player, "invsnap.insurance.now-off");
    }

    @Test
    void aChoiceThatCouldNotBeSavedIsSaidSo() {
        when(insurance.setInsured(id, true)).thenReturn(false);
        preference.choose().accept(player, 0);
        verify(messages).send(player, "invsnap.insurance.not-saved");
        verify(messages, never()).send(player, "invsnap.insurance.now-off");
    }

    @Test
    void offeredOnlyToWhoMayInsureWhereDeathInsuranceExists() {
        when(player.hasPermission(PermissionNodes.INSURE)).thenReturn(true);
        assertThat(preference.shown().test(player)).isTrue();
        when(insurance.enabled()).thenReturn(false);
        assertThat(preference.shown().test(player)).isFalse();
        when(insurance.enabled()).thenReturn(true);
        when(player.hasPermission(PermissionNodes.INSURE)).thenReturn(false);
        assertThat(preference.shown().test(player)).isFalse();
    }
}
