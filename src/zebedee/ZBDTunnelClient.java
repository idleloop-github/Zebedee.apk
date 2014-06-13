ii// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDTunnelClient.java,v 1.11 2001/07/03 15:31:54 wintonn Exp $

package zebedee;


import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.math.*;

/**
 * This implements the client side of the Zebedee protocol. The usual usage
 * is to create an instance of the tunnel specifying the name (and optionally
 * port) of the Zebedee server host. You then set whatever parameter values
 * may be requested such as the compression level. You then call the
 * {@link #connect(String,int) connect} method to establish a tunnel to the
 * desired target host and port. If this is successful you may then retrieve
 * special {@link ZBDInputStream} and {@link ZBDOutputStream} stream instances
 * through which you can read and write data.
 * <p>
 * So, to access the "daytime" service on a remote system via a Zebedee
 * server you might do the following:
 * <pre>
 *     ZBDTunnelClient t = new ZBDTunnelClient("serverhost");
 *     t.connect(13);	// Connect to port 13 on default target
 *     ZBDInputStream s = t.getInputStream();
 *     byte[] buf = new byte[100];
 *     s.read(buf);
 *     s.close();
 *     System.out.println(new String(buf));</pre>
 * <p>
 * Where multiple tunnels to the same server will be established it is
 * better to create a "master" <code>ZBDTunnelClient</code> instance and
 * then clone other tunnel instances from this one. The master can be used
 * to establish default parameter settings and to enable sharing of previously
 * established keys with its named server. It should not usually itself be
 * connected although it is valid to clone a new instance from and already
 * connected tunnel. The clone does not, however, share the connection.
 */

public class ZBDTunnelClient extends ZBDTunnel
{
    
    String targetHost;
    int targetPort;


    /**
     * Create a <code>ZBDTunnelClient</code> instance for tunnelling
     * to the named server listening on the default port for TCP-mode
     * connections.
     *
     * @param server The server host name or address.
     */

    public ZBDTunnelClient(String server)
    {
	super();
	serverHost = server;
    }


    /**
     * Create a <code>ZBDTunnelClient</code> instance for tunnelling
     * to the named server listening on the specified port.
     *
     * @param server The server host name or address.
     * @param port The port number on which the server is listening.
     */

    public ZBDTunnelClient(String server, int port)
    {
	super();
	serverHost = server;
	serverPort = port;
    }

    /**
     * Clone settings from the specified "master" instance.
     *
     * @param master The master tunnel instance.
     */

    public ZBDTunnelClient(ZBDTunnelClient master)
    {
	super(master);
    }

    /**
     * Connect to the Zebedee server and establish a tunnel to the
     * given target host and port.
     *
     * @param host The target host name or address.
     * @param port The target port.
     */

    public void connect(String host, int port) throws ZBDException
    {
	if (serverPort == -1)
	{
	    if (udpMode && !tcpMode)
	    {
		serverPort = DFLT_UDP_PORT;
	    }
	    else
	    {
		serverPort = DFLT_TCP_PORT;
	    }
	}

	try
	{
	    tunnelSocket = new Socket(serverHost, serverPort);
	}
	catch (IOException e)
	{
	    throw new ZBDNetworkException("can't connect to " + serverHost +
				   ":" + serverPort + ": " + e);
	}

	connect(tunnelSocket, host, port);
    }

    /**
     * Connect to the given port on the server's default target.
     *
     * @param port The target port number.
     */

    public void connect(int port) throws ZBDException
    {
	connect("0.0.0.0", port);
    }

    /**
     * Given an already-open socket to the Zebedee server establish a
     * tunnel to the given host and port.
     *
     * @param sock An established connection to a Zebedee server. No
     * Zebedee protocol traffic must yet have flowed over this connection.
     * @param host The target host name or address.
     * @param port The target port.
     *
     * @throws ZBDValidationException
     * @throws ZBDNetworkException
     * @throws ZBDProtocolException
     * @throws ZBDException
     */

    final public void connect(Socket sock, String host, int port) throws ZBDException
    {
	this.tunnelSocket = sock;
	targetHost = host;
	targetPort = port;

	if (!validator.validatePeer(sock))
	{
	    throw new ZBDValidationException("failed to validate server source address");
	}

	// Set idle timeout

	try
	{
	    sock.setSoTimeout(idleTimeout * 1000);
	}
	catch (SocketException e)
	{
	    throw new ZBDNetworkException("failed to set idle timeout (" +
					  idleTimeout + "): " + e);
	}

	// Get the input and output data streams

	try
	{
	    dataIn = new DataInputStream(sock.getInputStream());
	    dataOut = new DataOutputStream(sock.getOutputStream());
	}
	catch (IOException eio)
	{
	    throw new ZBDNetworkException("can't retrieve IO streams: " + eio);
	}

	// Initialise our nonce value -- this does not have to be a
	// cryptographically strong value.

	new Random().nextBytes(clientNonce);

	// Now enter negotiation with the server

	negotiate();

	// Indicate that we are connected and ready to go!

	readOK = true;
	writeOK = true;
    }

    /**
     * This is the client side of the Zebedee protocol negotiation.
     */

    final void negotiate() throws ZBDException
    {

	int response = 0;
	int serverToken = 0;

	try
	{
	    dataOut.writeShort(protocol);
	    response = dataIn.readShort();

	    if (response != protocol)
	    {
		throw new ZBDProtocolException("server responded with incompatible protocol version (requested "
				       + Integer.toHexString(protocol)
				       + ", received "
				       + Integer.toHexString(response) + ")");
	    }
	}
	catch (EOFException eof)
	{
	    throw new ZBDNetworkException("EOF encountered while reading protocol version");
	}
	catch (InterruptedIOException eint)
	{
	    throw new ZBDTimeoutException("connection timed out reading protocol version");
	}
	catch (IOException eio)
	{
	    throw new ZBDNetworkException("IO error while negotiating protocol version");
	}

	logger.log(3, "sent protocol version " + protocol + ", received " +
	     response);

	// Create a byte array output stream for the header data.
	// We wrap this into a DataOutputStream to allow easy
	// writing of the data.

	ByteArrayOutputStream hdrOut = new ByteArrayOutputStream(MAX_HDR_SIZE);
	DataOutputStream dHdrOut = new DataOutputStream(hdrOut);

	try
	{
	    logger.log(3, "requesting " + (udpMode ? "UDP mode" : "TCP mode"));
	    dHdrOut.writeShort(udpMode ? HDR_FLAG_UDPMODE : 0);

	    logger.log(3, "requesting buffer size = " + bufferSize);
	    dHdrOut.writeShort(bufferSize);

	    logger.log(3, "requesting compression = " + compressionInfo);
	    dHdrOut.writeShort(compressionInfo);

	    logger.log(3, "requesting target port = " + targetPort);
	    dHdrOut.writeShort(targetPort);

	    logger.log(3, "requesting key size = " + keySize);
	    dHdrOut.writeShort(keySize);

	    logger.log(3, "requesting key reuse token = " + tokens.getCurrentToken());
	    dHdrOut.writeInt(tokens.getCurrentToken());

	    logger.log(3, "sending nonce = " + bytesToHex(clientNonce));
	    dHdrOut.write(clientNonce, 0, NONCE_SIZE);
	    
	    // If the target is the same as the server we send all zeroes
	    // as the target address otherwise send an explicit address.
	    logger.log(3, "DEBUG = " + targetHost);logger.log(3, "DEBUG = " + serverHost); // idleloop 20130214
	    if (targetHost.equals(serverHost))
	    {
	    	dHdrOut.write(new byte[] {0, 0, 0, 0}, 0, 4);
	    }
	    else
	    {
	    	dHdrOut.write(InetAddress.getByName(targetHost).getAddress(), 0, 4);
		}

	    // Sanity check size of buffer

	    if (hdrOut.size() != MAX_HDR_SIZE)
	    {
		throw new ZBDException("generated header buffer size (" +
				       hdrOut.size() + ") not equal to expected size (" +
				       MAX_HDR_SIZE + ")");
	    }
	}
	catch (IOException eio)
	{
	    throw new ZBDException("error creating protocol header: " + eio);
	}

	// Now send the header data and then read the response

	try
	{
	    logger.log(3, "sending protocol header");

	    dataOut.write(hdrOut.toByteArray(), 0, hdrOut.size());

	    // Check TCP vs UDP mode

	    response = dataIn.readShort();
	    if ((udpMode && response != HDR_FLAG_UDPMODE) ||
		(tcpMode && response == HDR_FLAG_UDPMODE))
	    {
		throw new ZBDProtocolException("client requested " +
					       (udpMode ? "UDP" : "TCP") +
					       " mode and server is in " +
					       (tcpMode ? "TCP" : "UDP") +
					       " mode");
	    }
	    logger.log(3, "accepted " + (udpMode ? "UDP mode" : "TCP mode"));

	    // Accept server buffer size provided > 0

	    response = dataIn.readShort();
	    if (response <= 0)
	    {
		throw new ZBDProtocolException("server responded with zero buffer size");
	    }
	    setBufferSize(response);
	    logger.log(3, "accepted buffer size = " + response);

	    // Accept server compression level provided <= ours

	    response = dataIn.readShort();
	    if (response > compressionInfo)
	    {
		throw new ZBDProtocolException("server responded with invalid compression level (" +
					       Integer.toHexString(response) +
					       ")");
	    }
	    setCompression(response);
	    logger.log(3, "accepted compression = " + response);

	    // Now we know whether compression will be applied we
	    // can initialise the inflater/deflater.
	    // FIX-ME handle BZIP2 too.

	    if (compressionInfo > 0)
	    {
		inflater = new Inflater(false);
		deflater = new Deflater(compressionInfo, false);
	    }

	    // Check that the port was accepted

	    response = dataIn.readShort();
	    if (response != targetPort)
	    {
		throw new ZBDProtocolException("server refused request for redirection to target " +
					       targetHost + ":" + targetPort + ". Answer: " + response);
	    }
	    logger.log(3, "accepted target = " + targetHost + ":" + targetPort);

	    // Accept server key size provides >= our minimum

	    response = dataIn.readShort();
	    if (response < getMinKeySize())
	    {
		throw new ZBDProtocolException("server key size too small (" +
					       response + " < " +
					       getMinKeySize() + ")");
	    }
	    setKeySize(response);
	    logger.log(3, "accepted key size = " + response);

	    // Get the key reuse token

	    serverToken = dataIn.readInt();
	    logger.log(3, "key reuse token = " + serverToken);

	    // Get the server nonce value

	    dataIn.read(serverNonce, 0, NONCE_SIZE);
	    logger.log(3, "server nonce = " + bytesToHex(serverNonce));

	    // Skip the target address data -- we do not need the response
	    // but we must consume it!

	    dataIn.readInt();
	}
	catch (EOFException eof)
	{
	    throw new ZBDNetworkException("EOF encountered while reading protocol header");
	}
	catch (InterruptedIOException eint)
	{
	    throw new ZBDTimeoutException("connection timed out reading protocol header");
	}
	catch (IOException eio)
	{
	    throw new ZBDNetworkException("IO error processing protocol header");
	}

	// OK, we now have a negotiated set of parameters. Now see if we
	// have a previously generated secret key associated with the
	// token returned by the server.

	String contextKey = tokens.getKeyForToken(serverToken);
	String sessionKey = null;

	if (contextKey != null)
	{
	    sessionKey = generateSessionKey(contextKey);

	    // Initialise input and output Blowfish streams

	    setupBlowfish(sessionKey);

	    // Perform challenge-response dialogue

	    challengeResponse();
	}
	else if (keySize > 0)
	{
	    // Read the DH generator

	    int num = readMessage();
	    if (num <= 0)
	    {
		throw new ZBDNetworkException("EOF while reading DH generator");
	    }

	    // Convert ASCII, null-terminated string to String and
	    // store in context

	    try
	    {
		setGenerator(new String(message, 0, num - 1, "ASCII"));
	    }
	    catch (UnsupportedEncodingException e)
	    {
		// Ignore -- ASCII is guaranteed
	    }

	    // Now the modulus

	    num = readMessage();
	    if (num < 0)
	    {
		throw new ZBDNetworkException("EOF while reading DH modulus");
	    }

	    try
	    {
		setModulus(new String(message, 0, num - 1, "ASCII"));
	    }
	    catch (UnsupportedEncodingException e)
	    {
		// Ignore -- ASCII is guaranteed
	    }

	    // Now get the server DH key value

	    num = readMessage();
	    if (num < 0)
	    {
		throw new ZBDNetworkException("EOF while reading server DH key");
	    }

	    String serverDhKey = null;
	    try
	    {
		serverDhKey = new String(message, 0, num - 1, "ASCII");
	    }
	    catch (UnsupportedEncodingException e)
	    {
		// Ignore -- ASCII is guaranteed
	    }

	    // Identity checking

	    if (!validator.validateIdentity(generator, modulus, serverDhKey))
	    {
		throw new ZBDValidationException("failed to validate server identity");
	    }
	    else
	    {
		logger.log(3, "validated server identity");
	    }

	    // Generate our private key (the DH exponent)

	    String exponent = generatePrivateKey();

	    // Now do the Diffie-Hellman calculation

	    String dhKey = diffieHellman(generator, modulus, exponent);

	    // Send this to the server as a ASCII, null-terminated
	    // string.

	    try
	    {
		writeString(dhKey);
	    }
	    catch (Exception e)
	    {
		throw new ZBDNetworkException("failed writing DH key to server: " + e);
	    }

	    // Now generate the shared secret key

	    String sharedKey = diffieHellman(serverDhKey, modulus, exponent);

	    logger.log(5, "shared key = " + sharedKey);

	    // Create a shared session key

	    sessionKey = generateSessionKey(sharedKey);

	    logger.log(5, "session key = " + sessionKey);

	    // Initialise Blowfish state

	    setupBlowfish(sessionKey);

	    // Perform challenge-response dialogue

	    challengeResponse();

	    // Record the shared key against the server-supplied token

	    tokens.setCurrentToken(serverToken, sharedKey);
	}

	// Yippee! We made it!
    }

    /**
     * Engage in challenge-response dialogue with the server.
     */

    final void challengeResponse() throws ZBDException
    {
	byte[] serverChallenge = new byte[CHALLENGE_SIZE];

	// Read the challenge from the server

	if (readMessage(serverChallenge) != CHALLENGE_SIZE)
	{
	    throw new ZBDNetworkException("failed to read challenge from server");
	}

	logger.log(999, "read challenge " + bytesToHex(serverChallenge));

	// Transform the challenge into the answer

	challengeAnswer(serverChallenge);

	logger.log(999, "challenge answer is " + bytesToHex(serverChallenge));

	// Write the answer back

	try
	{
	    writeMessage(serverChallenge);
	}
	catch (ZBDException e)
	{
	    throw new ZBDNetworkException("failed writing challenge response to server: " + e);
	}

	// Now generate out own challenge and send it to the server

	byte[] challenge = generateChallenge();

	try
	{
	    writeMessage(challenge);
	}
	catch (ZBDException e)
	{
	    throw new ZBDNetworkException("failed writing client challenge to server: " + e);
	}

	// Get the reponse and validate it

	if (readMessage() != CHALLENGE_SIZE)
	{
	    throw new ZBDNetworkException("failed to read challenge response from server");
	}

	challengeAnswer(message);
	for (int i = 0; i < CHALLENGE_SIZE; i++)
	{
	    if (message[i] != challenge[i])
	    {
		throw new ZBDProtocolException("server responsed incorrectly to challenge");
	    }
	}
    }
}
