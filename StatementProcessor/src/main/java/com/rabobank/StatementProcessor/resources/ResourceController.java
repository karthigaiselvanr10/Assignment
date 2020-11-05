package com.rabobank.StatementProcessor.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rabobank.StatementProcessor.vo.RecordVO;

@Controller
public class ResourceController {
	private static final String CSV_FILE_PATH = "./src/main/webapp/WEB-INF/records.csv";
	private static final String XML_FILE_PATH = "./src/main/webapp/WEB-INF/records.xml";
	private static final String REPORT_CSV_FILE = "REPORT_CSV.csv";
	private static final String REPORT_XML_FILE = "REPORT_XML.csv";
	
	private static final String RECORD="record";
	private static final String REFERENCE="Reference";
	private static final String DESCRIPTION="Description";
	private static final String ERRORMESSAGE="ErrorMessage";
	private static final String XML_REFERENCE_TEXT="reference";
	private static final String XML_ACCOUNT_NUMBER="accountNumber";
	private static final String XML_START_BALANCE="startBalance";
	private static final String XML_MUTUATION="mutation";
	private static final String XML_END_BALANCE="endBalance";
	private static final String ERROR_MESSAGE_DESCRIPTION="Mutuation Balance Error";
	private static final String ERROR_MESSAGE_DUPLICATE="DuplicateReferenceNumber";
	private static final String CSV_ACCOUNT_NUMBER="AccountNumber";
	private static final String CSV_DESCRIPTION="Description";
	private static final String CSV_END_BALANCE="End Balance";
	private static final String CSV_MUTUATION="Mutation";
	private static final String CSV_START_BALANCE="Start Balance";
	private static final String XML_DESCRIPTION="description";
	

	@RequestMapping("/")
	public String home(Map<String, Object> model) {
		return "index";
	}
	@CrossOrigin("*")
	@RequestMapping("/generatecsvreport")
	void getCsvReport(HttpServletResponse response) throws IOException{
		Reader reader = Files.newBufferedReader(Paths.get(CSV_FILE_PATH));
		CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
				.withFirstRecordAsHeader()
				.withIgnoreHeaderCase()
				.withTrim());
		response.setContentType("application/download");
		response.setHeader("Content-disposition", "attachment; filename="+REPORT_CSV_FILE);
		List<RecordVO> csvWholeData = new ArrayList<RecordVO>();
		List refernceDuplicateCheck = new ArrayList();
		Set duplicaateRefernce = new HashSet();
		Map<String, Integer> header=csvParser.getHeaderMap();
		List<RecordVO> allData = extractDataFromCSV(csvParser);
		getReferenceData(refernceDuplicateCheck, allData);
		getDuplicateData(refernceDuplicateCheck, duplicaateRefernce);
		getExtractedData(csvWholeData, duplicaateRefernce, allData);
		List<RecordVO> invalidData=validateData(allData,csvWholeData);
		getReport(invalidData,response);
	}
	
	@CrossOrigin("*")
	@RequestMapping("/generatexmlreport")
	void getXmlReport(HttpServletResponse response) throws ParserConfigurationException, SAXException{
		try{
			//Get Document Builder
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new File(XML_FILE_PATH));
			document.getDocumentElement().normalize();
			Element root = document.getDocumentElement();
			System.out.println(root.getNodeName());
			NodeList nList = document.getElementsByTagName(RECORD);
			response.setContentType("application/download");
			response.setHeader("Content-disposition", "attachment; filename="+REPORT_XML_FILE);
			List<RecordVO> allRecord=getAllRecordsFromXML( nList);
			List<RecordVO> csvWholeData = new ArrayList();
			List refernceDuplicateCheck = new ArrayList();
			Set duplicaateRefernce = new HashSet();
			getReferenceData(refernceDuplicateCheck, allRecord);
			getDuplicateData(refernceDuplicateCheck, duplicaateRefernce);
			getExtractedData(csvWholeData, duplicaateRefernce, allRecord);
			List<RecordVO> invalidData=validateData(allRecord,csvWholeData);
			getReport(invalidData,response);
		}catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void getReport(List<RecordVO> records,HttpServletResponse response) throws IOException {
		try{
			CSVPrinter csvPrinter = new CSVPrinter(response.getWriter(), CSVFormat.DEFAULT
					.withHeader(REFERENCE, DESCRIPTION,ERRORMESSAGE));
			for (RecordVO record : records) {
				csvPrinter.printRecord(record.getReference(),record.getDescription(),record.getErrorMessage());
			}
			response.getWriter().flush();            
		}catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	/**
	 * @param allRecord
	 * @param nList
	 * @throws NumberFormatException
	 * @throws DOMException
	 */
	private static List<RecordVO> getAllRecordsFromXML(NodeList nList)
			throws NumberFormatException, DOMException {
		List<RecordVO> allRecord=new ArrayList<>();
		for (int temp = 0; temp < nList.getLength(); temp++)
		{
			Node node = nList.item(temp);
			RecordVO record = new RecordVO();//Just a separator
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element eElement = (Element) node;
				record.setReference(Integer.parseInt(eElement.getAttribute(XML_REFERENCE_TEXT)));
				record.setAccountNumber(eElement.getElementsByTagName(XML_ACCOUNT_NUMBER).item(0).getTextContent());
				record.setDescription(eElement.getElementsByTagName(XML_DESCRIPTION).item(0).getTextContent());
				record.setStartBalance(Double.parseDouble(eElement.getElementsByTagName(XML_START_BALANCE).item(0).getTextContent()));
				record.setMutation(Double.parseDouble(eElement.getElementsByTagName(XML_MUTUATION).item(0).getTextContent()));
				record.setEndBalance(Double.parseDouble(eElement.getElementsByTagName(XML_END_BALANCE).item(0).getTextContent()));
			}
			allRecord.add(record);
		}
		return allRecord;
	}

	private static List<RecordVO>  validateData(List<RecordVO> allRecord,List<RecordVO> csvWholeData){
		for (RecordVO record : allRecord) {
			DecimalFormat df=new DecimalFormat("0.00");
			String format = df.format(record.getStartBalance()+record.getMutation()); 

			Double formattedvalue = Double.parseDouble(format);

			if(!(record.getEndBalance().equals(formattedvalue))){
				RecordVO tempRec =new RecordVO();
				tempRec.setReference(record.getReference());
				tempRec.setDescription(record.getDescription());
				tempRec.setErrorMessage(ERROR_MESSAGE_DESCRIPTION);
				csvWholeData.add(tempRec);
			}

		}
		return csvWholeData;
	}
	
	
	/**
	 * @param csvParser
	 * @param csvWholeData
	 * @param duplicaateRefernce
	 * @param allData
	 */
	private static void getExtractedData(List<RecordVO> csvWholeData, Set duplicaateRefernce,
			List<RecordVO> allData) {
		for (RecordVO csvRecord : allData) {
			if(duplicaateRefernce.contains(csvRecord.getReference())){
				RecordVO newRecord=new RecordVO();
				newRecord.setReference(csvRecord.getReference());
				newRecord.setDescription(csvRecord.getDescription());
				newRecord.setErrorMessage(ERROR_MESSAGE_DUPLICATE);
				csvWholeData.add(newRecord);
			}
		}
	}

	/**
	 * @param refernceDuplicateCheck
	 * @param duplicaateRefernce
	 */
	private static void getDuplicateData(List refernceDuplicateCheck, Set duplicaateRefernce) {
		for(int i=0; refernceDuplicateCheck.size()>i;i++){
			int freq = Collections.frequency(refernceDuplicateCheck, refernceDuplicateCheck.get(i));
			if(freq > 1){
				duplicaateRefernce.add(refernceDuplicateCheck.get(i));
			}
		}
	}

	/**
	 * @param refernceDuplicateCheck
	 * @param allData
	 */
	private static void getReferenceData(List refernceDuplicateCheck, List<RecordVO> allData) {
		for (RecordVO csvRecord : allData) {
			refernceDuplicateCheck.add(csvRecord.getReference());
		}
	}

	/**
	 * @param csvParser
	 * @return
	 * @throws NumberFormatException
	 */
	private static List<RecordVO> extractDataFromCSV(CSVParser csvParser) throws NumberFormatException {
		List<CSVRecord> allRecords= new ArrayList<>();

		List<RecordVO> allData =new ArrayList<>();
		Iterator<CSVRecord> records= csvParser.iterator();
		while(records.hasNext()){
			CSVRecord recordscsv=records.next();
			RecordVO record=new RecordVO();
			record.setReference(Integer.parseInt(recordscsv.get(REFERENCE)));
			record.setAccountNumber(recordscsv.get(CSV_ACCOUNT_NUMBER));
			record.setDescription(recordscsv.get(CSV_DESCRIPTION));
			record.setEndBalance(Double.parseDouble(recordscsv.get(CSV_END_BALANCE)));
			record.setMutation(Double.parseDouble(recordscsv.get(CSV_MUTUATION)));
			record.setStartBalance(Double.parseDouble(recordscsv.get(CSV_START_BALANCE)));
			allData.add(record);
		}
		return allData;
	}
}