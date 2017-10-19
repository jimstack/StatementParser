/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Locale;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

/**
 *
 * @author jimst
 */
public class WorkbookUtils
{
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    
    public static void addSheet(String name, Collection<? extends Transaction> transactions, HSSFWorkbook workbook)
    {
        int rowNumber = 0;
        HSSFSheet sheet = workbook.createSheet(name);
        Row headerRow = sheet.createRow(rowNumber++);
        sheet.createFreezePane(0, 1);

        int columnNumber = 0; 
        headerRow.createCell(columnNumber++).setCellValue("Date");
        headerRow.createCell(columnNumber++).setCellValue("Amount");
        headerRow.createCell(columnNumber++).setCellValue("Description");

        for (Transaction transaction : transactions)
        {
            Row row = sheet.createRow(rowNumber++);

            columnNumber = 0; 
            row.createCell(columnNumber++).setCellValue(transaction.getDate());
            try
            {
                row.createCell(columnNumber++).setCellValue(NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue());
            }
            catch (ParseException ex)
            {
                row.createCell(columnNumber++).setCellValue("Parse error: " + transaction.getAmount());
            }
            row.createCell(columnNumber++).setCellValue(transaction.getDescription());
        }
        
        ++columnNumber; // Leave a blank column
        headerRow.createCell(columnNumber++).setCellValue("Sum:");
        headerRow.createCell(columnNumber++, CellType.FORMULA).setCellFormula("SUM(B2:B" + (rowNumber+1) + ")");
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(2);
    }
}
