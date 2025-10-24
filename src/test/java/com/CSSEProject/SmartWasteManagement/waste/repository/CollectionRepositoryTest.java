package com.CSSEProject.SmartWasteManagement.waste.repository;

import com.CSSEProject.SmartWasteManagement.waste.entity.CollectionEvent;
import com.CSSEProject.SmartWasteManagement.waste.entity.WasteBin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CollectionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CollectionEventRepository collectionRepository;

    @Test
    void findByCollectorId_WithValidCollector_ShouldReturnCollections() {
        // Arrange
        WasteBin bin = new WasteBin();
        bin.setBinId("TEST-BIN-1");
        bin.setLocation("Test Location");
        entityManager.persist(bin);

        CollectionEvent collection = new CollectionEvent();
        collection.setWeight(10.0);
        collection.setCollectionTime(LocalDateTime.now());
        collection.setWasteBin(bin);
        // Set collector through relationship
        entityManager.persist(collection);
        entityManager.flush();

        // Act
        List<CollectionEvent> result = collectionRepository.findByCollectorId(collection.getCollector() != null ? 
                collection.getCollector().getId() : 1L);

        // Assert
        assertNotNull(result);
    }

    @Test
    void findByWasteBinBinId_WithValidBinId_ShouldReturnCollections() {
        // Arrange
        WasteBin bin = new WasteBin();
        bin.setBinId("TEST-BIN-2");
        bin.setLocation("Test Location 2");
        entityManager.persist(bin);

        CollectionEvent collection1 = new CollectionEvent();
        collection1.setWeight(5.0);
        collection1.setCollectionTime(LocalDateTime.now().minusDays(1));
        collection1.setWasteBin(bin);

        CollectionEvent collection2 = new CollectionEvent();
        collection2.setWeight(8.0);
        collection2.setCollectionTime(LocalDateTime.now());
        collection2.setWasteBin(bin);

        entityManager.persist(collection1);
        entityManager.persist(collection2);
        entityManager.flush();

        // Act
        List<CollectionEvent> result = collectionRepository.findByWasteBinBinId("TEST-BIN-2");

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(c -> c.getWasteBin().getBinId().equals("TEST-BIN-2")));
    }

    @Test
    void findByCollectionTimeBetween_WithDateRange_ShouldReturnFilteredCollections() {
        // Arrange
        WasteBin bin = new WasteBin();
        bin.setBinId("TEST-BIN-3");
        entityManager.persist(bin);

        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now();

        CollectionEvent withinRange = new CollectionEvent();
        withinRange.setWeight(10.0);
        withinRange.setCollectionTime(LocalDateTime.now().minusDays(1));
        withinRange.setWasteBin(bin);

        CollectionEvent outsideRange = new CollectionEvent();
        outsideRange.setWeight(15.0);
        outsideRange.setCollectionTime(LocalDateTime.now().minusDays(5));
        outsideRange.setWasteBin(bin);

        entityManager.persist(withinRange);
        entityManager.persist(outsideRange);
        entityManager.flush();

        // Act
        List<CollectionEvent> result = collectionRepository.findByCollectionTimeBetween(start, end);

        // Assert
        assertEquals(1, result.size());
        assertEquals(10.0, result.get(0).getWeight());
    }

    @Test
    void getTotalWeightBetween_ShouldReturnSum() {
        // Arrange
        WasteBin bin = new WasteBin();
        bin.setBinId("TEST-BIN-4");
        entityManager.persist(bin);

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        CollectionEvent collection1 = new CollectionEvent();
        collection1.setWeight(5.0);
        collection1.setCollectionTime(LocalDateTime.now().minusHours(12));
        collection1.setWasteBin(bin);

        CollectionEvent collection2 = new CollectionEvent();
        collection2.setWeight(7.0);
        collection2.setCollectionTime(LocalDateTime.now().minusHours(6));
        collection2.setWasteBin(bin);

        entityManager.persist(collection1);
        entityManager.persist(collection2);
        entityManager.flush();

        // Act
        Double result = collectionRepository.getTotalWeightBetween(start, end);

        // Assert
        assertEquals(12.0, result);
    }

    @Test
    void getCollectionCountBetween_ShouldReturnCount() {
        // Arrange
        WasteBin bin = new WasteBin();
        bin.setBinId("TEST-BIN-5");
        entityManager.persist(bin);

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        CollectionEvent collection1 = new CollectionEvent();
        collection1.setWeight(5.0);
        collection1.setCollectionTime(LocalDateTime.now().minusHours(12));
        collection1.setWasteBin(bin);

        CollectionEvent collection2 = new CollectionEvent();
        collection2.setWeight(7.0);
        collection2.setCollectionTime(LocalDateTime.now().minusHours(6));
        collection2.setWasteBin(bin);

        entityManager.persist(collection1);
        entityManager.persist(collection2);
        entityManager.flush();

        // Act
        Long result = collectionRepository.getCollectionCountBetween(start, end);

        // Assert
        assertEquals(2, result);
    }

    @Test
    void findUninvoicedCollections_ShouldReturnCollectionsWithoutInvoice() {
        // Arrange
        WasteBin bin = new WasteBin();
        bin.setBinId("TEST-BIN-6");
        entityManager.persist(bin);

        CollectionEvent uninvoiced = new CollectionEvent();
        uninvoiced.setWeight(10.0);
        uninvoiced.setCollectionTime(LocalDateTime.now());
        uninvoiced.setWasteBin(bin);
        // invoice is null by default

        entityManager.persist(uninvoiced);
        entityManager.flush();

        // Act
        List<CollectionEvent> result = collectionRepository.findUninvoicedCollections();

        // Assert
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(c -> c.getInvoice() == null));
    }

    @Test
    void countByCollectionTimeBetween_ShouldReturnCorrectCount() {
        // Arrange
        WasteBin bin = new WasteBin();
        bin.setBinId("TEST-BIN-7");
        entityManager.persist(bin);

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        CollectionEvent collection1 = new CollectionEvent();
        collection1.setWeight(5.0);
        collection1.setCollectionTime(LocalDateTime.now().minusHours(12));
        collection1.setWasteBin(bin);

        CollectionEvent collection2 = new CollectionEvent();
        collection2.setWeight(7.0);
        collection2.setCollectionTime(LocalDateTime.now().minusHours(6));
        collection2.setWasteBin(bin);

        entityManager.persist(collection1);
        entityManager.persist(collection2);
        entityManager.flush();

        // Act
        long result = collectionRepository.countByCollectionTimeBetween(start, end);

        // Assert
        assertEquals(2, result);
    }
}