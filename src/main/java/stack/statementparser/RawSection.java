/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jimst
 */
public class RawSection
{
    private String name;
    private List<String> lines;
    
    public RawSection(String name)
    {
        this.name = name;
        lines = new ArrayList<>();
    }

    public String getName()
    {
        return name;
    }

    public List<String> getLines()
    {
        return lines;
    }
}
