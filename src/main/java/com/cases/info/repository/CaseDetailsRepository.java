package com.cases.info.repository;

import com.cases.info.entity.CaseDetails;
import com.cases.info.entity.MasterVolumePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaseDetailsRepository extends JpaRepository<CaseDetails, String> {
    Optional<CaseDetails> findByCaseIdAndMasterVolumePart(String name, MasterVolumePart parent);
}
