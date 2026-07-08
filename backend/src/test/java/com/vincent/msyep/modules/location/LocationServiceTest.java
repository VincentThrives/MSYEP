package com.vincent.msyep.modules.location;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Unit tests for the location master service (add / rename / delete + duplicate guards). */
class LocationServiceTest {

    private LocationRepository repo;
    private MongoTemplate mongo;
    private LocationService svc;

    @BeforeEach
    void setUp() {
        repo = mock(LocationRepository.class);
        mongo = mock(MongoTemplate.class);
        svc = new LocationService(repo, mongo);
    }

    private void stubDistricts(String... ds) {
        when(mongo.findDistinct(any(Query.class), eq("district"), eq(Location.class), eq(String.class)))
                .thenReturn(List.of(ds));
    }

    private void stubTaluks(String... ts) {
        when(mongo.findDistinct(any(Query.class), eq("taluk"), eq(Location.class), eq(String.class)))
                .thenReturn(List.of(ts));
    }

    @Test
    void addDistrictRejectsBlank() {
        assertThatThrownBy(() -> svc.addDistrict("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addDistrictRejectsDuplicateCaseInsensitive() {
        stubDistricts("Bagalkot");
        assertThatThrownBy(() -> svc.addDistrict("bagalkot")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addDistrictSavesNew() {
        stubDistricts("Bagalkot");
        svc.addDistrict("Mysuru");
        verify(repo).save(any(Location.class));
    }

    @Test
    void addTalukRejectsMissingFields() {
        assertThatThrownBy(() -> svc.addTaluk("", "Hunsur")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> svc.addTaluk("Mysuru", "")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addTalukRejectsDuplicate() {
        stubTaluks("Hunsur");
        assertThatThrownBy(() -> svc.addTaluk("Mysuru", "hunsur")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addTalukSavesNew() {
        stubTaluks("Hunsur");
        svc.addTaluk("Mysuru", "Nanjangud");
        verify(repo).save(any(Location.class));
    }

    @Test
    void renameDistrictRejectsExistingTarget() {
        stubDistricts("Bagalkot", "Mysuru");
        assertThatThrownBy(() -> svc.renameDistrict("Bagalkot", "Mysuru"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renameDistrictUpdatesAllRows() {
        stubDistricts("Bagalkot");
        svc.renameDistrict("Bagalkot", "Bagalkote");
        verify(mongo).updateMulti(any(Query.class), any(Update.class), eq(Location.class));
    }

    @Test
    void deleteDistrictRemovesRows() {
        svc.deleteDistrict("Bagalkot");
        verify(mongo).remove(any(Query.class), eq(Location.class));
    }

    @Test
    void deleteTalukRejectsMissing() {
        assertThatThrownBy(() -> svc.deleteTaluk("Mysuru", "")).isInstanceOf(IllegalArgumentException.class);
    }
}
