package net.vulkanmod.compat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatPolicyManagerTest {
    @Test
    void aModWeActuallyCheckedReadsVerifiedOnTheVersionWeChecked() {
        assertTrue(CompatPolicyManager.isVerifiedVersion("distanthorizons", "2.1.2-a"));
        assertTrue(CompatPolicyManager.isVerifiedVersion("create", "6.0.10"));
        assertTrue(CompatPolicyManager.isVerifiedVersion("jei", "19.44.0.399"));
        assertTrue(CompatPolicyManager.isVerifiedVersion("aeronautics", "1.2.1"));
        assertTrue(CompatPolicyManager.isVerifiedVersion("CREATE", "6.0.10"),
                "mod ids are matched case-insensitively");
    }

    @Test
    void aModNobodyEverCheckedIsNeverClaimedAsVerified() {
        assertFalse(CompatPolicyManager.isVerifiedVersion("journeymap", "6.0.0"),
                "journeymap is absent from verified_mod_versions.properties");
        assertFalse(CompatPolicyManager.isVerifiedVersion("some_mod_nobody_has_heard_of", "1.0.0"));
    }

    @Test
    void adifferentVersionOfACheckedModIsStillUnverified() {
        assertFalse(CompatPolicyManager.isVerifiedVersion("create", "0.5.1.f"),
                "6.0 is what runs on this Minecraft version, 0.5 was never checked here");
        assertFalse(CompatPolicyManager.isVerifiedVersion("distanthorizons", "3.0.0"));
    }

    @Test
    void aVersionWeCouldNotReadIsNotAPassingGrade() {
        assertFalse(CompatPolicyManager.isVerifiedVersion("create", "UNKNOWN"));
        assertFalse(CompatPolicyManager.isVerifiedVersion("create", null));
        assertFalse(CompatPolicyManager.isVerifiedVersion(null, "0.5.1"));
    }

    @Test
    void aModWeTestedAndFoundBrokenReadsUnsupportedAtAnyVersion() {
        assertTrue(CompatPolicyManager.isUnsupported("journeymap"));
        assertTrue(CompatPolicyManager.isUnsupported("resourcify"));
        assertTrue(CompatPolicyManager.isUnsupported("JourneyMap"), "ids are case-insensitive here too");
        assertTrue(CompatPolicyManager.isUnsupported("distanthorizons"),
                "an OpenGL renderer stays unsupported without being listed twice");
    }

    @Test
    void aModWeHaveNoVerdictOnIsNeitherSupportedNorUnsupported() {
        assertFalse(CompatPolicyManager.isUnsupported("appleskin"));
        assertFalse(CompatPolicyManager.isVerifiedVersion("appleskin", "2.5.1"));
        assertFalse(CompatPolicyManager.isUnsupported(null));
    }

    @Test
    void everyModWeHaveAVerdictOnActuallyShowsUpOnThePage() {
        for (String modId : CompatMods.UNSUPPORTED_MOD_IDS) {
            assertTrue(CompatMods.contains(CompatMods.REPORT_MOD_IDS, modId),
                    modId + " has a verdict nobody will ever read");
        }
        for (String modId : List.of("create", "jei", "aeronautics")) {
            assertTrue(CompatMods.contains(CompatMods.REPORT_MOD_IDS, modId),
                    modId + " was tested but the page never walks it");
        }
    }

    @Test
    void aModCannotBeCalledSupportedAndUnsupportedAtOnce() {
        for (String modId : CompatMods.UNSUPPORTED_MOD_IDS) {
            assertFalse(CompatPolicyManager.isVerifiedVersion(modId, "1"),
                    modId + " is listed as broken and as verified");
        }
    }
}
