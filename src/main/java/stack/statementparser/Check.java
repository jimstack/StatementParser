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
public class Check extends Transaction
{
    private int checkNumber;
    private String referenceNumber;
    
    public Check(int checkNumber, String referenceNumber, String date, String amount, String description)
    {
        super(date, amount, description);
        this.checkNumber = checkNumber;
        this.referenceNumber = referenceNumber;
    }

    public int getCheckNumber()
    {
        return checkNumber;
    }

    public String getReferenceNumber()
    {
        return referenceNumber;
    }
}
