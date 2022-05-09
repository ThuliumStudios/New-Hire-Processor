package com.sps.bullhorn;

import com.bullhornsdk.data.exception.RestApiException;
import com.bullhornsdk.data.model.entity.core.standard.Candidate;
import com.bullhornsdk.data.model.entity.core.standard.ClientCorporation;
import com.bullhornsdk.data.model.entity.core.standard.JobOrder;
import com.bullhornsdk.data.model.entity.core.standard.Placement;
import com.sps.util.Units;
import org.apache.poi.ss.usermodel.*;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BullhornTask implements Job {
    private int employeesParsed;
    private final String payroll = "D:/Southern Point Staffing/Southern Point Staffing Team Site - Finance/Payroll/Vensure";
    private final String res = "./src/main/resources";

    private Path employeePath;
    private Path depositPath;

    /**
     * TASK ORDER:
     *  (if first run) receive first placement ID as dialog input
     *  Create (copy) a new Import Template into the Finance folder
     *  Read Placement IDs from list
     *  Parse ID, pull in Placement/Candidate and map data as normal
     *  Paste mapped values into the copied spreadsheet, starting at row 8
     *  Increment Placement ID, setting the next successful query as the last placement pulled
     *  Once 10 empty placements have been cycled,
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        BullhornAPI bullhorn = (BullhornAPI) context.getJobDetail().getJobDataMap().get("bullhorn");
        employeesParsed = 0;

        Set<String> placementFields = fieldSet("candidate", "jobOrder", "dateBegin", "id", "payRate");
        Set<String> candidateFields = fieldSet("id", "name", "ssn", "firstName", "lastName", "middleName",
                "dateOfBirth", "gender", "ethnicity", "address", "email", "federalFilingStatus", "customText5",
                "customText6");

        // Copy the employee import templates to the Finance folder for editing
        System.out.println("Copying file");
        try {
            employeePath = Path.of(payroll).resolve(Units.today('-') + "_EmployeeImport.xlsx");
            depositPath = Path.of(payroll).resolve(Units.today('-') + "_EmployeeDeposit.xlsx");

            Files.copy(Path.of(res).resolve("Employee Import Template.xlsx"), employeePath);
            Files.copy(Path.of(res).resolve("Employee Deposit Template.xlsx"), depositPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: Programmatically determine
        int currentPlacement = 6997;

        // Loop and process Placement/Candidates until 10 IDs are empty
        int attempts = 0;
        while (attempts < 10) {
            Placement p;
            try {
                p = bullhorn.getPlacement(currentPlacement++, placementFields);
            } catch (RestApiException e) { // Move on
                // e.printStackTrace();
                System.out.println("No Placement found with ID=" + (currentPlacement - 1));
                attempts++;
                continue;
            }

            // Pull in and pass the values associated with this placement
            Candidate candidate = bullhorn.getCandidate(p.getCandidate().getId(), candidateFields);
            JobOrder job = bullhorn.get(JobOrder.class, p.getJobOrder().getId(),
                    fieldSet("id", "title", "clientCorporation"));
            ClientCorporation company = bullhorn.get(ClientCorporation.class, job.getClientCorporation().getId(),
                    fieldSet("id", "name", "customText8"));

            generateEmployeeValues(p, candidate, job, company);
            employeesParsed++;
            attempts = 0;
        }
        currentPlacement -= 10;
        System.out.println("Ended. 10 attempts, stopped at " + currentPlacement);
    }

    /**
     * Routing number: customText5
     * Account number: customText6
     *
     * @param p
     */
    public void generateEmployeeValues(Placement p, Candidate c, JobOrder job, ClientCorporation company) {
        List<String> employee = new ArrayList<>();
        List<String> deposit = new ArrayList<>();

        System.out.println("Looping through candidate " + c.getName());
        for (String s : new String[] {"16727", c.getSsn(), c.getFirstName(), c.getLastName(), c.getMiddleName(),
                Units.formatDate(c.getDateOfBirth()), c.getGender(), "X" /** TODO: c.getEthnicity() */,
                "TODO: Marital Status",
                c.getEmail(), c.getAddress().getAddress1(), "", c.getAddress().getCity(), c.getAddress().getState(),
                c.getAddress().getZip(), "", "", "A", "F", "", Units.formatDate(p.getDateBegin()),
                Units.formatDate(p.getDateBegin()), Units.formatDate(p.getDateBegin()), "", "", company.getCustomText8(),
                "Weekly", p.getPayRate().toString(), "Hourly", "40", "", "", "", company.getName(), job.getTitle(),
                "PRIMARY", company.getCustomText8(), "", "", "", "", "", "", "", c.getFederalFilingStatus(),
                "", "", "", "", ""})
            employee.add(s);
        for (String s : new String[] {"16727", c.getName(), "A", "RVS", "", "C", c.getCustomText5(), c.getCustomText6(), "RVS", "B",
                "", "", "P"})
            deposit.add(s);

        try {
            generateFile(employeePath, employee);
            generateFile(depositPath, deposit);

            employee.clear();
            deposit.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void generateFile(Path path, List<String> fields) throws IOException {
        // Instantiate workbook
        FileInputStream excelReader = new FileInputStream(path.toString());
        Workbook workbook = WorkbookFactory.create(excelReader);
        Sheet worksheet = workbook.getSheetAt(0);

        // Parse data into rows
        AtomicInteger col = new AtomicInteger();
        Row row = worksheet.getRow(7 + employeesParsed);
        fields.forEach(field -> {
            Cell cell = row.getCell(col.get());
            cell.setCellValue(field);
            col.getAndIncrement();
        });
        excelReader.close();

        // Write to Excel file and close connections
        FileOutputStream excelWriter = new FileOutputStream(path.toString());
        workbook.write(excelWriter);
        excelWriter.close();
        workbook.close();
    }

    private Set<String> fieldSet(String... fields) {
        Set<String> fieldSet = new HashSet<>();
        fieldSet.addAll(Arrays.asList(fields));
        return fieldSet;
    }
}