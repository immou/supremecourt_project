package com.cases.info.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="mastervolumepart")
public class MasterVolumePart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String year;
    String volume;
    String part;
    Integer count;
    @OneToMany(mappedBy = "masterVolumePart", cascade = CascadeType.ALL, orphanRemoval = true)
    List<CaseDetails> caseDetailsList;
    /*public void addCaseDetails(List<CaseDetails> caseDetailsList) {
        this.caseDetailsList = caseDetailsList;
        this.caseDetailsList.forEach(c -> c.setMasterVolumePart(this));
    }*/
}
