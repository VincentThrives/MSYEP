package com.vincent.msyep.modules.sow;

import com.vincent.msyep.modules.center.CenterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for the KP-MSYEP SOW save / fetch (upsert per center+program). */
class SowServiceTest {

    private SowSubmissionRepository repo;
    private CenterRepository centers;
    private SowService svc;

    @BeforeEach
    void setUp() {
        repo = mock(SowSubmissionRepository.class);
        centers = mock(CenterRepository.class);
        svc = new SowService(repo, centers, Optional.empty(), "");
    }

    @Test
    void saveCreatesNewSubmission() {
        when(repo.findByCenterIdAndProgramIndex("C1", 1)).thenReturn(Optional.empty());
        when(repo.save(any(SowSubmission.class))).thenAnswer(i -> i.getArgument(0));

        SowSubmission saved = svc.save("C1", 1, Map.of("guestName", "Dr. Rao"), Map.of());

        assertThat(saved.getCenterId()).isEqualTo("C1");
        assertThat(saved.getProgramIndex()).isEqualTo(1);
        assertThat(saved.getFields()).containsEntry("guestName", "Dr. Rao");
        verify(repo).save(any(SowSubmission.class));
    }

    @Test
    void saveUpdatesExistingSubmission() {
        SowSubmission existing = SowSubmission.builder().id("X").centerId("C1").programIndex(2).build();
        when(repo.findByCenterIdAndProgramIndex("C1", 2)).thenReturn(Optional.of(existing));
        when(repo.save(any(SowSubmission.class))).thenAnswer(i -> i.getArgument(0));

        SowSubmission saved = svc.save("C1", 2, Map.of("k", "v"), Map.of());

        assertThat(saved.getId()).isEqualTo("X"); // same doc, not a new one
        assertThat(saved.getFields()).containsEntry("k", "v");
    }

    @Test
    void getReturnsExisting() {
        SowSubmission s = SowSubmission.builder().centerId("C1").programIndex(3).build();
        when(repo.findByCenterIdAndProgramIndex("C1", 3)).thenReturn(Optional.of(s));
        assertThat(svc.get("C1", 3)).isSameAs(s);
    }

    @Test
    void getReturnsNullWhenAbsent() {
        when(repo.findByCenterIdAndProgramIndex("C1", 4)).thenReturn(Optional.empty());
        assertThat(svc.get("C1", 4)).isNull();
    }
}
