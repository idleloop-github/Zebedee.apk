// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDTunnelServer.java,v 1.10 2001/07/03 15:31:54 wintonn Exp $

package zebedee;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.math.*;


public class ZBDTunnelServer extends ZBDTunnel
{
    Object targetSocket = null;
    String defaultTarget = "localhost";
    boolean clientUdpMode = false;


    // Constructor
    //
    // Default constructor. Use this version to create an initial "master"
    // ZBDTunnelServer for later cloning.

    public ZBDTunnelServer()
    {
	super();
    }

    // Constructor
    //
    // Specify TCP/UDP mode

    public ZBDTunnelServer(boolean tcpMode, boolean udpMode)
    {
	super();
	this.tcpMode = tcpMode;
	this.udpMode = udpMode;
    }

    // Constructor
    //
    // Clone settings from "master", share token table and access list

    public ZBDTunnelServer(ZBDTunnelServer master)
    {
	super(master);
	clientUdpMode = false;
	defaultTarget = master.defaultTarget;
    }

    /**
     * Establish a connection with the client on <code>tunnelSocket</code>
     * and using the supplied target connection.
     */

    final public void connect(Socket tunnelSocket, Object targetSocket)
	throws ZBDException
    {
	this.targetSocket = targetSocket;
	connect(tunnelSocket);
    }

    // connect
    //
    // Given and already-open socket to the Zebedee client enter into
    // negotiation to establish a tunnel.

    final public void connect(Socket tunnelSocket) throws ZBDException
    {
	if (!validator.validatePeer(tunnelSocket))
	{
	    throw new ZBDValidationException("failed to validate client source address");
	}

	this.tunnelSocket = tunnelSocket;

	// Set idle timeout

	try
	{
	    tunnelSocket.setSoTimeout(idleTimeout * 1000);
	}
	catch (SocketException e)
	{
	    throw new ZBDException("failed to set idle timeout (" + idleTimeout
				   + "): " + e);
	}

	// Get the input and output data streams

	try
	{
	    dataIn = new DataInputStream(tunnelSocket.getInputStream());
	    dataOut = new DataOutputStream(tunnelSocket.getOutputStream());
	}
	catch (IOException eio)
	{
	    throw new ZBDNetworkException("can't retrieve IO streams: " + eio);
	}

	// Initialise our nonce value.

	new Random().nextBytes(serverNonce);

	// Now enter negotiation with the client

	negotiate();

	// Indicated that we are connected and read to go!

	readOK = true;
	writeOK = true;
    }

    // negotiate
    //
    // This is the server side of the Zebedee protocol negotiation

    final void negotiate() throws ZBDException
    {
	int request = 0;
	int serverToken = 0;

	try
	{
	    request = dataIn.readShort();

	    if (request < protocol)
	    {
		throw new ZBDProtocolException("client requested incompatible protocol version ("
				       + Integer.toHexString(request) + ")");
	    }

	    // Send back our supported version

	    dataOut.writeShort(protocol);
	}
	catch (EOFException eof)
	{
	    throw new ZBDNetworkException("EOF encountered while reading protocol version");
	}
	catch (InterruptedIOException eint)
	{
	    throw new ZBDNetworkException("connection timed out while reading protocol version");
	}
	catch (IOException eio)
	{
	    throw new ZBDNetworkException("IO error while negotiating protocol version");
	}

	logger.log(3, "received protocol version " + Integer.toHexString(request)
		+ ", sent " + Integer.toHexString(protocol));

	// Create a byte array output stream for the header data.
	// We wrap this into a DataOutputStream to allow easy
	// writing of the data.

	ByteArrayOutputStream hdrOut = new ByteArrayOutputStream(MAX_HDR_SIZE);
	DataOutputStream dHdrOut = new DataOutputStream(hdrOut);

	// Read the client protocol header fields

	int clientUdpRequest = -1;
	int clientBufSize = -1;
	int clientCmpInfo = -1;
	int clientTargetPort = -1;
	int clientKeySize = -1;
	int clientToken = -1;
	String clientTargetAddr;
	boolean refused = false;

	try
	{
	    // Read all of the header fields

	    clientUdpRequest = dataIn.readUnsignedShort();
	    clientBufSize = dataIn.readUnsignedShort();
	    clientCmpInfo = dataIn.readUnsignedShort();
	    clientTargetPort = dataIn.readUnsignedShort();
	    clientKeySize = dataIn.readUnsignedShort();
	    clientToken = dataIn.readInt();
	    dataIn.read(clientNonce, 0, NONCE_SIZE);
	    clientTargetAddr = addrToString(dataIn.readInt());
	}
	catch (EOFException eof)
	{
	    throw new ZBDNetworkException("EOF encountered while reading protocol header");
	}
	catch (InterruptedIOException eint)
	{
	    throw new ZBDNetworkException("connection timed out while reading protocol header");
	}
	catch (IOException eio)
	{
	    throw new ZBDNetworkException("IO error processing protocol header");
	}

	// Sanity check some values

	if (clientUdpRequest == -1 || clientBufSize == -1 || clientCmpInfo == -1 ||
	    clientTargetPort == -1 || clientKeySize == -1)
	{
	    throw new ZBDNetworkException("EOF encountered while reading protocol header");
	}

	clientUdpMode = (clientUdpRequest == HDR_FLAG_UDPMODE);

	try
	{
	    // Check TCP vs UDP mode

	    if ((clientUdpMode && !udpMode) || (!clientUdpMode && !tcpMode))
	    {
		logger.log(0, "client requested " + (clientUdpMode ? "UDP" : "TCP") +
			" mode and server is in " + (!clientUdpMode ? "TCP" : "") +
			" mode");
		refused = true;
	    }
	    if (clientUdpMode)
	    {
		dHdrOut.writeShort(udpMode ? HDR_FLAG_UDPMODE : 0);
	    }
	    else
	    {
		dHdrOut.writeShort(tcpMode ? 0 : HDR_FLAG_UDPMODE);
	    }

	    logger.log(3, "accepted request for " + (clientUdpMode ? "UDP mode" : "TCP mode"));

	    // Accept buffer size request provided > 0 and < bufferSize

	    if (clientBufSize <= 0)
	    {
		logger.log(0, "client request buffer size <= 0 (" + clientBufSize + ")");
		clientBufSize = bufferSize;
	    }
	    else
	    {
		setBufferSize(clientBufSize < bufferSize ? clientBufSize : bufferSize);
	    }
	    dHdrOut.writeShort(bufferSize);
	    logger.log(3, "responding with buffer size = " + bufferSize);

	    // Accept server compression level provided <= ours

	    setCompression(clientCmpInfo > compressionInfo ? compressionInfo : clientCmpInfo);
	    dHdrOut.writeShort(compressionInfo);
	    logger.log(3, "responding with compression = " + Integer.toHexString(compressionInfo));

	    // Now we know whether compression will be applied we
	    // can initialise the inflater/deflater.
	    // FIX-ME handle BZIP2 too.

	    if (compressionInfo > 0)
	    {
		inflater = new Inflater(false);
		deflater = new Deflater(compressionInfo, false);
	    }

	    // Check the target port/host combination

	    if (clientTargetAddr.equals("0.0.0.0"))
	    {
		clientTargetAddr = defaultTarget;
	    }

	    if (!validator.validateTarget(clientTargetAddr, clientTargetPort, clientUdpMode))
	    {
		logger.log(3, "client requested connection to disallowed target "
			+ clientTargetAddr + ":" + clientTargetPort);
		dHdrOut.writeShort(0);
		refused = true;
	    }
	    else if (targetSocket == null)
	    {
		try
		{
		    if (clientUdpMode)
		    {
			DatagramSocket s = new DatagramSocket();
			s.connect(InetAddress.getByName(clientTargetAddr),
				  clientTargetPort);
			targetSocket = s;
		    }
		    else
		    {
			targetSocket = new Socket(clientTargetAddr, clientTargetPort);
		    }
		    logger.log(3, "accepted target = " + clientTargetAddr +
			       ":" + clientTargetPort);
		    dHdrOut.writeShort(clientTargetPort);
		}
		catch (Exception e)
		{
		    logger.log(3, "failed to connect to target " + clientTargetAddr +
			    ":" + clientTargetPort + ": " + e);
		    dHdrOut.writeShort(0);
		    refused = true;
		}
	    }
	    else
	    {
		logger.log(3, "using already supplied target socket");
	    }

	    // Accept client key size provides >= our minimum and <= our max

	    logger.log(3, "client requested key size = " + clientKeySize);

	    if (clientKeySize < getMinKeySize())
	    {
		clientKeySize = getMinKeySize();
	    }
	    if (clientKeySize > getKeySize())
	    {
		clientKeySize = getKeySize();
	    }
	    logger.log(3, "replying with key size = " + clientKeySize);
	    dHdrOut.writeShort(getKeySize());

	    // See if the requested key reuse token exists in our token
	    // table. If so we will reply with it (and use the shared key)
	    // otherwise we generate a new token.

	    logger.log(3, "client requested key reuse token = " + clientToken);
	    if (tokens.getKeyForToken(clientToken) == null)
	    {
		clientToken = tokens.generateToken(clientToken);
	    }
	    logger.log(3, "replying with key reuse token = " + clientToken);
	    dHdrOut.writeInt(clientToken);

	    // Send our nonce value back

	    logger.log(3, "sending nonce = " + bytesToHex(serverNonce));
	    dHdrOut.write(serverNonce, 0, NONCE_SIZE);

	    // Pad the header with zero bytes

	    dHdrOut.writeInt(0);

	    // Now send the header data and then read the request

	    logger.log(3, "sending protocol header, size = " + hdrOut.size() + " bytes");

	    dataOut.write(hdrOut.toByteArray(), 0, hdrOut.size());
	}
	catch (Exception e)
	{
	    throw new ZBDNetworkException("error while sending protocol header:" + e);
	}

	// Check whether we refused the connection and throw an exception
	// now if so.

	if (refused)
	{
	    throw new ZBDValidationException("negotiation for connection to " +
				   clientTargetAddr + ":" + clientTargetPort +
				   " failed");
	}

	// OK, we now have a negotiated set of parameters. Now see if we
	// have a previously generated secret key associated with the
	// clientToken.

	String contextKey = tokens.getKeyForToken(clientToken);
	String sessionKey = null;

	if (contextKey != null)
	{
	    logger.log(5, "found context key: " + contextKey);

	    sessionKey = generateSessionKey(contextKey);

	    // Initialise input and output Blowfish streams

	    setupBlowfish(sessionKey);

	    // Perform challenge-request dialogue

	    challengeResponse();
	}
	else if (clientKeySize > 0)
	{
	    // Send the DH generator

	    logger.log(3, "sending DH generator: " + generator);
	    try
	    {
		writeString(generator);
	    }
	    catch (Exception e)
	    {
		throw new ZBDNetworkException("error writing DH generator: " + e);
	    }

	    logger.log(3, "sending DH modulus: " + modulus);
	    try
	    {
		writeString(modulus);
	    }
	    catch (Exception e)
	    {
		throw new ZBDNetworkException("error writing DH modulus: " + e);
	    }

	    // Generate our private key (the DH exponent)

	    String exponent = generatePrivateKey();

	    // Now do the Diffie-Hellman calculation

	    String dhKey = diffieHellman(generator, modulus, exponent);
	    logger.log(5, "public DH key is " + dhKey);

	    // Send this to the client

	    try
	    {
		writeString(dhKey);
	    }
	    catch (Exception e)
	    {
		throw new ZBDNetworkException("error writing DH key to client: " + e);
	    }

	    // Read the client DH key

	    int num = readMessage();
	    if (num < 0)
	    {
		throw new ZBDNetworkException("EOF while reading client DH key");
	    }

	    String clientDhKey = null;
	    try
	    {
		clientDhKey = new String(message, 0, num - 1, "ASCII");
	    }
	    catch (UnsupportedEncodingException e)
	    {
		// Ignore -- ASCII is guaranteed
	    }
	    logger.log(5, "client DH key = " + clientDhKey);

	    // Validate the identity at this point

	    if (!validator.validateIdentity(generator, modulus, clientDhKey))
	    {
		throw new ZBDValidationException("failed to validate client identity");
	    }
	    else
	    {
		logger.log(3, "validated client identity");
	    }

	    // Now generate the shared secret key

	    String sharedKey = diffieHellman(clientDhKey, modulus, exponent);

	    logger.log(5, "shared key = " + sharedKey);

	    // Create a shared session key

	    sessionKey = generateSessionKey(sharedKey);

	    logger.log(5, "session key = " + sessionKey);

	    // Initialise Blowfish state

	    setupBlowfish(sessionKey);

	    // Perform challenge-request dialogue

	    challengeResponse();

	    // Record the shared key against the server-supplied token

	    tokens.setCurrentToken(clientToken, sharedKey);
	}

	// Yippee! We made it!
    }

    // setDefaultTarget

    public synchronized String setDefaultTarget(String host)
    {
	defaultTarget = host;
	return host;
    }

    // getDefaultTarget

    public String getDefaultTarget()
    {
	return defaultTarget;
    }

    public Object getTargetSocket()
    {
	return targetSocket;
    }

    public boolean isUdpMode()
    {
	return clientUdpMode;
    }

    // challengeResponse
    //
    // Engage in challenge-request dialogue with the client

    final void challengeResponse() throws ZBDException
    {
	// Generate our challenge and send it to the client

	byte[] challenge = generateChallenge();

	try
	{
	    writeMessage(challenge);
	}
	catch (ZBDException e)
	{
	    throw new ZBDNetworkException("failed writing challenge to client: " + e);
	}

	// Get the reponse and validate it

	if (readMessage() != CHALLENGE_SIZE)
	{
	    throw new ZBDNetworkException("failed to read challenge response from client");
	}

	challengeAnswer(message);
	for (int i = 0; i < CHALLENGE_SIZE; i++)
	{
	    if (message[i] != challenge[i])
	    {
		throw new ZBDProtocolException("client responsed incorrectly to challenge");
	    }
	}

	// Read the challenge from the client

	byte[] clientChallenge = new byte[CHALLENGE_SIZE];

	if (readMessage(clientChallenge) != CHALLENGE_SIZE)
	{
	    throw new ZBDNetworkException("failed to read challenge from client");
	}

	logger.log(999, "read challenge " + bytesToHex(clientChallenge));

	// Transform the challenge into the answer

	challengeAnswer(clientChallenge);

	logger.log(999, "challenge answer is " + bytesToHex(clientChallenge));

	// Write the answer back

	try
	{
	    writeMessage(clientChallenge);
	}
	catch (ZBDException e)
	{
	    throw new ZBDNetworkException("failed writing challenge response to client: " + e);
	}
    }

    // addrToString
    //
    // Convert IPv4 address in network byte order to a string

    static final String addrToString(int addr)
    {
	return (((addr >> 24) & 0xff) + "." + ((addr >> 16) & 0xff) + "." +
		((addr >> 8 ) & 0xff) + "." + (addr & 0xff));
    }
}
