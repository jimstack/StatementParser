/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

/**
 *
 * @author jimst
 */
public class RawTransaction extends RawSection
{
    private String date;
    
    public RawTransaction(String date)
    {
        super("Transaction");
        this.date = date;
    }
    
    public String getDate()
    {
        return date;
    }
}
