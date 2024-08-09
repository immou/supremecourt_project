package com.cases.info.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CaseDetailsDTO implements Serializable{
    String caseNumber;
    String caseType;
    String judgementDate;
    String petitionerRespondent;
    String bench;
    String judgementBy;
    String caseDocLink;
}
