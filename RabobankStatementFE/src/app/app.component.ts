import { Component, ViewChild } from '@angular/core';
import { RecordModel } from './RecordModel';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'RabobankStatementFE';

  public csvRowData: any[] = [];  
  csvColumnDefs = [
    { field: 'firstName', filter: false },
    { field: 'surName', filter: false },
    { field: 'issueCount', filter: true },
    { field: 'dateOfBirth', filter: false }
  ];
  columnFilterConf = {
    width: "350%",
    editable: true,
    filter: 'agTextColumnFilter',
    floatingFilter: true,
    resizable: true,
  };
  @ViewChild('csvFileReader') csvReader: any; 
  
  uploadListener($event: any): void {  
  
    let files = $event.srcElement.files;  
  
    if (this.isValidCSVFile(files[0])) {  
  
      let input = $event.target;  
      let reader = new FileReader();  
      reader.readAsText(input.files[0]);  
      
      reader.onload = () => {  
        let csvData = reader.result;  
        let csvRecordsArray = (<string>csvData).replace(/"/g, "").split(/\r\n|\n/); 
  
        let headersRow = this.getHeaderArray(csvRecordsArray);  
  
        this.csvRowData = this.getDataRecordsArrayFromCSVFile(csvRecordsArray, headersRow.length);  
      };  
  
    } else {  
      alert("Please import valid .csv file.");  
      this.fileReset();  
    }  
  }  
  
  getDataRecordsArrayFromCSVFile(csvRecordsArray: any, headerLength: any) {  
    let csvArr = [];  
  
    for (let i = 1; i < csvRecordsArray.length; i++) {  
      let curruntRecord = (<string>csvRecordsArray[i]).split(',');  
      if (curruntRecord.length == headerLength) {  
        let csvRecord: RecordModel = new RecordModel();  
        csvRecord.firstName = curruntRecord[0].trim();  
        csvRecord.surName = curruntRecord[1].trim();  
        csvRecord.issueCount = curruntRecord[2].trim();  
        csvRecord.dateOfBirth = curruntRecord[3].trim();
        csvArr.push(csvRecord);  
      }  
    }  
    return csvArr;  
  }  
  
  isValidCSVFile(file: any) {  
    return file.name.endsWith(".csv");  
  }  
  
  getHeaderArray(csvRecordsArr: any) {  
    let headers = (<string>csvRecordsArr[0]).split(',');  
    let headerArray = [];  
    for (let j = 0; j < headers.length; j++) {  
      headerArray.push(headers[j]);  
    }  
    return headerArray;  
  }  
  
  fileReset() {  
    this.csvReader.nativeElement.value = "";  
    this.csvRowData = [];  
  }  
}
