/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * This file appears (by its comments) to be (C) 1999 Brian Davies
 * Portions of this file Copyright (C) 2009 Jeremy D Monin <jeremy@nand.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The author of this program can be reached at thomas@infolab.northwestern.edu
 **/
package soc.client;

import java.awt.TextArea;

/*
 * SnippingTextArea.java 
 * Brian Davies 
 * Written 1/21/99 
 */

/**
 * Limits lines displayed to MaximumLines.  Note that the empty string is also
 * considered a line.
 */
public class SnippingTextArea extends TextArea
{
    /**
     * A bug in Java 1.4.2: the first time replaceRange() is called, it
     * deletes one char beyond what is requested, but only because it's a
     * newline (java bug 5025532 and possibly 4910757).  Afterwards it
     * appears to work normally, resulting in the oldest line in the display
     * will be short 1 char. To avoid we work on the text itself for 1.4.2. It
     * flickers, but works.
     * Added 2004-06-24 by Chad McHenry - JSettlers 1.1 CVS
     * @since 1.1.06
     */
    static final boolean isJava142 =
        System.getProperty("java.version").startsWith("1.4.2");

    /**
     * Bug in Mac OS X 10.5 java display: If multiple threads try to update display at once,
     * can hang the GUI (with rainbow "beach ball"). Non-display threads continue execution.
     * We avoid this by not snipping our text area's length. Also extending to 10.6 in case
     * it isn't fixed yet by that version.  - JDM 2009-05-21
     * To identify osx from within java, see technote TN2110:
     * http://developer.apple.com/technotes/tn2002/tn2110.html
     * @since 1.1.06
     */
    static final boolean isJavaOnOSX105 =
        SOCPlayerClient.isJavaOnOSX
        && (System.getProperty("os.version").startsWith("10.5.")
            || System.getProperty("os.version").startsWith("10.6."));

    int maximumLines = 100;
    int lines = 0;


    /**
     * Creates a SnippingTextArea which limits hard line breaks to maxLines,
     * and uses SCROLLBARS_VERTICAL_ONLY.
     */
    public SnippingTextArea(int rows, int columns, int maxLines)
    {
        this("", rows, columns, SCROLLBARS_VERTICAL_ONLY, maxLines);
    }

    /**
     * Creates a new SnippingTextArea object with specified text, which limits
     * hard line breaks to maxLines, and uses SCROOBARS_VERTICAL_ONLY.
     */
    public SnippingTextArea(String text, int maxLines)
    {
        this("", 40, 80, SCROLLBARS_VERTICAL_ONLY, maxLines);
    }

    /**
     * Creates a new SnippingTextArea object with specified text, rows,
     * columns, and scroll bar visibility, which limits hard line breaks to
     * maxLines.
     */
    public SnippingTextArea(String text, int rows, int columns,
        int scrollbars, int maxLines)
    {
        super(text, rows, columns, scrollbars);
        maximumLines = maxLines;
        lines = 1; // the empty string is a line, text==null results in empty string
        lines += countNewLines(text);
    }

    /**
     * Maximum lines this text area will display.
     */
    public int getMaximumLines()
    {
        return maximumLines;
    }

    /**
     * Set the maximum lines this text area will display, contents are snipped
     * if necessary.
     */
    public void setMaximumLines(int newMax)
    {
        maximumLines = newMax;
        snipText();
    }

    /**
     * @return current number of lines in text
     */
    public int lines() {
        return lines;
    }

    // inherit javadoc from TextArea
    public synchronized void setText(String newString)
    {
        super.setText(newString);
        lines = countNewLines(newString);
        snipText(); 
    }

    // inherit javadoc from TextArea
    public synchronized void replaceRange(String newString, int x, int y)
    {
        lines -= countNewLines(getText().substring(x,y));
        super.replaceRange(newString, x, y);
        lines += countNewLines(newString);
        snipText (); 
    }

    // inherit javadoc from TextArea
    public synchronized void insert(String newString, int x)
    {
        super.insert(newString, x);
        lines += countNewLines(newString);
        snipText();
    }

    // inherit javadoc from TextArea
    public synchronized void append(String newString)
    {
        super.append(newString);
        lines += countNewLines(newString);
        snipText();
    }

    /** Count the lines in a string of text. */
    protected  int countNewLines(String s)
    {
        int lines = 0;
        int last = -1;

        while ( (last = s.indexOf('\n', last+1)) > -1)
            lines++;

        return lines;
    }

    /**
     * Remove lines at the beginning of the text, until there are only maxLines
     * in the component.
     */
    public void snipText()
    {
        if (isJavaOnOSX105)
            return;

        while (lines > maximumLines)
        {
            String s = getText();
            int nextLine = s.indexOf('\n') + 1;

            if (isJava142) // see comment for isJava142
                super.setText(s.substring(nextLine));
            else
                super.replaceRange("", 0, nextLine);

            lines--;
        }
        // java 1.2 deprecated getPeer, adding isDisplayable()

        if (isDisplayable())
        {
            setCaretPosition(getText().length());
        }
    }

}


