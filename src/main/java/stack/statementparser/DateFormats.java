/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jimst
 */
public class DateFormats
{
    public static final DateFormat MONTH_DAY_FULL_YEAR_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
    public static final DateFormat MONTH_DAY_TWO_DIGIT_YEAR_FORMAT = new SimpleDateFormat("MM/dd/yy");
    public static final DateFormat MONTH_DAY_FORMAT = new SimpleDateFormat("MM/dd");
    public static final DateFormat MONTH_NAME_DAY_FORMAT = new SimpleDateFormat("MMMM dd");
    public static final DateFormat MONTH_NAME_DAY_FULL_YEAR_FORMAT = new SimpleDateFormat("MMMM dd, yyyy");
    
    private static final String MONTH_NAME_REGEX = "\\b" +
                                                   "(?:" +
                                                   "Jan(?:uary)?" +
                                                   "|Feb(?:ruary)?" +
                                                   "|Mar(?:ch)?" +
                                                   "|Apr(?:il)?" +
                                                   "|May" +
                                                   "|Jun(?:e)?" +
                                                   "|Jul(?:y)?" +
                                                   "|Aug(?:ust)?" +
                                                   "|Sep(?:tember)?" +
                                                   "|Oct(?:ober)?" +
                                                   "|Nov(?:ember)?" +
                                                   "|Dec(?:ember)?" +
                                                   ")";
    
    public static final Pattern MONTH_NAME_PATTERN = Pattern.compile(MONTH_NAME_REGEX);
    public static boolean isDateRangeWithNamedMonths(String value)
    {
        Matcher matcher = MONTH_NAME_PATTERN.matcher(value);
        if (matcher.find())
        {
            return true;
        }
        
        return false;
    }
    
    public static boolean isMonthAndDay(String value)
    {
        try
        {
            MONTH_DAY_FORMAT.parse(value);
        }
        catch (ParseException ex)
        {
            return false;
        }
        
        return true;
    }
}
