package net.vulkanmod.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatPolicyManagerTest {
    @Test
    void aModWeActuallyCheckedReadsVerifiedOnTheVersionWeChecked() {
        assertTrue(CompatPolicyManager.isVerifiedVersion("distanthorizons", "2.1.2-a"));
        assertTrue(CompatPolicyManager.isVerifiedVersion("create", "0.5.1.f"));
        assertTrue(CompatPolicyManager.isVerifiedVersion("CREATE", "0.5.1.f"),
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
        assertFalse(CompatPolicyManager.isVerifiedVersion("create", "0.6.0"),
                "0.5 was checked, 0.6 was not");
        assertFalse(CompatPolicyManager.isVerifiedVersion("distanthorizons", "3.0.0"));
    }

    @Test
    void aVersionWeCouldNotReadIsNotAPassingGrade() {
        assertFalse(CompatPolicyManager.isVerifiedVersion("create", "UNKNOWN"));
        assertFalse(CompatPolicyManager.isVerifiedVersion("create", null));
        assertFalse(CompatPolicyManager.isVerifiedVersion(null, "0.5.1"));
    }

    @Test
    void everyVerifiedEntryNamesAModTheReportActuallyWalks() {
        for (String modId : CompatMods.REPORT_MOD_IDS) {
            assertFalse(modId.isBlank());
        }
        assertTrue(CompatMods.contains(CompatMods.RENDERER_GL_MOD_IDS, "distanthorizons"),
                "a renderer mod must stay unsupported whatever version is installed");
    }
}
