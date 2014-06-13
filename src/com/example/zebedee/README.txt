jzbd -- Zebedee in Java
=======================

WARNING: THIS IS EARLY RELEASE SOFTWARE AND HAS NOT BEEN THOROUGHLY TESTED. IT
IS PROVIDED "AS IS" WITHOUT ANY KIND OF WARRANTY. IT MAY WORK, IT MAY NOT. IT
COULD DO DIRE THINGS TO YOUR MACHINE AND WREAK HAVOC (ALTHOUGH THAT SHOULD BE
CONSTRUED AS A BUG AND REPORTED :-).

USE AT YOUR OWN RISK. YOU HAVE BEEN WARNED.

There, having got that out of the way this is an early version of Zebedee in
Java. You'll need at least JDK 1.2 (and maybe 1.3, that's what I'm using).
It's "alpha" quality software. Some stuff just doesn't work, there's bound to
be bugs in the things I think do work but it's basically functional.

The core Zebedee API (see ZBDTunnel.java) should work with JDK 1.1 (I did try
it at some point in the development). It's the classes that wrap it all up
into a standalone application that rely on JDK 1.2.

What's New
==========

2001-10-10:

Removed more post JDK-1.1 dependencies from core classes.

2001-07-03:

Clean-up of unsupported options and documentation (ha!).

2001-06-26:

Client implementation of "listenmode" (and as a side-effect a non-stub version
of the ZBDBasicValidator.validatePeer() method) added. This means that
things are now almost feature-complete apart from "keygencommand".

2001-06-21:

Server can initiate connections (clienthost is set). The matching client-side
"listenmode" is still not functional.

Client and server can now run in TCP and UDP mode simultaneously. When running
in this way all traffic goes to port 11965. If running in UDP-only mode
it will go to 11230 for compatibility with the C version. The new "ipmode"
keyword controls this.

Main class is now just "Zebedee".

2001-06-12:

UDP client and server modes now functional.

Multi-use mode setting now honoured.

2001-05-21:

First release.

Using JZBD
==========

To run it from the command line do:

	java Zebedee options ...

The options are the "normal" Zebedee command-line options and arguments.
For example:

	java Zebedee -s -v 5 targethost:daytime,telnet

for a server and:

	java Zebedee 10000:serverhost:telnet

for a client.

At a protocol level it's completely compatible with Zebedee 2.2 -- but doesn't
support old versions of the protocol. The command-line and configuration file
parsing is mostly compatible. I've dropped support for some of the old
keywords (and the -r command-line option). However, there are a couple of
new features that haven't yet made it into the C version.

You should read the Zebedee "Classic" manual in conjunction with the
following notes. It has been an aim to make the Java and C versions largely
compatible at the command-line level. Most TCP and UDP-mode client and server
functionality seems to be there and working. Here's a brief list of the
differences between this and the C version (other than being written in Java,
of course :-):

    The following options are recognised but are not (and will not be)
    supported: -D, -d, -e, -r and -S.

    The following options are recognised but are not yet supported, although
    they probably will be: -h, -H.
    
    The following configuration file keywords are recognised but are now
    considered obsolete and will cause an error:

    	redirect -- use a 'target' expression instead.
	
	localport  )
	clientport ) -- use a 'tunnel expression instead
	remoteport )
	targetport ) 

    The following keywords are recognised but are not supported:

	command		-- will not be implemented
	debug		-- will not be implemented
	detached	-- will not be implemented
	keygenlevel	-- allowed but ignored
	udpmode		-- allowed but superseded by ipmode
	udptimeout	-- allowed but superseded by idletimeout
	
    The following keywords have been added:

	idletimeout	-- Like the existing 'udptimeout' but can be applied
			   to all connections. Any tunnel without any traffic
			   within this many seconds will be closed.

	listenip	-- Specifies the address on which to listen (for
			   client or server). May be used multiple times
			   to specify multiple addresses.

	ipmode		-- This can have the value "tcp", "udp" or "both"
			   to indicate whether the program is running in
			   TCP-mode, UDP-mode or mixed mode. This replaces
			   the old "udpmode" keyword (which is still
			   supported). If running in mixed mode the server
			   will only listen on a single port (the default
			   TCP port) but will correctly handle either TCP or
			   UDP connections.
			   
	checkaddress	-- Validates that the peer's IP address matches
			   the specified address. This can be a single
			   IP address or in a form identical to that used
			   by the "target" keyword (see below). This includes
			   the ability, should you want it, to tie down
			   the source ports too!

			   You can repeat this keyword multiple times.
			   
    The following keywords have been modified:

        checkidfile	-- This may be repeated multiple times in order to
			   allow identities from multiple files to be
			   examined.

	target		-- A 'target' hostname can now be in the form of
			   a CIDR network mask, for example,

			   	10.1.1.0/24:ftp,telnet

			   This specifies that all targets within that
			   address range are permitted.

	listenmode	-- The same syntax as for "target" and "checkaddress"
			   is applied to the server name (in effect the
			   server name is added to the "checkaddress" list).
			   
There is also now a Zebedee API -- see ZBDTunnel and its associated classes.
There are a couple of simple examples of using this (other than the main code
itself) in the files CTest.java and STest.java. There are some JavaDoc
comments in the code but that's not finished yet.

This work has been supported by my employer, Internet Designers, who have
graciously allowed this to be licensed under the GNU Lesser General Public
License (see LGPL21.txt). This project was undertaken to allow me to develop
my Java skills. As a company IDL has a very enlightened skill-development
philosophy and it's a great place to work. Please stop by

	http://www.internet-designers.net/

if you're interested. And yes, we are hiring good people now. If you're based
in the UK, send me a CV if you're interested!

Neil Winton
neil.winton@idl-bt.com (aka neil@winton.org.uk)
