package com.ouc.tcp.test;

import java.util.zip.CRC32;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {

	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		//计算校验和
		int checkSum = 0;
		TCP_HEADER header = tcpPack.getTcpH();
		int[] data = tcpPack.getTcpS().getData();
		int length = data.length;
		int[] header_info = new int[3];
		header_info[0] = header.getTh_ack();			//seq
		header_info[1] = header.getTh_seq();			//ack
		//header_info[2] = header.getTh_sum();			//sum
		//这里不代入sum进行计算是为了少更改Receiver的已有代码
		int maxValue = 0xffff;
		int modulus = 65536;
		for(int i = 0; i < 2; i++) {
			if(checkSum > maxValue) {
				checkSum = checkSum % modulus + checkSum / modulus;
			}
			checkSum = checkSum + header_info[i];
		}
		for(int i = 0; i < length; i++) {
			if(checkSum > maxValue) {
				checkSum = checkSum % modulus + checkSum / modulus;
			}
			checkSum = checkSum + data[i];
		}
		if(checkSum > maxValue) {
			checkSum = checkSum % modulus + checkSum / modulus;
		}
		checkSum = ~checkSum;
		//System.out.println("checksum=" + checkSum);
		return (short) checkSum;
	}
}
