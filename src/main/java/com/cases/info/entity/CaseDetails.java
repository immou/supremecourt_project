package com.cases.info.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="casedetails")
public class CaseDetails {
    @Id
    @Column(length = 300)
    String caseId;
    @Column(length = 300)
    String caseNumber;
    String caseType;
    @Column(length = 300)
    String judgementDate;
    @Column(length = 300)
    String petitionerRespondent;
    @Column(length = 300)
    String bench;
    @Column(length = 300)
    String judgementBy;
    @Column(length = 500)
    String caseDocLink;
    @ManyToOne
    @JoinColumn(name = "master_id")
    MasterVolumePart masterVolumePart;
}
