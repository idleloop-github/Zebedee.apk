package zebedee;
// Copyright 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: Getopt.java,v 1.4 2001/06/29 10:51:47 wintonn Exp $

/**
 * The <code>Getopt</code> class provides a simple command-line option parser.
 * It is intended to parse arguments in a manner compatible with the normal
 * UNIX conventions. Options are introduced with a <code>'-'</code>
 * character and may be grouped together like this:
 * <blockquote>
 *    <code>-abc</code>
 * </blockquote>
 * or specified separately, like this:
 * <blockquote>
 *    <code>-a -b -c</code>
 * </blockquote>
 * <p>
 * Where an option takes an argument it may either follow directly
 * on from the option or be separated from it by whitespace.
 * Parsing of options stops when either the first argument not beginning
 * with a <code>'-'</code> character is reached or the special option
 * <code>"--"</code> is encountered.
 *
 * @author Neil Winton
 */

public class Getopt
{
    String opts;
    String args[];
    int optind = 0; // Current index into args
    int offset = 0; // Current offset into args[optind]
    char optchar;   // Current option character
    String optarg;  // Last option argument

    /**
     * The {@link #next()} method may throw an <code>InvalidOptionException</code>.
     * If it does so the offending option character may be retrieved
     * through the {@link #getOption()} method.
     */

    static public class InvalidOptionException extends Exception
    {
	String opt;

	/**
	 * Constructor with <code>"invalid option: c"</code> message
	 * and additional text.
	 */

	InvalidOptionException(char c, String s)
	{
	    super("invalid option: " + c + ": " + s);
	    opt = new String(new char[] {c});
	}

	/** Constructor with <code>"invalid option: c"</code> message. */

	InvalidOptionException(char c)
	{
	    super("invalid option: " + c);
	    opt = new String(new char[] {c});
	}

	/** Return the offending option character (as a string). */

	public String getOption()
	{
	    return opt;
	}
    }

    /**
     * Create a new option parser.
     *
     * @param opts The option specification string. This is in the same
     * form as accepted by the standard UNIX <code>getopt</code> routine.
     * The string consists of a set of valid option characters. If an option
     * requires an argument then it is followed by a <code>':'</code>. The
     * string <code>"ab:c"</code> represents three valid options, of which
     * the second, <code>-b</code> requires an argument.
     *
     * @param args This is the array of arguments to be parsed.
     */

    public Getopt(String opts, String[] args)
    {
	this.opts = opts;
	this.args = args;
	optind = 0;
    }

    /**
     * Retrieve the next option, if any. This should be called repeatedly
     * until the value -1 is returned. The value of the option character
     * found is returned or can be retrieved by using the {@link #option()}
     * method. If the option required an argument its value can be accessed
     * through the {@link #optionArg()} method.
     *
     * @return The option character found as an <code>int</code> value
     * or -1 when the end of the options has been reached.
     * @throws Getopt.InvalidOptionException
     */

    synchronized public int next() throws InvalidOptionException
    {
	if (optind < args.length)
	{
	    // Check for more chars in current string

	    if (offset > 0)
	    {
		// Have we come top the end of the current argument?

		if (offset < args[optind].length())
		{
		    // Is the next character a valid option?

		    optchar = args[optind].charAt(offset);

		    int ix = opts.indexOf(optchar);
		    if (ix == -1)
		    {
			offset++;
			throw new InvalidOptionException(optchar);
		    }

		    // Does this option require an argument?

		    if (ix < opts.length() - 1 && opts.charAt(ix + 1) == ':')
		    {
			// Is there anything left of the current argument?

			if (offset + 1 < args[optind].length())
			{
			    optarg = args[optind].substring(offset + 1);
			    optind++;
			    offset = 0;
			}
			else
			{
			    // Is there a following argument?

			    offset = 0;
			    optind++;
			    if (optind < args.length)
			    {
				optarg = args[optind++];
			    }
			    else
			    {
				throw new InvalidOptionException(optchar, "option requires an argument");
			    }
			}
		    }
		    else    // No argument required
		    {
			offset++;
			optarg = null;
		    }

		    return optchar;
		}
		else	// Examine the next argument
		{
		    offset = 0;
		    optind++;

		    // Check whether we can carry on

		    if (optind >= args.length)
		    {
			return -1;
		    }
		}
	    }

	    // Have we reached the end-of-options marker?

	    if (args[optind].equals("--"))
	    {
		optind++;
		optchar = '-';
		optarg = null;
		return -1;
	    }

	    // Is the next argument an option?

	    if (args[optind].startsWith("-"))
	    {
		offset++;
		return next();
	    }
	}

	// No more options or arguments left!

	return -1;
    }

    /**
     * Retrieves the value of the last option parsed.
     */

    synchronized public char option()
    {
	return optchar;
    }

    /**
     * Retrieve the argument associated with the last option parsed.
     * If the option had no argument then it returns <code>null</code>.
     */

    synchronized public String optionArg()
    {
	return optarg;
    }

    /**
     * Return the remaining, unprocessed arguments as a <code>String</code>
     * array.
     */

    synchronized public String[] remainder()
    {
	String[] ret = new String[args.length - optind];
	System.arraycopy(args, optind, ret, 0, (args.length - optind));
	return ret;
    }

/* For testing, uncomment the following ...

    public static void main(String args[])
    {
	try {
	    Getopt opt = new Getopt("ab:cd:E", args);
	    while (opt.next() != -1)
	    {
		System.out.println("option = " + opt.option() + ", argument = " +
				   (opt.optionArg() == null ? "<null>" : opt.optionArg()));
	    }

	    String[] rest = opt.remainder();

	    for (int i = 0; i < rest.length; i++)
	    {
		System.out.println("extra " + i + " = " + rest[i]);
	    }
	}
	catch (Getopt.InvalidOptionException e)
	{
	    System.out.println("ERROR: " + e.getMessage());
	}
    }

*/
}
