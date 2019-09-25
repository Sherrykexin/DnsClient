import java.io.*;
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
}