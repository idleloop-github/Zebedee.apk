// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: Zebedee.java,v 1.11 2001/07/03 15:31:47 wintonn Exp $

package com.example.zebedee;

import zebedee.*;

import java.util.*;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;

import zebedee.Getopt;
import zebedee.ZBDBasicLogger;
import zebedee.ZBDException;
import zebedee.ZBDParseException;
import zebedee.ZBDTunnel;
import zebedee.ZBDTunnelInfoList;
import zebedee.ZBDValueException;

/**
 * This is the base class for the {@link ZebedeeClient} and
 * {@link ZebedeeServer} classes. It handles the basic configuration
 * and option parsing.
 */

public class Zebedee
{
    /** Default idle timeout for UDP-mode */
    public static final int DFLT_UDP_TIMEOUT = 300;
    /** Default timeout for server-initiated connections */
    public static final int DFLT_CONN_TIMEOUT = 300;
    /** Maximum server-initiated connection timeout */
    public static final int MAX_CONN_TIMEOUT = 65535;

    String clientHost = null;
    boolean debug = false;
    boolean detached = true;
//    String executeCmd = null;
    boolean hashString = false;
    boolean hashFile = false;
    int keySize = ZBDTunnel.DFLT_KEY_SIZE;
    boolean listenMode = false;
    String programName = "jzbd";
    String logFile = null;
    boolean serverMode = false;
    boolean makePrivateKey = false;
    boolean makePublicKey = false;
    boolean timestampLog = false;
    int serverPort = -1;
    boolean udpMode = false;
    boolean tcpMode = true;
    int verbosity = 1;
    int cmpType = ZBDTunnel.DFLT_COMPRESSION;
    int minKeySize = 0;
    int maxBufSize = ZBDTunnel.DFLT_BUFFER_SIZE;
    int localPort = 0;
    String serverHost = null;
    ZBDTunnelInfoList tunnelInfoList = new ZBDTunnelInfoList();
    String keyGenCmd = null;
    boolean multiUse = true;
    String modulus = "";
    String generator = "";
    String privateKey = null;
    int keyLifetime = ZBDTunnel.DFLT_KEY_LIFETIME;
    int idleTimeout = ZBDTunnel.DFLT_IDLE_TIMEOUT;
    LinkedList listenAddrList = new LinkedList();
    int connectTimeout = DFLT_CONN_TIMEOUT;
    ZBDBasicLogger logger = new ZBDBasicLogger();
    ZBDBasicValidator validator = new ZBDBasicValidator();
    String defaultTarget = null;
    
    Zebedee()
    {
    }
    
    // [[idleloop]] 20130223: modified logger to show messages on activity screen
    Zebedee(MainActivity activity)
    {
    	logger = new ZBDBasicLogger(activity);
    }
    	
    Zebedee(Zebedee that)
    {
	clientHost = that.clientHost;
	debug = that.debug;
	detached = that.detached;
//	executeCmd = that.executeCmd;
	hashString = that.hashString;
	hashFile = that.hashFile;
	keySize = that.keySize;
	listenMode = that.listenMode;
	programName = that.programName;
	logFile = that.logFile;
	serverMode = that.serverMode;
	makePrivateKey = that.makePrivateKey;
	makePublicKey = that.makePublicKey;
	timestampLog = that.timestampLog;
	serverPort = that.serverPort;
	udpMode = that.udpMode;
	tcpMode = that.tcpMode;
	verbosity = that.verbosity;
	cmpType = that.cmpType;
	minKeySize = that.minKeySize;
	maxBufSize = that.maxBufSize;
	localPort = that.localPort;
	serverHost = that.serverHost;
	tunnelInfoList = that.tunnelInfoList;
	keyGenCmd = that.keyGenCmd;
	multiUse = that.multiUse;
	modulus = that.modulus;
	generator = that.generator;
	privateKey = that.privateKey;
	keyLifetime = that.keyLifetime;
	idleTimeout = that.idleTimeout;
	listenAddrList = that.listenAddrList;
	connectTimeout = that.connectTimeout;
	logger = that.logger;
	validator = that.validator;
	defaultTarget = that.defaultTarget;
	
	logger.setLevel(verbosity);
    }

    //
    // [[idleloop]] 20130214: all commented, except for file parameter:
    //	
    /*
    public static void main(String args[]) throws Exception
    {
    */
    //Zebedee(String parametersFile) //throws Exception
    Zebedee(String parametersFile, MainActivity activity) //throws Exception
    {
        
    // [[idleloop]] 20130223: modified logger to show messages on activity screen	
    logger = new ZBDBasicLogger(activity);
        	
	//Getopt opts = new Getopt("c:Dde:f:Hhk:lmn:o:pPsS:tT:uv:x:z:", args);
    Getopt opts = new Getopt("c:Dde:f:Hhk:lmn:o:pPsS:tT:uv:x:z:", new String[0]);
    //Zebedee master = new Zebedee(); 
    Zebedee master = new Zebedee(activity);
	try
	{
	    /*
		while (opts.next() != -1)
	    {
		switch (opts.option())
		{
		case 'c':
		    master.clientHost = opts.optionArg();
		    break;

		case 'D':
		    master.debug = true;
		    break;

		case 'd':
		    master.detached = false;
		    master.logger.log(2, "-d option ignored");
		    break;

		case 'e':
		    throw new ZBDParseException("-e option not supported");
		    // break;

		case 'f':
			master.parseFile(opts.optionArg());
		*/
			// [[idleloop]] 20130214
		    master.parseFile(parametersFile);
		/*
		    break;

		case 'H':
		    master.hashString = true;
		    throw new ZBDParseException("-H option not yet supported");
		    // break;

		case 'h':
		    master.hashFile = true;
		    throw new ZBDParseException("-h option not yet supported");
		    // break;

		case 'k':
		    master.keySize = parseInt(opts.optionArg());
		    break;

		case 'l':
		    master.listenMode = true;
		    break;

		case 'm':
		    master.multiUse = true;
		    break;

		case 'n':
		    master.programName = opts.optionArg();
		    break;

		case 'o':
		    master.logFile = opts.optionArg();
		    break;

		case 'p':
		    master.makePrivateKey = true;
		    break;

		case 'P':
		    master.makePublicKey = true;
		    break;

		case 'r':
		    throw new ZBDParseException("-r option not supported");
		    // break;

		case 'S':
		    throw new ZBDParseException("-S option not supported");
		    // break;

		case 's':
		    master.serverMode = true;
		    break;

		case 't':
		    master.timestampLog = true;
		    break;

		case 'T':
		    master.serverPort = parseInt(opts.optionArg());
		    break;

		case 'u':
		    master.udpMode = true;
		    master.tcpMode = false;
		    break;

		case 'v':
		    master.verbosity = parseInt(opts.optionArg());
		    break;

		case 'x':
		    master.parseLine(opts.optionArg());
		    break;

		case 'z':
		    master.cmpType = parseCompression(opts.optionArg());
		    break;
		}
	    }
	    */
	}
	catch (Exception e)
	{
	    master.logger.setTag(master.programName);
	    master.logger.error(e.toString());
	    System.exit(1);
	}

	master.logger.setTag(master.programName);

	if (master.makePrivateKey)
	{
	    master.privateKey = generatePrivateKey();
	    System.out.println("privatekey '" + master.privateKey + "'");
	}

	if (master.makePublicKey)
	{
	    if (master.privateKey == null)
	    {
		master.logger.error("private key must be specified or generated");
	    }
	    else
	    {
		String dhKey = ZBDTunnel.diffieHellman(master.generator,
						       master.modulus,
						       master.privateKey);

		String id = ZBDBasicValidator.identityHash(master.generator,
							   master.modulus,
							   dhKey);
		System.out.println(id + " jzbd-generated-id");
	    }
	}

	if (master.makePublicKey || master.makePrivateKey)
	{
	    System.exit(0);
	}

	if (master.hashFile || master.hashString)
	{
	    //throw new ZBDParseException("-h and -H options not yet supported"); // 20130214 idleloop
	}

	Zebedee prog;
	master.logger.setLevel(master.verbosity);

	if (master.serverMode)
	{
	    prog = new ZebedeeServer(master);
	}
	else
	{
	    prog = new ZebedeeClient(master);
	}

	try
	{
	    prog.execute(opts.remainder());
	}
	catch (Exception e)
	{
	    master.logger.error(e.toString());
//	    e.printStackTrace();
	}
    }

    public void execute() throws ZBDException
    {
	throw new ZBDException("programmer error: Zebedee.execute() called!");
    }
    
    public void execute(String[] args) throws ZBDException
    {
	throw new ZBDException("programmer error: Zebedee.execute(String[]) called!");
    }

    void parseOptions(String lines[]) throws ZBDParseException
    {
	for (int i = 0; i < lines.length; i++)
	{
	    parseLine(lines[i]);
	}
    }

    void parseFile(String file) throws ZBDParseException
    {
	BufferedReader in;
	try
	{
	    in = new BufferedReader(new FileReader(file));
	}
	catch (Exception e)
	{
	    throw new ZBDParseException("failed to open '" + file + "' for reading: " + e);
	}

	try
	{
	    String line;
	    while ((line = in.readLine()) != null)
	    {
		parseLine(line);
	    }
	}
	catch (Exception e)
	{
	    throw new ZBDParseException("failed reading file '" + file + "': " + e);
	}
	finally
	{
	    try { in.close(); } catch (Exception e) { }
	}
    }

    void parseLine(String line) throws ZBDParseException
    {
	line = line.trim();

	// Discard comments

	if (line.startsWith("#"))
	{
	    return;
	}

	// Split into characters

	char[] chars = line.toCharArray();

	final int SPACE = 0;
	final int WORD = 1;
	final int DQUOTE = 2;
	final int SQUOTE = 3;
	final int END = 4;

	int i = 0;
	StringBuffer tokens[] = { new StringBuffer(), new StringBuffer() };
	int tokenCount = 0;
	int state = SPACE;

	while (i < chars.length && state != END)
	{
	    switch (state)
	    {
	    case SPACE:
		if (chars[i] == '#')
		{
		    state = END;
		}
		else if (chars[i] == '\"')
		{
		    state = DQUOTE;
		    i++;
		}
		else if (Character.isWhitespace(chars[i]))
		{
		    i++;
		}
		else
		{
		    state = WORD;
		}
		break;

	    case WORD:
		if (tokenCount >= 2)
		{
		    throw new ZBDParseException("too many tokens on line: " + line);
		}

		if (Character.isWhitespace(chars[i]))
		{
		    tokenCount++;
		    state = SPACE;
		}
		else
		{
		    tokens[tokenCount].append(Character.toLowerCase(chars[i]));
		    i++;
		}
		break;

	    case DQUOTE:
		if (tokenCount >= 2)
		{
		    throw new ZBDParseException("too many tokens on line: " + line);
		}

		if (chars[i] == '\"')
		{
		    tokenCount++;
		    state = SPACE;
		    i++;
		}
		else
		{
		    tokens[tokenCount].append(chars[i]);
		    i++;
		}
		break;

	    case SQUOTE:
		if (tokenCount >= 2)
		{
		    throw new ZBDParseException("too many tokens on line: " + line);
		}

		if (chars[i] == '\'')
		{
		    tokenCount++;
		    state = SPACE;
		    i++;
		}
		else
		{
		    tokens[tokenCount].append(chars[i]);
		    i++;
		}
		break;
	    }
	}

	if (state == WORD)
	{
	    tokenCount++;
	}

	// Check for blank line

	if (tokenCount == 0)
	{
	    return;
	}

	if (tokenCount != 2)
	{
	    throw new ZBDParseException("too few tokens on line: " + line);
	}

	// We now know we have two tokens, the first is the name, the
	// second the value.

	String name = tokens[0].toString();
	String value = tokens[1].toString();

	if (name.equals("server")) serverMode = parseBoolean(value);
	else if (name.equals("detached")) detached = parseBoolean(value);
	else if (name.equals("debug")) debug = parseBoolean(value);
	else if (name.equals("detached")) detached = parseBoolean(value);
	else if (name.equals("compression")) cmpType = parseCompression(value);
	else if (name.equals("keylength")) keySize = parseInt(value);
	else if (name.equals("minkeylength")) minKeySize = parseInt(value);
	else if (name.equals("maxbufsize")) maxBufSize = parseInt(value);
	else if (name.equals("verbosity")) verbosity = parseInt(value);
	else if (name.equals("serverport")) serverPort = parseInt(value);
	else if (name.equals("remotehost")) serverHost = value;
	else if (name.equals("serverhost")) serverHost = value;
	else if (name.equals("keygencommand")) keyGenCmd = value;
	else if (name.equals("keygenlevel")) ; // Ignore
	else if (name.equals("logfile")) logFile = value;
	else if (name.equals("timestamplog")) timestampLog = parseBoolean(value);
	else if (name.equals("multiuse")) multiUse = parseBoolean(value);
	else if (name.equals("include")) parseFile(value);
	else if (name.equals("modulus")) modulus = value;
	else if (name.equals("generator")) generator = value;
	else if (name.equals("privatekey")) privateKey = value;
	else if (name.equals("checkidfile")) validator.addIdFile(value);
	else if (name.equals("keylifetime")) keyLifetime = parseInt(value);
	else if (name.equals("udpmode"))
	{
	    udpMode = parseBoolean(value);
	    tcpMode = !udpMode;
	}
	else if (name.equals("keylifetime")) keyLifetime = parseInt(value);
	else if (name.equals("udptimeout")) idleTimeout = parseInt(value);
	else if (name.equals("idletimeout")) idleTimeout = parseInt(value);
	else if (name.equals("localsource"))
	{
	    if (parseBoolean(value))
	    {
		listenAddrList.add((Object)"127.0.0.1");
	    }
	}
	else if (name.equals("listenip")) listenAddrList.add((Object)value);
	else if (name.equals("listenmode")) listenMode = parseBoolean(value);
	else if (name.equals("clienthost")) clientHost = value;
	else if (name.equals("connecttimeout")) connectTimeout = parseInt(value);
	else if (name.equals("tunnel")) tunnelInfoList.add(value);
	else if (name.equals("target")) validator.addTarget(value);
	else if (name.equals("message")) logger.log(1, value);
	else if (name.equals("checkaddress")) validator.addPeer(value);
	else if (name.equals("ipmode"))
	{
	    if (value.equalsIgnoreCase("tcp"))
	    {
		tcpMode = true;
		udpMode = false;
	    }
	    else if (value.equalsIgnoreCase("udp"))
	    {
		udpMode = true;
		tcpMode = false;
	    }
	    else if (value.equalsIgnoreCase("all") ||
		     value.equalsIgnoreCase("any") ||
		     value.equalsIgnoreCase("both"))
	    {
		udpMode = tcpMode = true;
	    }
	}
	else if (name.equals("command"))
	{
	    throw new ZBDParseException("'command' keyword not supported");
	}
	else if (name.equals("redirect"))
	{
	    throw new ZBDParseException("'redirect' keyword not supported, use 'target' keyword instead");
	}
	else if (name.equals("localport") || name.equals("clientport"))
	{
	    throw new ZBDParseException("'" + name + "' keyword not supported, use 'tunnel' keyword instead");
	}
	else if (name.equals("remoteport") || name.equals("targetport"))
	{
	    throw new ZBDParseException("'" + name + "' keyword not supported, use 'tunnel' keyword instead");
	}
	else
	{
	    throw new ZBDParseException("unknown keyword: " + name);
	}
    }

    static boolean parseBoolean(String s) throws ZBDParseException
    {
	if (s.equalsIgnoreCase("true")) return true;
	if (s.equalsIgnoreCase("false")) return false;
	if (s.equalsIgnoreCase("yes")) return true;
	if (s.equalsIgnoreCase("no")) return false;
	if (s.equalsIgnoreCase("1")) return true;
	if (s.equalsIgnoreCase("0")) return false;
	throw new ZBDParseException("invalid boolean value: " + s);
    }

    static int parseCompression(String s) throws ZBDParseException
    {
	s = s.toLowerCase();
	try
	{
	    if (s.startsWith("zlib:"))
	    {
		return ZBDTunnel.makeCompressionValue(ZBDTunnel.COMPRESSION_ZLIB,
						      Integer.parseInt(s.substring(5)));
	    }
	    if (s.startsWith("bzip:"))
	    {
		return ZBDTunnel.makeCompressionValue(ZBDTunnel.COMPRESSION_BZIP2,
						      Integer.parseInt(s.substring(5)));
	    }
	    return Integer.parseInt(s);
	}
	catch (Exception e)
	{
	    throw new ZBDParseException("invalid compression: " + s);
	}
    }

    static int parseInt(String s) throws ZBDParseException
    {
	try
	{
	    return Integer.parseInt(s);
	}
	catch (NumberFormatException e)
	{
	    throw new ZBDParseException("invalid number: " + s);
	}
    }

    static String generatePrivateKey()
    {
	SecureRandom rand = new SecureRandom();
	byte[] key = new byte[20];
	rand.nextBytes(key);
	return ZBDTunnel.bytesToHex(key);
    }

    /**
     * Sets the server-initiated connection timeout value.
     * This sets the time in seconds for which a client will wait for a 
     * server to initiate a connection.
     * <p>
     * If the supplied value is greater than {@link ZBDTunnel#MAX_KEY_LIFETIME}
     * it will be set to {@link ZBDTunnel#MAX_KEY_LIFETIME}. If it is less than 0
     * an exception will be thrown.
     *
     * @param life The key lifetime, in seconds.
     *
     * @throws ZBDValueException Thrown if the lifetime is less than zero.
     */

    synchronized public int setConnectTimeout(int time) throws ZBDValueException
    {
	if (time < 0)
	{
	    throw new ZBDValueException("connect timeout less than zero (" + time + ")");
	}
	else if (time > MAX_CONN_TIMEOUT)
	{
	    time = MAX_CONN_TIMEOUT;
	}

	connectTimeout = time;
	return connectTimeout;
    }

    // getConnectTimeout

    synchronized public int getConnectTimeout()
    {
	return connectTimeout;
    }


}
