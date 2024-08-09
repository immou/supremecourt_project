package com.cases.info.service;

import com.cases.info.dto.PartDTO;
import com.cases.info.dto.VolumeDTO;
import com.cases.info.entity.CaseDetails;
import com.cases.info.entity.MasterVolumePart;
import com.cases.info.repository.CaseDetailsRepository;
import com.cases.info.repository.VolumePartCountRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CaseDataServiceImpl implements CaseDataService{
    @Autowired
    RestTemplate restTemplate;

    @Autowired
    CaseDetailsRepository caseDetailsRepository;
    @Autowired
    VolumePartCountRepository volumePartCountRepository;
    @Override
    public void fetchCaseVolumePerYear(List<String> years) throws URISyntaxException, IOException, InterruptedException {
        System.out.println("years"+years);
        ObjectMapper mapper = new ObjectMapper();
        String url = "https://digiscr.sci.gov.in/admin/judgement_ebooks/ajax/judgemnt_ajax";

        for(String year : years) {
            MultiValueMap<String, String> formDataVolume = new LinkedMultiValueMap<>();
            formDataVolume.add("tag", "year");
            formDataVolume.add("year", year);
            ResponseEntity<String> volumeResponse = callJudgementAPI(url, formDataVolume);
            System.out.println("volume response: "+ volumeResponse.getBody());
            List<VolumeDTO> volumeList = (List<VolumeDTO>)mapper.readValue(volumeResponse.getBody(), new TypeReference<List<VolumeDTO>>() {});
           // System.out.println("volume list: "+ volumeList);
            for(VolumeDTO volume : volumeList){
                //System.out.println(volume.getVolume_id());
               // System.out.println(volume.getVolume_name());
                String volumeId = volume.getVolume_id();
                MultiValueMap<String, String> formDataPart = new LinkedMultiValueMap<>();
                formDataPart.add("tag", "part-value");
                formDataPart.add("year", year);
                formDataPart.add("volume", volumeId);
                ResponseEntity<String> partResponse = callJudgementAPI(url, formDataPart);
                System.out.println("part response: "+ partResponse.getBody());
                List<PartDTO> partList = mapper.readValue(partResponse.getBody().toLowerCase(), new TypeReference<List<PartDTO>>() {});
                fetchCaseDetailsPopulateDB(year,volumeId, partList);
            }
        }
        //return null;
    }

    private void fetchCaseDetailsPopulateDB(String year, String volumeId, List<PartDTO> partList) throws URISyntaxException, IOException, InterruptedException {
        String docUrl = "https://digiscr.sci.gov.in/fetch_judgement_ajax";
        String casePage = "";

        for(PartDTO part : partList){
            MultiValueMap<String, String> formDataDoc = new LinkedMultiValueMap<>();
            System.out.print("volume : "+volumeId +"-");
            System.out.println("part : "+part.getId());
            formDataDoc.add("year", year);
            formDataDoc.add("volume",volumeId);
            formDataDoc.add("partno", part.getId());

            ResponseEntity<String> docResponse = callJudgementAPI(docUrl, formDataDoc);
            casePage = docResponse.getBody();
            parseCasePageResponse(casePage, formDataDoc);
        }
    }

    //@Transactional
    private void parseCasePageResponse(String casePage,MultiValueMap<String, String> formData) {
        String pdfContextPath = "https://digiscr.sci.gov.in/";
        Document document = Jsoup.parse(casePage);
        Element totalRecordSpan = document.body().getElementsByClass("records").get(0).child(0);
        List<CaseDetails> caseDetailsList = new ArrayList<>();
        Elements liList = document.body().getElementsByClass("linumbr");
        System.out.println("size number"+liList.size());
        MasterVolumePart volPart = null;
        String fileName="";
        String fileDate="";
        String dateInUse="";
        try{
            volPart = new MasterVolumePart();
            volPart.setYear(formData.getFirst("year"));
            volPart.setVolume(formData.getFirst("volume"));
            volPart.setPart(formData.getFirst("partno"));
            volPart.setCount(liList.size());
            //volumePartCountRepository.save(volPart);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if(liList.size() > 0){
            for(int i=0; i< liList.size(); i++){
                CaseDetails caseDetails = new CaseDetails();
                Elements divList = liList.get(i).children();
                String anchor =divList.get(0).getElementsByTag("a").get(0).html();
                String party = anchor.substring(0,anchor.indexOf("<span")) + " Vs " + anchor.substring(anchor.indexOf("</")+7);
                Elements caseTypeNumDate =divList.get(1).getElementsByClass("civil").get(0).children();
                String caseTypeNum = caseTypeNumDate.get(0).html();
                String caseType = caseTypeNum.substring(caseTypeNum.indexOf('(')+1, caseTypeNum.indexOf('/')).trim();
                String caseNumber = caseTypeNum.substring(caseTypeNum.indexOf('/')+1, caseTypeNum.lastIndexOf(')')).trim();
                String judgementDate = caseTypeNumDate.get(1).html();
                Elements judges = caseTypeNumDate.get(2).getElementsByClass("entryjudgment").get(0).children();
               // Elements verdictJudges = judges.attr("class","author");
                String judgeNames = "";
                String judgementBy = "";
                if(judges.size() > 0){
                    for(int j=0; j<judges.size(); j++){
                        String className = "";
                        className = judges.get(j).attr("class");
                        if(judges.get(j).attr("class").equals("author")){
                            judgementBy = judgementBy + judges.get(j).html() + ", ";
                        }
                        if(judges.size() > 1) {
                            judgeNames = judgeNames + judges.get(j).html() + ", ";
                        }
                        else {
                            judgeNames = judgeNames + judges.get(j).html();
                        }
                    }
                }
                String combinedJudgesName = judgeNames;
                if(StringUtils.isNotBlank(judgeNames) && judgeNames.contains("<sup>*</sup>")){
                    combinedJudgesName = judgeNames.replace( "<sup>*</sup>", "");
                }
                if(StringUtils.isNotBlank(combinedJudgesName) && combinedJudgesName.substring(combinedJudgesName.length()-2).equals(", ")){
                    combinedJudgesName=combinedJudgesName.substring(0,combinedJudgesName.length()-2);
                }
                if(StringUtils.isNotBlank(judgementBy) && judgementBy.contains("<sup>*</sup>")){
                    judgementBy = judgementBy.replace( "<sup>*</sup>", "");
                }
                if(StringUtils.isNotBlank(judgementBy) && judgementBy.substring(judgementBy.length()-2).equals(", ")){
                    judgementBy=judgementBy.substring(0,judgementBy.length()-2);
                }
                String casePDFPath = divList.get(2).child(0).child(3).getElementsByTag("a").attr("href");
                String casePDF = pdfContextPath + casePDFPath;
                try{
                    String[] dateParts = judgementDate.trim().split(" ");
                    String day = dateParts[0];
                    String month = dateParts[1].substring(0,3);
                    String year = dateParts[2];
                    fileDate = day+"-"+month+"-"+year;
                   /* DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
                    String date = fileDate;
                    LocalDate localDate = LocalDate.parse(date, formatter);*/
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy"); // Set your date format
                    dateInUse = sdf.format(new Date(fileDate));
                    fileName = caseNumber;
                    if(caseNumber.contains("/")){
                        fileName=caseNumber.replace("/","-");
                    }
                    File fileDownload = new File("C:/Technology-Prctice-Training/Microservice_Project/Student_Management/CaseDocument/src/main/resources/"+fileName+"_"+dateInUse+".pdf");
                    FileUtils.copyURLToFile(new URL(casePDF), fileDownload, 60000, 60000);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                caseDetails.setCaseId(fileName+"-"+dateInUse);
                caseDetails.setCaseNumber(caseNumber);
                caseDetails.setCaseType(caseType);
                caseDetails.setJudgementDate(judgementDate);
                caseDetails.setPetitionerRespondent(party);
                caseDetails.setBench(combinedJudgesName);
                caseDetails.setJudgementBy(judgementBy);
                caseDetails.setCaseDocLink(casePDF);
                caseDetails.setMasterVolumePart(volPart);
                caseDetailsList.add(caseDetails);
                System.out.println(caseDetails);

            }
            populateDBData(caseDetailsList, volPart);
        }
    }

    @Transactional
    private void populateDBData(List<CaseDetails> caseDetailsList, MasterVolumePart volumePart) {
        if(caseDetailsList.size()>0){
            volumePart.setCaseDetailsList(caseDetailsList);
        }
        MasterVolumePart existingParent = volumePartCountRepository.findByYearAndVolumeAndPart(volumePart.getYear(), volumePart.getVolume(), volumePart.getPart())
                .orElse(null);

        if (existingParent != null) {
            // Update existing children or add new ones
            for (CaseDetails newChild : volumePart.getCaseDetailsList()) {
                caseDetailsRepository.findByCaseIdAndMasterVolumePart(newChild.getCaseId(), existingParent)
                        .ifPresentOrElse(existingChild -> {
                            // Update existing child
                            existingChild.setCaseId(newChild.getCaseId());
                            existingChild.setCaseType(newChild.getCaseType());
                            existingChild.setJudgementDate(newChild.getJudgementDate());
                            existingChild.setPetitionerRespondent(newChild.getPetitionerRespondent());
                            existingChild.setBench(newChild.getBench());
                            existingChild.setJudgementBy(newChild.getJudgementBy());
                            existingChild.setCaseDocLink(newChild.getCaseDocLink());
                            existingChild.setMasterVolumePart(newChild.getMasterVolumePart());;
                        }, () -> {
                            // Add new child
                            newChild.setMasterVolumePart(existingParent);
                            existingParent.getCaseDetailsList().add(newChild);
                        });
            }

            volumePartCountRepository.save(existingParent);
        } else {
            // Save new parent along with children
            volumePartCountRepository.save(volumePart);
        }
    }

    public ResponseEntity<String> callJudgementAPI(String url, MultiValueMap<String, String> formData) throws URISyntaxException, IOException, InterruptedException {


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(formData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url, request , String.class);

        //System.out.println(response.getBody());

        return response;
    }

    private String getFormDataAsString(Map<String, String> formData) {
        StringBuilder formBodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
            if (formBodyBuilder.length() > 0) {
                formBodyBuilder.append("&");
            }
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
            formBodyBuilder.append("=");
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
        }
        return formBodyBuilder.toString();
    }

}
