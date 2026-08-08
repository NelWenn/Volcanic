package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OverviewModelTest {

    @Test
    void rowsKeepTheOrderTheyWereAddedIn() {
        List<OverviewModel.Row> rows = new OverviewModel()
                .add("vulkanmod.overview.gpu", Optional.of("Apple M1 Pro"))
                .add("vulkanmod.overview.resolution", Optional.of("2560x1600 @ 120Hz"))
                .add("vulkanmod.overview.vsync", Optional.of("On"))
                .rows();

        assertEquals(List.of(
                new OverviewModel.Row("vulkanmod.overview.gpu", "Apple M1 Pro"),
                new OverviewModel.Row("vulkanmod.overview.resolution", "2560x1600 @ 120Hz"),
                new OverviewModel.Row("vulkanmod.overview.vsync", "On")), rows);
    }

    @Test
    void anUnavailableValueIsOmittedInsteadOfLeavingABlankRow() {
        List<OverviewModel.Row> rows = new OverviewModel()
                .add("vulkanmod.overview.gpu", Optional.of("Apple M1 Pro"))
                .add("vulkanmod.overview.shader_pack", Optional.empty())
                .add("vulkanmod.overview.vsync", Optional.of("On"))
                .rows();

        assertEquals(List.of("vulkanmod.overview.gpu", "vulkanmod.overview.vsync"),
                rows.stream().map(OverviewModel.Row::labelKey).toList());
    }

    @Test
    void anEmptyModelHasNoRows() {
        assertEquals(List.of(), new OverviewModel().rows());
    }

    @Test
    void aNullLabelKeyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new OverviewModel().add(null, Optional.of("On")));
    }

    @Test
    void aNullLabelKeyIsRejectedEvenWhenTheValueIsUnavailable() {
        assertThrows(IllegalArgumentException.class,
                () -> new OverviewModel().add(null, Optional.empty()));
    }

    @Test
    void aNullValueIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new OverviewModel().add("vulkanmod.overview.vsync", null));
    }

    @Test
    void aBlankValueIsRejectedRatherThanStoredAsABlankRow() {
        assertThrows(IllegalArgumentException.class,
                () -> new OverviewModel().add("vulkanmod.overview.vsync", Optional.of("   ")));
    }

    @Test
    void aRowRejectsNullPartsDirectly() {
        assertThrows(IllegalArgumentException.class,
                () -> new OverviewModel.Row("vulkanmod.overview.vsync", null));
        assertThrows(IllegalArgumentException.class,
                () -> new OverviewModel.Row(null, "On"));
    }

    @Test
    void aTakenRowListIsNotChangedByLaterAdditions() {
        OverviewModel model = new OverviewModel().add("vulkanmod.overview.gpu", Optional.of("Apple M1 Pro"));
        List<OverviewModel.Row> taken = model.rows();

        model.add("vulkanmod.overview.vsync", Optional.of("On"));

        assertEquals(1, taken.size());
        assertEquals(2, model.rows().size());
    }
}
