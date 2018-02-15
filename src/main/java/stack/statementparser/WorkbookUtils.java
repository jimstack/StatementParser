/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import static stack.statementparser.DateFormats.MONTH_DAY_FULL_YEAR_FORMAT;

/**
 *
 * @author jimst
 */
public class WorkbookUtils
{
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    
    public static void addSheet(String name, List<? extends Transaction> transactions, HSSFWorkbook workbook)
    {
        if (transactions.isEmpty())
        {
            return;
        }
            
        int rowNumber = 0;
        HSSFSheet sheet = workbook.createSheet(name);
        Row headerRow = sheet.createRow(rowNumber++);
        sheet.createFreezePane(0, 1);

        int columnNumber = 0; 
        headerRow.createCell(columnNumber++).setCellValue("Date");
        headerRow.createCell(columnNumber++).setCellValue("Amount");
        headerRow.createCell(columnNumber++).setCellValue("Description");
        
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        
        Iterator<Cell> cellIterator = headerRow.cellIterator();
        while (cellIterator.hasNext())
        {
            cellIterator.next().setCellStyle(cellStyle);
        }
        
        Collections.sort(transactions, new Comparator<Transaction>()
        {
            @Override
            public int compare(Transaction o1, Transaction o2)
            {
                try
                {
                    Date date1 = MONTH_DAY_FULL_YEAR_FORMAT.parse(o1.getDate()),
                         date2 = MONTH_DAY_FULL_YEAR_FORMAT.parse(o2.getDate());
                    
                    return date1.compareTo(date2);
                }
                catch (ParseException e)
                {
                    
                }
                
                return 0;
            }
        });

        for (Transaction transaction : transactions)
        {
            Row row = sheet.createRow(rowNumber++);

            columnNumber = 0; 
            row.createCell(columnNumber++).setCellValue(transaction.getDate());
            try
            {
                row.createCell(columnNumber++, CellType.NUMERIC).setCellValue(NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue());
            }
            catch (ParseException ex)
            {
                row.createCell(columnNumber).setCellValue("Parse error: " + transaction.getAmount());
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
