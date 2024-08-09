package com.cases.info.repository;

import com.cases.info.dto.VolumeDTO;
import com.cases.info.entity.CaseDetails;
import com.cases.info.entity.MasterVolumePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VolumePartCountRepository extends JpaRepository<MasterVolumePart, Long> {
    @Query(value="select * from masterVolumePart a where a.year=:year AND a.volume=:volume AND a.part=:part", nativeQuery=true)
    Optional<MasterVolumePart> findByYearAndVolumeAndPart(String year, String volume, String part);
}
