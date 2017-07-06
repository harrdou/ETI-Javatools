package ca.gc.ssc.eti.ct;

enum Site {
	LEP ("LEP", (byte)240, "205.195.240.150", 443),
	VDA ("VDA", (byte)241, "205.195.241.150", 443),
	APDC ("APDC", (byte)242, "205.195.242.152", 587),
	ICDC ("ICDC", (byte)243, "205.195.243.152", 587);
	
	private final String name;
	private final byte subnet;
	private final String testAddr;
	private final int testPort;
	
	Site (String name, byte subnet, String testAddr, int testPort) {
		this.name = name;
		this.subnet = subnet;
		this.testAddr = testAddr;
		this.testPort = testPort;
		
	}

	public String getName() {
		return name;
	}

	public byte getSubnet() {
		return subnet;
	}

	public String getTestAddr() {
		return testAddr;
	}

	public int getTestPort() {
		return testPort;
	}
	
	@Override
	public String toString() {
		return this.name;
	}

	
}
