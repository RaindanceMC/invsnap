package de.raindancer.modules.invsnap;

import de.raindancer.core.ui.help.HelpLint;
import de.raindancer.modules.api.ModuleCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GuideTest {

    @Test
    @DisplayName("the guide pages read, quote real settings and explain every command")
    void theGuideIsComplete() {
        assertThat(HelpLint.of(Path.of("src/main/resources/rains-help"))
                .module("invsnap")
                .settings(InvSnapSettings.class, InvSnapSettings.DEFAULTS)
                .foreignSettings("core")
                .otherTopics("basics", "guide")
                .commands(new InvSnapModule().commands().stream().map(ModuleCommand::name).toList())
                .problems()).isEmpty();
    }
}
