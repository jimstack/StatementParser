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
public class Transfer extends Transaction
{
    private String otherAccount;
    
    public Transfer(Transaction source)
    {
        super(source.date, source.amount, source.description);
    }

    public String getOtherAccount()
    {
        return otherAccount;
    }

    public void setOtherAccount(String otherAccount)
    {
        this.otherAccount = otherAccount;
    }
}
