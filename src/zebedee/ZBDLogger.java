// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDLogger.java,v 1.3 2001/06/21 14:53:49 wintonn Exp $

package zebedee;


import java.io.IOException;

/**
 * The abstract base class for logging mechanism implementations.
 */

abstract public class ZBDLogger
{
    /**
     * The current logging level. Messages logged using
     * {@link #log(int,String) log} with a level less than or equal to this
     * value should be output, those with a level greater than this value
     * should  not.
     */
    protected int currentLevel = 1;

    /**
     * The current message prefix tag. This gives a string to be prefixed
     * to messages logged with the {@link #log(int,String) log} or
     * {@link #error(String) error} methods.
     */
    protected String currentTag = "jzbd";

    /**
     * This holds the current message loggindestination. The interpretation of
     * this value is left to sub-classes. It is initially set to the value
     * <code>"STDOUT"</code>. See {@link #log(int,String)}.
     */
    protected String currentLogDest = "STDOUT";

    /**
     * This holds the current error destination. The interpretation of
     * this value is left to sub-classes. It is initially set to the value
     * <code>"STDOUT"</code>. See {@link #error(String)}.
     */
    protected String currentErrorDest = "STDERR";

    /**
     * This indicates whether messages should be timestamped. The default
     * value is <code>false</code>.
     */
    protected boolean currentTimestamp = false;


    /**
     * Creates a new <code>ZBDLogger</code> instance with the
     * {@link #currentLevel} set to the given <code>level</code>.
     *
     * @param level The initial value for {@link #currentLevel}.
     */

    public ZBDLogger(int level)
    {
	currentLevel = level;
    }

    /**
     * Creates a new <code>ZBDLogger</code> instance with the
     * {@link #currentLevel} set to 1.
     */

    public ZBDLogger()
    {
	this(1);
    }

    /**
     * Writes the specified <code>message</code> to the current output
     * stream at the given logging <code>level</code>. The exact interpretation
     * and format of such messages is defined by a sub-class.
     *
     * @param level The level at which the message is bing logged.
     * @param message The message text.
     */
    abstract public void log(int level, String message);

    /**
     * Writes the specified <code>message</code> to the current error stream.
     *  The exact interpretation and format of such messages is defined by a
     * sub-class.
     *
     * @param message The error message text.
     */
    abstract public void error(String message);

    /**
     * Sets the current logging level ({@link #currentLevel}).
     *
     * @param level The new value of the logging level.
     */

    public synchronized void setLevel(int level)
    {
	currentLevel = level;
    }

    /**
     * Retrieves the current logging level.
     */

    public synchronized int getLevel()
    {
	return currentLevel;
    }

    /**
     * Sets the current message tag ({@link #currentTag}).
     *
     * @param tag The new value of the tag string.
     */

    public synchronized void setTag(String tag)
    {
	currentTag = tag;
    }

    /**
     * Retrieves the current message tag.
     */

    public synchronized String getTag()
    {
	return currentTag;
    }

    /**
     * Sets the current logging destination ({@link #currentLogDest}).
     * Note that the interpretation of destination names is left to
     * sub-classes.
     *
     * @param dest The new value of the destination.
     *
     * @throws IOException This exception may be thrown if an error occurs
     * in assigning the new destinaton.
     */

    public synchronized void setLogOutput(String dest) throws IOException
    {
	currentLogDest = dest;
    }

    /**
     * Retrieves the current logging destination.
     */

    public synchronized String getLogOutput()
    {
	return currentLogDest;
    }

    /**
     * Sets the current error destination ({@link #currentErrorDest}).
     * Note that the interpretation of destination names is left to
     * sub-classes.
     *
     * @param dest The new value of the destination.
     *
     * @throws IOException This exception may be thrown if an error occurs
     * in assigning the new destinaton.
     */

    public synchronized void setErrorOutput(String dest) throws IOException
    {
	currentErrorDest = dest;
    }

    /**
     * Retrieves the current error destination.
     */

    public synchronized String getErrorOutput()
    {
	return currentErrorDest;
    }

    /**
     * Sets a the flag requesting message timestamping ({@link #currentTimestamp}).
     *
     * @param onOff Turn timestamping on (<code>true</code>) or off
     * (<code>false</code>).
     */

    public synchronized void setTimestamp(boolean onOff)
    {
	currentTimestamp = onOff;
    }

    /**
     * Retrieves the current timetamping value.
     */

    public synchronized boolean getTimestamp()
    {
	return currentTimestamp;
    }
}
