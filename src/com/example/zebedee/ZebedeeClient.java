// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZebedeeClient.java,v 1.11 2001/07/03 15:31:48 wintonn Exp $

package com.example.zebedee;

import zebedee.*;

import java.util.*;
import java.net.*;
import java.io.*;

import zebedee.ZBDException;
import zebedee.ZBDExternalKeySource;
import zebedee.ZBDLogger;
import zebedee.ZBDNetworkException;
import zebedee.ZBDParseException;
import zebedee.ZBDTcpTunnelReader;
import zebedee.ZBDTcpTunnelWriter;
import zebedee.ZBDTimeoutException;
import zebedee.ZBDTunnel;
import zebedee.ZBDTunnelClient;
import zebedee.ZBDTunnelInfoList;
import zebedee.ZBDUdpTunnelReader;
import zebedee.ZBDValueException;

/**
 * This class implements a Zebedee client program. Basic option parsing
 * is handled by the superclass, {@link Zebedee}. This class assumes
 * that the program definitely is a client and starts up a number of
 * local listener threads, waiting for connections on local end-points.
 */

public class ZebedeeClient extends Zebedee
{
    ZBDTunnelClient master;


    public ZebedeeClient()
    {
	super();
    }

    public ZebedeeClient(Zebedee that)
    {
	super(that);
    }

    public ZebedeeClient(String fileName) throws ZBDParseException
    {
	parseFile(fileName);
    }

    public ZebedeeClient(String options[]) throws ZBDParseException
    {
	parseOptions(options);
    }

    public void execute() throws ZBDException
    {
	// Check that we actually do have a tunnel spec of some form.

	if (tunnelInfoList.size() == 0)
	{
	    throw new ZBDException("no tunnel specification given");
	}

	// If we still do not have a server host then take it from
	// the first target.

	if (serverHost == null)
	{
	    serverHost = tunnelInfoList.get(0).getTarget();
	}

	// Figure out the target port ...

	if (serverPort == -1)
	{
	    if (udpMode && !tcpMode)
	    {
		serverPort = ZBDTunnel.DFLT_UDP_PORT;
	    }
	    else
	    {
		serverPort = ZBDTunnel.DFLT_TCP_PORT;
	    }
	}


	logger.log(2, "client tunnels to server on " + serverHost + ":" + serverPort);
	
	master = new ZBDTunnelClient(serverHost, serverPort);
	master.setGenerator(generator);
	master.setModulus(modulus);
	master.setCompression(cmpType);
	master.setKeySize(keySize);
	master.setMinKeySize(minKeySize);
	master.setKeyLifetime(keyLifetime);
	master.setIdleTimeout(idleTimeout);
	master.setBufferSize(maxBufSize);
	master.setPrivateKey(privateKey);
	master.setLogger(logger);
	master.setValidator(validator);
	if (keyGenCmd != null)
	{
	    master.setKeySource(new ZBDExternalKeySource(keyGenCmd));
	}

	startListeners();
    }

    public void execute(String args[]) throws ZBDException
    {
	for (int i = 0; i < args.length; i++)
	{
	    // Special case -- first argument may be the server host name
	    // and not a tunnel spec.

	    if (i == 0 && args[i].indexOf(':') == -1)
	    {
		serverHost = args[i];
	    }
	    else
	    {
		try
		{
		    tunnelInfoList.add(args[i]);
		}
		catch (Exception e)
		{
		    throw new ZBDException("invalid tunnel specification: "
					   + args[i]);
		}
	    }
	}

	execute();
    }

    void startListeners() throws ZBDException
    {
	ServerSocket serverSocket = null;

	if (listenMode)
	{
	    try
	    {
		serverSocket = new ServerSocket(serverPort);
		serverSocket.setSoTimeout(connectTimeout * 1000);
		logger.log(1, "listening for server-initiated connections on port " + serverPort);
	    }
	    catch (Exception e)
	    {
		throw new ZBDNetworkException("can't listen for server connections on port "
					      + serverPort + ": " + e);
	    }

	    // We will validate the address of servers connecting ...

	    validator.addPeer(serverHost);
	}

	for (int i = 0; i < tunnelInfoList.size(); i++)
	{
	    ZBDTunnelInfoList.TunnelInfo elem = 
		(ZBDTunnelInfoList.TunnelInfo)tunnelInfoList.get(i);
	    int[] from = elem.getFrom();
	    int[] to = elem.getTo();

	    synchronized (listenAddrList)
	    {
		if (listenAddrList.size() == 0)
		{
		    listenAddrList.add((Object)"0.0.0.0");
		}

		ListIterator iter = listenAddrList.listIterator();

		while (iter.hasNext())
		{
		    String name = (String)iter.next();
		    InetAddress addr = null;
		    try
		    {
			addr = InetAddress.getByName(name);
		    }
		    catch (UnknownHostException e)
		    {
			throw new ZBDValueException("invalid local listen address: " + name);
		    }

		    for (int fromPort = from[0]; fromPort <= from[1]; fromPort++)
		    {
			int toPort = to[0] + (fromPort - from[0]);

			Runnable listener;

			// Create the right kind of listener

			if (udpMode)
			{
			    master.setUdpMode(true);
			    listener =
				new ClientUdpListener(master,
						      addr,
						      fromPort,
						      elem.getTarget(),
						      toPort,
						      serverSocket,
						      this);
			    new Thread(listener).start();
			}

			if (tcpMode)
			{
			    master.setTcpMode(true);
			    listener =
				new ClientTcpListener(master,
						      addr,
						      fromPort,
						      elem.getTarget(),
						      toPort,
						      serverSocket,
						      this);
			    new Thread(listener).start();
			}
		    }
		}
	    }
	}
    }
}

abstract class ClientListener implements Runnable
{
    InetAddress localAddr;
    int localPort;
    int targetPort;
    String targetHost;
    ZBDTunnelClient master;
    ZBDLogger logger;
    ServerSocket serverSocket = null;
    ZebedeeClient client;
    InetAddress serverAddr;
    int serverMask;
    boolean multiUse = true;


    ClientListener(ZBDTunnelClient master,
		   InetAddress localAddr, int localPort,
		   String targetHost, int targetPort,
		   ServerSocket serverSocket,
		   ZebedeeClient client)
    {
	this.master = master;
	this.localPort = localPort;
	this.targetHost = targetHost;
	this.targetPort = targetPort;
	this.localAddr = localAddr;
	this.serverSocket = serverSocket;
	this.client = client;
	logger = master.getLogger();
	multiUse = client.multiUse;
    }

    abstract public void run();

    Socket getServerConnection() throws ZBDException
    {
	try
	{
	    Socket sock = serverSocket.accept();
	    return sock;
	}
	catch (InterruptedIOException eio)
	{
	    throw new ZBDTimeoutException("timeout waiting for connection from server");
	}
	catch (Exception e)
	{
	    throw new ZBDException("error accepting server connection: " + e);
	}
    }
}

class ClientTcpListener extends ClientListener
{


    ClientTcpListener(ZBDTunnelClient master,
		      InetAddress localAddr, int localPort,
		      String targetHost, int targetPort,
		      ServerSocket serverSocket, ZebedeeClient client)
    {
	super(master,
	      localAddr, localPort,
	      targetHost, targetPort,
	      serverSocket, client);
    }

    public void run()
    {
	ServerSocket listenSock = null;

	try
	{
	    listenSock = new ServerSocket(localPort, 50, localAddr);
	    localPort = listenSock.getLocalPort();
	    logger.log(1, "listening on " + localAddr + ":" + localPort);
	}
	catch (Exception e)
	{
	    logger.error("failed to create listener socket for " +
			 localAddr + ":" + localPort + ": " + e);
	    return;
	}

	boolean firstTime = true;
	while (multiUse || firstTime)
	{
	    Socket clientSock = null;
	    ZBDTunnelClient t = new ZBDTunnelClient(master);

	    t.setTcpMode(true);
	    t.setUdpMode(false);

	    try
	    {
		clientSock = listenSock.accept();
		logger.log(1, "accepted connection from " +
			   clientSock.getInetAddress());

		if (serverSocket != null)
		{
		    Socket sock = getServerConnection();
		    t.connect(sock, targetHost, targetPort);
		}
		else
		{
		    t.connect(targetHost, targetPort);
		}

		logger.log(1, "connected tunnel to " + targetHost + ":" +
			   targetPort);
	    }
	    catch (Exception e)
	    {
		logger.error("failed to establish tunnel to " +
			     targetHost + ":" + targetPort);
		logger.error("reason: " + e);
		try
		{
		    clientSock.close();
		}
		catch (Exception eio)
		{
		    // Ignore
		}
		continue;
	    }

	    new ZBDTcpTunnelReader(t, clientSock).start();
	    new ZBDTcpTunnelWriter(t, clientSock).start();

	    firstTime = false;
	}

	// If we get here, we will exit once all child threads have
	// exited. We can, however, close the listening socket.

	try { listenSock.close(); } catch (Exception e) {}
    }
}

class ClientUdpListener extends ClientListener
{
    DatagramSocket masterSock;
    UdpTunnelTable table = new UdpTunnelTable();
    int idleTimeout = 0;


    ClientUdpListener(ZBDTunnelClient master,
		      InetAddress localAddr, int localPort,
		      String targetHost, int targetPort,
		      ServerSocket serverSocket, ZebedeeClient client)
    {
	super(master,
	      localAddr, localPort,
	      targetHost, targetPort,
	      serverSocket, client);
	idleTimeout = client.idleTimeout;
    }

    public void run()
    {
	try
	{
	    masterSock = new DatagramSocket(localPort, localAddr);
	    masterSock.setSoTimeout(idleTimeout * 1000);
	    localPort = masterSock.getLocalPort();
	    logger.log(1, "listening for UDP traffic on " +
		       localAddr + ":" + localPort);
	}
	catch (Exception e)
	{
	    logger.error("can't create datagram socket for " +
			 localAddr + ":" + localPort + ": " + e);
	    return;
	}

	DatagramPacket packet =
	    new DatagramPacket(new byte[ZBDTunnel.MAX_BUFFER_SIZE],
			       ZBDTunnel.MAX_BUFFER_SIZE);

	InetAddress fromAddr = null;
	int fromPort = 0;
	ZBDTunnelClient tunnel = null;

	while (true)
	{
	    // Reset buffer length

	    packet.setLength(ZBDTunnel.MAX_BUFFER_SIZE);

	    try
	    {
		masterSock.receive(packet);
	    }
	    catch (InterruptedIOException ie)
	    {
		// Hit a timeout -- if not in multi-use mode we will quit
		// the loop.

		if (!multiUse)
		{
		    break;
		}
	    }
	    catch (Exception e)
	    {
		logger.error("failed receiving packet: " + e);
		continue;
	    }

	    fromAddr = packet.getAddress();
	    fromPort = packet.getPort();

	    OutputStream out = null;
	    try
	    {
		// See if we have a tunnel for this address/port combination

		tunnel = table.getTunnel(fromAddr, fromPort);

		// No tunnel? Create a new one.

		if (tunnel == null)
		{
		    tunnel = newTunnel(fromAddr, fromPort, serverSocket);

		    // Start a reader thread

		    new ZBDUdpTunnelReader(tunnel, masterSock,
					   fromAddr, fromPort, false).start();

		}

		out = tunnel.getOutputStream();
	    }
	    catch (Exception e)
	    {
		logger.error("can't get tunnel or output stream: " + e);
		continue;
	    }

	    try
	    {
		out.write(packet.getData(), 0, packet.getLength());
	    }
	    catch (Exception e)
	    {
		logger.error("failed writing datagram to tunnel: " + e);
	    }
	}

	// If we get here, we need to exit.

	try { masterSock.close(); } catch (Exception e) {}
    }

    ZBDTunnelClient newTunnel(InetAddress addr, int port, ServerSocket serverSocket)
	throws ZBDException, IOException
    {
	ZBDTunnelClient tunnel = new ZBDTunnelClient(master);
	tunnel.setUdpMode(true);
	tunnel.setTcpMode(false);

	if (serverSocket != null)
	{
	    Socket sock = serverSocket.accept();
	    tunnel.connect(sock, targetHost, targetPort);
	}
	else
	{
	    tunnel.connect(targetHost, targetPort);
	}

	// Add it to the hash table

	table.addTunnel(tunnel, addr, port);

	return tunnel;
    }
}

class UdpTunnelTable
{
    HashMap table = new HashMap();

    UdpTunnelTable()
    {
	// Create a background cleanup thread.

	Thread t = new Thread() {
	    public void run()
	    {
		setPriority(Thread.NORM_PRIORITY - 1);
		cleanup();
	    }
	};
	t.setDaemon(true);
	t.start();
    }

    /**
     * Get the tunnel instance associated with the given source address
     * and port.
     */

    synchronized ZBDTunnelClient getTunnel(InetAddress addr, int port)
	throws ZBDException
    {
	ZBDTunnelClient tunnel = (ZBDTunnelClient)table.get(addr + ":" + port);
	if (tunnel != null)
	{
	    // Check this is still valid for read/write. If so then
	    // return it otherwise carry on and get a new one.

	    if (!tunnel.readable() || !tunnel.writeable())
	    {
		tunnel = null;
	    }
	}

	return tunnel;
    }

    synchronized void addTunnel(ZBDTunnelClient tunnel, InetAddress addr, int port)
    {
	table.put(addr + ":" + port, tunnel);
    }

    /**
     * Background cleanup of tunnel table. This thread wakes up every
     * 10 minutes and purges invalid entries from the tunnel table. An
     * invalid entry is one where the tunnel is no longer readable or
     * writable.
     */

    void cleanup()
    {
	while (true)
	{
	    try { Thread.sleep(600000); } catch (Exception e) {}

	    synchronized (this)
	    {
		Collection values = table.values();

		for (Iterator i = table.values().iterator();
		     i.hasNext(); )
		{
		    ZBDTunnelClient t = (ZBDTunnelClient)i.next();
		    if (!t.readable() || !t.writeable())
		    {
			i.remove();
		    }
		}
	    }
	}
    }
}
