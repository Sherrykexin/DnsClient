/*import java.io.*;
import java.net.*;

public class DnsClient {
	
	static int port = 53;
	static int timeout = 5;
	static int max_retries = 3;
	static int query_type = 1;//1 for type A, 2 for MX and 3 for NS
	static String QTYPE = "0001";//0001, 0002 or 000f for A, NS, MX
	static String domainName = "";
	static String auth = "nonauth";//used for answer part, default nonauth
	static int ANCOUNT = 0;
	static int NSCOUNT = 0;
	static int ARCOUNT = 0;
	
    public static void main(String argv[]) throws Exception {

        DatagramSocket clientSocket = new DatagramSocket();

        String ipString = argv[0].substring(1);
        String s[] = ipString.split("\\.");
        byte b[] = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (Integer.parseInt(s[i]));
        }
        
        InetAddress ip = InetAddress.getByAddress(b);

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(byteOutput);
        
        long start = System.currentTimeMillis();
        dataOutput.writeShort(0x1111);
        dataOutput.writeShort(0x0100);
        dataOutput.writeShort(0x0001);
        dataOutput.writeShort(0x0000);
        dataOutput.writeShort(0x0000);
        dataOutput.writeShort(0x0000);

        String[] name = argv[1].split("\\.");

        for (int i = 0; i<name.length; i++) {
            byte[] nameBytes = name[i].getBytes("UTF-8");
            dataOutput.writeByte(nameBytes.length);
            dataOutput.write(nameBytes);
        }

        dataOutput.writeByte(0x00);
        dataOutput.writeShort(0x0001);
        dataOutput.writeShort(0x0001);

        byte[] sendData = byteOutput.toByteArray();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, 53);

        clientSocket.send(sendPacket);
        
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        long end = System.currentTimeMillis();

        long rt = (end-start);

        clientSocket.receive(receivePacket);

        System.out.println("\n\nReceived: " + receivePacket.getLength() + " bytes");

        System.out.println("DnsClient sending request for " + argv[1]);
        System.out.println("Server: " + ipString);
        System.out.println("Request type: ");

        System.out.println("Response received after " + (rt/1000.0) + " seconds (0 retries)");
        System.out.println();

        DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(receiveData));
        dataInput.readShort();
        short x = dataInput.readShort();
        String aa;
        if ((x & (1 << 10)) != 0) {
            aa = "auth";
        } else {
            aa = "nonauth";
        }
        dataInput.readShort();

        System.out.println("***Answer Section (" + String.format("%d", dataInput.readShort()) + " records)***");
        dataInput.readShort();
        short additional = dataInput.readShort();

        int recLen = 0;
        while ((recLen = dataInput.readByte()) > 0) {
            byte[] record = new byte[recLen];

            for (int i = 0; i < recLen; i++) {
                record[i] = dataInput.readByte();
            }

            System.out.println("Record: " + new String(record, "UTF-8"));
        }

        System.out.println("Record Type: " + String.format("%s", dataInput.readShort()));
        System.out.println("Class: " + String.format("%s", dataInput.readShort()));

        dataInput.readShort();
        short type = dataInput.readShort();
        dataInput.readShort();
        int ttl = dataInput.readInt();
        dataInput.readShort();

        if (type == 1) {
            System.out.println("IP \t " + String.format("%s", dataInput.readUnsignedByte()) + "." + String.format("%s", dataInput.readUnsignedByte()) 
            + "." + String.format("%s", dataInput.readUnsignedByte()) + "." + String.format("%s", dataInput.readUnsignedByte()) + " \t " + ttl + " seconds"
            + " \t " + aa);
        }

        System.out.println("***Additional Section (" + additional + " records)***\n");

        System.out.println("\n");
		
		clientSocket.close();
    }
}*/

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

public class DnsClient {
	// flags
	static byte DNSServerIP[] = new byte[4];
	// Default Parameters value
	static int port = 53;
	static int timeout = 5;
	static int max_retries = 3;
	static int query_type = 1;//1 for type A, 2 for MX and 3 for NS
	static String QTYPE = "0001";//0001, 0002 or 000f for A, NS, MX
	static String domainName = "";
	static String auth = "nonauth";//used for answer part, default nonauth
	static int ANCOUNT = 0;
	static int NSCOUNT = 0;
	static int ARCOUNT = 0;
	static String name = "";//name for NS, MX, CNAME response
	static int IDcheck;
	//generate a random query ID to be used for transmission
	public static String generateID() {
		String ID = null;
		int IDNumber = (int) (Math.random() * 65535);
		IDcheck = IDNumber;
		ID = String.format("%4s", Integer.toHexString(IDNumber)).replace(' ', '0');
		//System.out.println("ID:" + ID);
		return ID;
	}

	public static void parseInput(String args[]) throws Exception {
		int i = 0;
		//loop through the parameters before @, update if necessary
		while (args[i].substring(0, 1).equals("-")) {
			if (args[i].equals("-t")) {
				timeout = Integer.parseInt(args[i + 1]);
				i = i + 2;
			}
			//System.out.println("timeout =" + timeout);
			if (args[i].equals("-r")) {
				max_retries = Integer.parseInt(args[i + 1]);
				i = i + 2;
			}
			//System.out.println("max_retries =" + max_retries);
			if (args[i].equals("-p")) {
				port = Integer.parseInt(args[i + 1]);
				i = i + 2;
			}
			//System.out.println("port =" + port);
			if (args[i].equals("-mx")) {
				if (query_type != 1) {
					throw new Exception("Multiple query type, please check input");
				}
				query_type = 2;
				QTYPE = "000f";
				i = i + 1;
			}
			if (args[i].equals("-ns")) {
				if (query_type != 1) {
					throw new Exception("Multiple query type, please check input");
				}
				query_type = 3;
				QTYPE = "0002";
				i = i + 1;
			}

		}
		//check for @
		if (!args[i].substring(0, 1).equals("@")) {
			throw new Exception("Please enter @ServerName correctly and check preceeding parameters");
		}
		//remove the @, and separate the input server address by the dots, so we should get 4 strings of number
		String[] ServerIpStringArray = args[i].replace("@", "").split("\\.");

		// Parse server address
		for (int j = 0; j < 4; j++) {
			try {
				// if more than 255 or less than 0 then throw exception
				if (Integer.parseInt(ServerIpStringArray[j]) > 255 || Integer.parseInt(ServerIpStringArray[j]) < 0) {
					throw new Exception("IP out of range");
				}
				DNSServerIP[j] = (byte) Integer.parseInt(ServerIpStringArray[j]);

			} catch (Exception e) {
				// for length problem, such as 123.123.123
				System.out.println("ERROR:\tWrong server address");
				System.exit(1);
				//throw new Exception("ERROR:\tWrong server address");
			}
		}

		i = i + 1;
		domainName = args[i];
	}
	//converts a String of domain name to a hex String, with values of its corresponding hex values
	public static String qname(String domainName) {
		//first separate by dots
		String[] temp = domainName.split("\\.");
		String hexDomainName = "";

		for (int j = 0; j < temp.length; j++) {
			//put the length in front, i.e. 3www
			hexDomainName += String.format("%2s", Integer.toHexString(temp[j].length())).replace(' ', '0');
			//loop through the rest of the string to convert to hex values
			for (int i = 0; i < temp[j].length(); i++) {

				hexDomainName += String.format("%2x", (int) (temp[j].charAt(i)));
			}
		}
		//System.out.println("hexDomainName= " + hexDomainName);
		return hexDomainName;
	}
	//converts a hex string to a byte array we can send
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		//the byte array would have half the length of the hex string, since 2 hex numbers form a byte
		byte[] data = new byte[len / 2];
		
		for (int i = 0; i < len; i+=2) {
			//first number in an octet is left shifted by 4 bits, allowing space for the second number
			//for example, to convert 0x12, first we have 0b00000001, left shift by 4 is 0b00010000, then add the second number
			//0b0002, the result would be 0b00010002, for one byte
			data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	//checks for empty data, which shouldn't happen
	public static String data(byte[] array, int start){
		String data = RData(array,start);
		try{
			return data.substring(0, data.length() -1);
		} catch (Exception e) {
			return "ERROR:\twrong data";
		}
	}
	//Decode compressed string in a recursive fashion
	public static String RData(byte[] array, int start){
		String result = "";
		//Base case
		if((array[start] & 0xff) == 0x00) return result;
		//Pointer!
		else if((array[start] & 0xff) == 0xc0) return RData(array, (int)array[start+1]);
		else {
			int length = array[start] & 0xff;
			byte[] d = Arrays.copyOfRange(array, start+1, start+1+length);
			String data = new String(d);
			result += data;
			return result +"." + RData(array,start+1+length);
		}
	}
	//Return an integer from part of a byte array
	public static int byteArrayToInt(byte[] array, int start, int end) {
		byte[] num = new byte[4];
		for(int j = 0; j<(end-start)+1; j++) {
			num[3-j] = array[end-j];
		}
		int number=0;
		try{
			number = ByteBuffer.wrap(num).getInt();
		}catch (Exception e){
			System.out.println(e);
		}
		return number;
	}
	//parse and format the IP address in the response and return it as a string
	public static String responseIPString(byte[] RDATA, int start, int end) {
		String result= "";
		for(int i = start; i < end; i++) {
			result += (RDATA[i] & 0xff);
			if (i<end-1) result += ".";
		}
		return result;
	}

	public static void parseResponse(byte[] responseData, int querySize) throws Exception {
		int index = 0;
		//int temp=(((responseData[index])*256) + responseData[index+1]);
		//boolean b = ((temp^0xffff0000) == IDcheck);
		index = 2;
		//first check the AA bit then set the auth string accordingly, 0x85 means authoritative.
		if((responseData[index]&0xff) == 0x85){auth = "auth";}
		index++;
		int RCODE = responseData[index] & 0xff;
		switch(RCODE){
		//case 0x80 :{System.out.println("good");}
		case 0x81 :{throw new Exception("ERROR\t[Format error]");}
		case 0x82 :{throw new Exception("ERROR\t[Server failure]");}
		case 0x83 :{throw new Exception("Domain name NOTFOUND");}
		case 0x84 :{throw new Exception("ERROR\t[Unsupported query type]");}
		case 0x85 :{throw new Exception("ERROR\t[Refused request]");}
		}
		//count the number of answers in the 16 bit ANCOUNT field
		index += 3;
		ANCOUNT = (responseData[index] & 0xff)*256;
		index++;
		ANCOUNT += responseData[index] & 0xff;
		
		//count NSCOUNT, same as ANCOUNT
		index++;
		NSCOUNT = (responseData[index] & 0xff)*256;
		index++;
		NSCOUNT += responseData[index] & 0xff;
		
		//count ARCOUNT, same as ANCOUNT
		index ++;
		ARCOUNT = (responseData[index] & 0xff)*256;
		index++;
		ARCOUNT += responseData[index] & 0xff;
		
		if((ANCOUNT+NSCOUNT+ARCOUNT) == 0){System.out.println("NOTFOUND");}
		int TLL = 0;
		int RDLENGTH =0;
		
		for(int i = 0; i<(ANCOUNT+NSCOUNT+ARCOUNT);i++) {
			if(i == 0){
				System.out.println("***Answer Section (" + ANCOUNT + " records)***");
			}
			if(i == (ANCOUNT + NSCOUNT)){
				System.out.println("***Additional Section (" + ARCOUNT + " records)***");
			}
			
			//obtain seconds to cache
			TLL = byteArrayToInt(responseData,querySize+6,querySize+9);
			//obtain RDLENGTH
			RDLENGTH = byteArrayToInt(responseData,querySize+10,querySize+11);
			
			//handles the resource records with the same three values that QTYPE can take, 1 for A, 2 for NS, f for MX
			//also 5 for CNAME
			int type = (responseData[querySize + 3]&0xff);
			
			switch(type){
			// type A
			case 0x01 : {
				//use the responseIPString() to obtain a normal string of IP 
				System.out.println("IP\t[" + responseIPString(responseData,querySize+12,querySize+16) +"]\t[" + TLL +"]\t[" + auth +"]");
				break;
			}
			//type NS
			case 0x02 : {
				//obtain name of server
				name = data(responseData, querySize+12);
				System.out.println("NS\t["+name+"]\t["+TLL+"]\t["+auth+"]");
				break;
			}
			//type MX
			case 0x0f : {
				//obtain pref
				int pref = byteArrayToInt(responseData,querySize+12,querySize+13);
				//obtain name
				name = data(responseData, querySize+14);
				System.out.println("MX\t["+name+"]\t["+pref+"]\t["+TLL+"]\t["+auth+"]");
				break;
			}
			// type CNAME
			case 0x05 : {
				//obtain name
				name = data(responseData, querySize+12);
				System.out.println("CNAME\t["+name+"]\t["+TLL+"]\t["+auth+"]");
				break;
			}
			default:{
				//for debugging purpose, 
				System.out.println("ERROR:\tUnexpected response record");
				continue;
			}
			}
			querySize += 12 + RDLENGTH;
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		try {

			parseInput(args);
			System.out.println("DnsClient sending request for " + domainName);
			System.out.println("Server:" + (DNSServerIP[0] & 0xff) + "." + (DNSServerIP[1] & 0xff) + "."
					+ (DNSServerIP[2] & 0xff) + "." + (DNSServerIP[3] & 0xff));
			System.out.print("Request type: ");
			switch (query_type) {
			case 1:
				System.out.println("[A]");
				break;
			case 2:
				System.out.println("[MX]");
				break;
			case 3:
				System.out.println("[NS]");
				break;
			}

		} catch (Exception e) {
			System.out.print("ERROR:\tIncorrect input syntax");
			System.exit(1);
		}
		// Create a UDP socket

		DatagramSocket clientSocket = new DatagramSocket();

		// create a buffer for the data to be sent
		byte[] sendData = hexStringToByteArray(
				generateID() + "01000001000000000000" + qname(domainName) + "00" + QTYPE + "0001");
		//buffer for receive, with maximum length of 512
		byte[] receiveData = new byte[512];
		clientSocket.setSoTimeout(timeout * 1000);
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByAddress(DNSServerIP),
				port);
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		//Start timer
		Long sendTime = System.currentTimeMillis();

		int i;

		//Loop until we've reached max number of retries
		for (i = 0; i <= max_retries; ) {
			clientSocket.send(sendPacket);
			
			try {
				clientSocket.receive(receivePacket);
				//check ID
				if(!(receiveData[0] == sendData[0]) && (receiveData[1] == sendData[1])){
					System.out.println("wrong ID, retry");
					i++;
					continue;
				}
				break;
			} catch (Exception e) {
				if (i == max_retries ){
					System.out.println("ERROR:\tMaximum number of retries " + max_retries + " exceeded");
					System.exit(1);
					//throw new Exception(SocketTimeoutException);
				}
				System.out.println("timeout, retry");
				
				i++;
				continue;
			}
		}
		//Stop timer
		Long receiveTime = System.currentTimeMillis();
		double secondsToRespond = ((double) (receiveTime - sendTime)) / 1000.0;
		NumberFormat formatter = new DecimalFormat("#0.0000");
		System.out.println("Response received after " + formatter.format(secondsToRespond) + " seconds ("+i+" retries)");
		
		receiveData = receivePacket.getData();
		parseResponse(receiveData, sendData.length);

		// Close the socket
		clientSocket.close();
	}
}