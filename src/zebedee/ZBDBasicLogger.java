// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDBasicLogger.java,v 1.3 2001/06/21 14:53:47 wintonn Exp $

package zebedee;


import java.io.*;
import java.util.Date;

import com.example.zebedee.MainActivity; // [[idleloop]] 20130220, import of resources (.R)

/**
 * <code>ZBDBasicLogger</code> provides simple file-based logging mechanisms.
 * By default messages are logged to the standard output stream and errors to
 * the standard error stream.
 */

public class ZBDBasicLogger extends ZBDLogger
{
    private PrintStream logStream = System.out;
	private PrintStream errStream = System.err;
	private MainActivity activity;
	//private TextView textView_log;
	//private ScrollView scrollView_log;
	
    /**
     * Creates a new <code>ZBDBasicLogger</code> instance with the initial
     * logging level set to <code>level</code>.
     *
     * @param level The initial logging level.
     */

    public ZBDBasicLogger(int level)
    {
	super(level);
    }

    /**
     * Creates a new <code>ZBDBasicLogger</code> instance with the initial
     * logging level set to 1.
     */

    public ZBDBasicLogger()
    {
	super(1);
    }
    
    
    // [[idleloop]] 20130223: log to a TextView control (inside a ScrollView control)
    public ZBDBasicLogger(MainActivity activity)
    {
    	super(1);
    	this.activity=activity;
    }
        

    /**
     * Writes the specified <code>message</code> to the current output
     * stream at the given logging <code>level</code>. The message is
     * only written if the current logging level is greater than the
     * specified <code>level</code>.
     * <p>
     * If the current tag string (see {@link #setTag(String)}) is not
     * null the message will be preceded by the tag string and current
     * thread name.
     * If timestamps are enabled (see {@link #setTimestamp(boolean)})
     * then this will be followed by a timestamp string. Finally the
     * message text preceded by the level number will be written out.
     *
     * @param level The level at which this message should be logged.
     * @param message The message text.
     */

    public synchronized void log(int level, String message)
    {
	if (logStream != null && level <= currentLevel)
	{
	    if (currentTag != null)
	    {
		/*logStream.print(currentTag + "(" +
				(Thread.currentThread().getName()) + "): ");*/
		// [[idleloop]] 20130216
	    addTextToTextView( currentTag + "(" +
				(Thread.currentThread().getName()) + "): ");
	    }
	    if (currentTimestamp)
	    {
		//logStream.print((new Date()) + ": ");
		// [[idleloop]] 20130216
	    addTextToTextView( (new Date()) + ": ");
	    }

	    //logStream.println(level + ": " + message);
	    // [[idleloop]] 20130216
	    addTextToTextView( level + ": " + message + "\n");	 		
	}
    }

    /**
     * Writes the specified <code>message</code> to the current error stream.
     * The format of the message is similar to that output by
     * {@link #log(int,String)} except that error messages are written
     * regardless of logging level and also contain the string
     * <code>"ERROR:"</code>.
     *
     * @param message The error message text.
     */

    public synchronized void error(String message)
    {
	if (errStream != null && currentLevel >= 0)
	{
	    if (currentTag != null)
	    {
		/*errStream.print(currentTag + "(" +
				 (Thread.currentThread().getName()) + "): ");*/
		// [[idleloop]] 20130216
	    addTextToTextView( currentTag + "(" +
				(Thread.currentThread().getName()) + "): ");
	    }
	    if (currentTimestamp)
	    {
	    //errStream.print((new Date()) + ": "); //logStream.print((new Date()) + ": ");
	    // [[idleloop]] 20130216
	    addTextToTextView( (new Date()) + ": ");
	    }

	    //errStream.println("ERROR: " + message);
	    // [[idleloop]] 20130216
	    addTextToTextView("ERROR: " + message + "\n");
	}
    }

    /**
     * Sets the current output destination. The destination, given by
     * <code>dest</code> is either the name of an output file or one
     * of the special string values <code>"NULL"</code>, <code>"STDOUT"</code>,
     * or <code>"STDERR"</code>. These, respectively, turn off logging
     * entirely, send output to the standard output or send it to the
     * standard error. The string <code>"SYSLOG"</code> is also recognised
     * and treated as a synonym for <code>"STDOUT"</code>.
     * <p>
     * If the destination is a file then messages will be appended to it
     * if it exists or a new file created otherwise. Messages will be
     * automatically flushed to the file on a line-at-a-time basis.
     *
     * @param dest The logging destination.
     *
     * @throws IOException This exception may be thrown if there is a failure
     * to open the specified destination file.
     */

    public synchronized void setLogOutput(String dest) throws IOException
    {
	if (logStream != null &&
	    logStream != System.out &&
	    logStream != System.err)
	{
	    logStream.close();
	}

	currentLogDest = dest;

	if (dest == null || dest.equals("NULL"))
	{
	    logStream = null;
	}
	else if (dest.equals("STDOUT") || dest.equals("SYSLOG"))
	{
	    logStream = System.out;
	}
	else if (dest.equals("STDERR"))
	{
	    logStream = System.err;
	}
	else
	{
	    // Open file for append and autoflush PrintStream

	    logStream = new PrintStream(new FileOutputStream(dest, true), true);
	}
    }

    /**
     * Sets the current error stream destination. This operates in an
     * identical manner to {@link #setLogOutput} but for the error stream.
     *
     * @param dest The logging destination.
     * @throws IOException This exception may be thrown if there is a failure
     * to open the specified destination file.
     */

    public synchronized void setErrorOutput(String dest) throws IOException
    {
	if (errStream != null &&
	    errStream != System.out &&
	    errStream != System.err)
	{
	    errStream.close();
	}

	currentErrorDest = dest;

	if (dest == null || dest.equals("NULL"))
	{
	    errStream = null;
	}
	else if (dest.equals("STDOUT") || dest.equals("SYSLOG"))
	{
	    errStream = System.out;
	}
	else if (dest.equals("STDERR"))
	{
	    errStream = System.err;
	}
	else
	{
	    // Open file for append and autoflush PrintStream

	    errStream = new PrintStream(new FileOutputStream(dest, true), true);
	}
    }

    // [[idleloop]] 20130223
    private void addTextToTextView(String strLog)
    {
        //append the new text to the bottom of the TextView
    	activity.setLogString(strLog);
    	// view update code MUST run on MainActivity:
    	// http://stackoverflow.com/questions/5161951/android-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-vi
    	activity.runOnUiThread(new Runnable() {
    	     public void run() {

    	    	 //stuff that updates ui
    	    	 activity.showLog();

    	    }
    	});
    	
    }

/* -- TESTING ONLY

    public static void main(String args[]) throws Exception
    {
	ZBDBasicLogger l = new ZBDBasicLogger(2);

	l.log(1, "hello at level 1");
	l.log(3, "hello at level 3");
	l.setLevel(99);
	l.log(3, "hello again at level 3");
	l.setLogOutput("test.log");
	l.log(1, "logging to file");
	l.setLogOutput("STDOUT");
	l.log(2, "back to stdout");
	l.error("it's an error");
	l.setTimestamp(true);
	l.log(1, "the time is ...");
    }
*/
}
