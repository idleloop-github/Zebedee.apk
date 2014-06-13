// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZebedeeServer.java,v 1.10 2001/06/29 10:42:13 wintonn Exp $

package com.example.zebedee;

import zebedee.*;

import java.io.*;
import java.net.*;
import java.util.*;

import zebedee.ZBDException;
import zebedee.ZBDExternalKeySource;
import zebedee.ZBDLogger;
import zebedee.ZBDParseException;
import zebedee.ZBDTcpTunnelReader;
import zebedee.ZBDTcpTunnelWriter;
import zebedee.ZBDTunnel;
import zebedee.ZBDTunnelServer;
import zebedee.ZBDUdpTunnelReader;
import zebedee.ZBDUdpTunnelWriter;
import zebedee.ZBDValueException;

public class ZebedeeServer extends Zebedee
{
    ZBDTunnelServer master;


    public ZebedeeServer()
    {
	super();
    }

    public ZebedeeServer(Zebedee that)
    {
	super(that);
    }

    public ZebedeeServer(String fileName) throws ZBDParseException
    {
	parseFile(fileName);
    }

    public ZebedeeServer(String options[]) throws ZBDParseException
    {
	parseOptions(options);
    }

    public void execute() throws ZBDException
    {
	master = new ZBDTunnelServer(tcpMode, udpMode);

	master.setDefaultTarget(defaultTarget);
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

	// If in TCP-mode or in combined TCP/UDP mode then use the
	// default TCP server port if one has not already been set.
	// Otherwise use the default UDP-mode port.

	if (tcpMode)
	{
	    if (serverPort == -1)
	    {
		serverPort = ZBDTunnel.DFLT_TCP_PORT;
	    }
	}
	else
	{
	    if (serverPort == -1)
	    {
		serverPort = ZBDTunnel.DFLT_UDP_PORT;
	    }
	}

	if (clientHost != null)
	{
	    new Thread(new ServerInitiator(master, clientHost, serverPort)).start();
	}
	else
	{
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

		    new Thread(new ServerListener(master, addr, serverPort)).start();
		}
	    }
	}
    }

    public void execute(String args[]) throws ZBDException
    {
	for (int i = 0; i < args.length; i++)
	{
	    try
	    {
		validator.addTarget(args[i]);
	    }
	    catch (Exception e)
	    {
		throw new ZBDException("invalid target specification: "
				       + args[i]);
	    }
	}

	if (defaultTarget == null)
	{
	    defaultTarget = validator.defaultTarget();
	    if (defaultTarget == null)
	    {
		defaultTarget = "localhost";
		validator.addTarget("localhost");
	    }
	}

	execute();
    }
}

class ServerListener implements Runnable
{
    InetAddress addr;
    int port;
    ZBDTunnelServer master;
    ZBDLogger logger;


    ServerListener(ZBDTunnelServer master, InetAddress addr, int port)
    {
	this.master = master;
	this.port = port;
	this.addr = addr;
	logger = master.getLogger();
    }

    public void run()
    {
	ServerSocket listenSock = null;

	try
	{
	    listenSock = new ServerSocket(port, 50, addr);
	    logger.log(1, "server listening on " + addr + ":" +
		       listenSock.getLocalPort());
	}
	catch (Exception e)
	{
	    logger.error("failed to create server listener socket for " +
			 addr + ":" + port + ": " + e);
	    return;
	}

	while (true)
	{
	    Socket client = null;
	    ZBDTunnelServer t = new ZBDTunnelServer(master);

	    try
	    {
		client = listenSock.accept();
		logger.log(1, "accepted connection from " +
			   client.getInetAddress());

		t.connect(client);
		logger.log(1, (t.isUdpMode() ? "UDP" : "TCP") +
			   "-mode tunnel from " + client.getInetAddress()
			   + " established");
	    }
	    catch (Exception e)
	    {
		logger.error("failed to establish tunnel from " +
			     client.getInetAddress());
		logger.error("reason: " + e);
		if (!(e instanceof ZBDException))
		{
		    e.printStackTrace();
		}
		try
		{
		    client.close();
		}
		catch (Exception eio)
		{
		    // Ignore
		}
		continue;
	    }

	    if (t.isUdpMode())
	    {
		new ZBDUdpTunnelReader(t, (DatagramSocket)t.getTargetSocket(), true).start();
		new ZBDUdpTunnelWriter(t, (DatagramSocket)t.getTargetSocket(), true).start();
	    }
	    else
	    {
		new ZBDTcpTunnelReader(t, (Socket)t.getTargetSocket()).start();
		new ZBDTcpTunnelWriter(t, (Socket)t.getTargetSocket()).start();
	    }
	}
    }
}

class ServerInitiator implements Runnable
{
    String clientHost;
    int port;
    ZBDTunnelServer master;
    ZBDLogger logger;


    ServerInitiator(ZBDTunnelServer master, String client, int port)
    {
	this.master = master;
	this.clientHost = client;
	this.port = port;
	logger = master.getLogger();
    }

    public void run()
    {
	while (true)
	{
	    Socket clientSock = null;
	    ZBDTunnelServer t = new ZBDTunnelServer(master);

	    try
	    {
		clientSock = new Socket(clientHost, port);
		logger.log(1, "made connection to " + clientHost +
			   ":" + port);

		t.connect(clientSock);
		logger.log(1, (t.isUdpMode() ? "UDP" : "TCP") +
			   "-mode tunnel from " + clientSock.getInetAddress()
			   + " established");
	    }
	    catch (Exception e)
	    {
		logger.error("failed to establish tunnel to " +
			     clientHost);
		logger.error("reason: " + e);
		if (!(e instanceof ZBDException))
		{
		    e.printStackTrace();
		}
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

	    if (t.isUdpMode())
	    {
		new ZBDUdpTunnelReader(t, (DatagramSocket)t.getTargetSocket(), true).start();
		new ZBDUdpTunnelWriter(t, (DatagramSocket)t.getTargetSocket(), true).start();
	    }
	    else
	    {
		new ZBDTcpTunnelReader(t, (Socket)t.getTargetSocket()).start();
		new ZBDTcpTunnelWriter(t, (Socket)t.getTargetSocket()).start();
	    }
	}
    }
}
