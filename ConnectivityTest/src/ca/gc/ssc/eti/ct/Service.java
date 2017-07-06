package ca.gc.ssc.eti.ct;

import java.net.*;
import java.io.*;


class Service {
	private String name;
	enum Type {USER, APPLICATION, BOTH};
	private Type type;
	private String hostName;
	private int port;
	private Site[] sites;
	private InetAddress[] addresses;
	
	Service (String name, Type type, String hostName, int port, Site[] sites) {
		this.name = name;
		this.type = type;
		this.hostName = hostName;
		this.port = port;
		this.sites = sites;
	}

	public int getNumTests() {
		return 1 + sites.length;
	}
	
	public boolean isUserService() {
		return type == Type.USER || type == Type.BOTH;
	}
	
	public boolean isApplicationService() {
		return type == Type.APPLICATION || type == Type.BOTH;
	}

	public boolean lookup (PrintStream out) {
		boolean testPassed = true;
		
	try {
		out.print("Lookup " + String.format("%-35s", hostName +":"));
		long time = System.currentTimeMillis();
		InetAddress lookupAddress = InetAddress.getByName(this.hostName);
		time = System.currentTimeMillis() - time;
		if (lookupAddress instanceof Inet4Address) {
			out.print("PASS (" + time + "ms)");
			addresses = new InetAddress[sites.length];
			for (int i = 0 ; i < sites.length; i++) {
				byte[] address = new byte[] {
						lookupAddress.getAddress()[0],
						lookupAddress.getAddress()[1],
						sites[i].getSubnet(),
						lookupAddress.getAddress()[3]};
				
				addresses[i] = InetAddress.getByAddress(this.hostName, address);
			}
		} else {
			out.println("Got an IPV6 address!!");
			testPassed = false;
			}
	} catch (UnknownHostException e) {
		out.print("FAIL");
		testPassed = false;
		}
	
	return testPassed;
	}
		
	
	public int connect(PrintStream out) {
		int sitesPassed = 0;
		out.print(" Connect on port "+ String.format("%-3d", port) +" to");
		for (int i = 0 ; i< sites.length; i++) {
			out.print(String.format(" %-5s",sites[i].name() +":"));
			try {
				Socket socket = new Socket();
				long time = System.currentTimeMillis();
				socket.connect(new InetSocketAddress(addresses[i], port), 2000);
				time = System.currentTimeMillis() - time;
				if (port == 25 || port == 587 || port == 110 || port == 143) {
					if (startTLS(socket)) {
						out.print("PASS (" + time + "ms)");
						sitesPassed++;		
					} else {
						out.print("STARTTLS FAIL");
					}
				} else {
					out.print("PASS (" + time + "ms)");
					sitesPassed++;
				}
				socket.close();
			} catch (IOException e) {
				out.print("FAIL");
			}
		}
		
		return sitesPassed;
		
	}
	
	private boolean startTLS(Socket socket) throws IOException {
		BufferedReader  in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
		String serverMsg = in.readLine();
		if (serverMsg == null)
			return false;

		if (socket.getPort() == 587 || socket.getPort() == 25) { // SMTP
			out.println("STARTTLS");
			out.flush();
			serverMsg = in.readLine();
			if (serverMsg != null && serverMsg.startsWith("220")) {
				return true;
			} else {
				return false;
			}
		} else if (socket.getPort() == 110) { // POP
			out.println("STLS");
			out.flush();
			serverMsg = in.readLine();
			if (serverMsg != null && serverMsg.startsWith("+OK")) {
				return true;
			} else {
				return false;
			}
		} else { // IMAP
			out.println(". STARTTLS");
			out.flush();
			serverMsg = in.readLine();
			if (serverMsg != null && serverMsg.startsWith(". OK")) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	public int test(PrintStream out) {
		out.print(String.format("%-20s", name));
		int numPassed = (lookup(out) ? 1 : 0);
		if (numPassed > 0) {
			numPassed += connect(out);
		}
		out.println();
		return numPassed;
	}
	
		
}
