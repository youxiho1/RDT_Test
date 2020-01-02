/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {

	private TCP_PACKET ackPack;	//回复的ACK报文段
	int sequence=1;				//2.1版本用于记录当前待接收的包序号，注意包序号不完全是
	int count = 0;
	private SR_ReceiveWindow window = new SR_ReceiveWindow(client);

	/*构造函数*/
	public TCP_Receiver() {
		super();	//调用超类构造函数
		super.initTCP_Receiver(this);	//初始化TCP接收端
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		int seqInPack = recvPack.getTcpH().getTh_seq();
		//2.0版本：检查校验码，生成ACK
		//2.1版本，加入对seqInPack的判断（代替书中0和1两个状态）
		//if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum() && seqInPack == sequence) {
		System.out.println("seqInPack = " + seqInPack);
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum() && seqInPack >= window.base && seqInPack < window.base + window.size) {
			//是我期望的序号 && 校验通过
			//生成ACK报文段（设置确认号）
			tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
			//回复ACK报文段
			try {
				Vector<TCP_PACKET> vector = window.recvPacket(recvPack.clone());
				if(vector != null && vector.size() > 0) {
					for (int i = 0; i < vector.size(); i++) {
						dataQueue.add(vector.get(i).getTcpS().getData());
					}


					//交付数据（每20组数据交付一次）
					//if(dataQueue.size() >= 20)			//SR版本修改交付情况
					deliver_data();
				}
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			reply(ackPack);
			System.out.println("ack包序号为" + ackPack.getTcpH().getTh_seq());


			//将接收到的正确有序的数据插入data队列，准备交付
			//dataQueue.add(recvPack.getTcpS().getData());
			//sequence++;
			//2.1版本，调整sequence增长方式
			//sequence = sequence++;
		}
//		else if(seqInPack == sequence){
//			//2.0版本 NAK
////			System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));
////			System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
////			System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
////			tcpH.setTh_ack(-1);
////			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
////			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
//
//			//2.2版本 无NAK，改用序号不足的ack来充当NAK
//			System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));
//			System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
//			System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
//			//tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
//
//			//ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
//			//tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
//			//回复ACK报文段
//			System.out.println("ack包序号为" + ackPack.getTcpH().getTh_seq());
//			reply(ackPack);
//		} else {
//			System.out.println("重复");
//			//seqInPack != sequence，说明该数据报我已经接收过了
//			tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
//			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
//			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
//			//回复ACK报文段
//			System.out.println("ack包序号为" + ackPack.getTcpH().getTh_seq());
//			reply(ackPack);
//		}

		else if(seqInPack < window.base && seqInPack > window.base - window.size) {
			//收到了一个序号小于我的包
			//SR版本：收到了一个窗口以外的包
			System.out.println("该包在窗口以外");
			tcpH.setTh_ack(seqInPack);
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
			//回复ACK报文段
			reply(ackPack);
			//System.out.println("ack包序号为" + ackPack.getTcpH().getTh_seq());
			//将接收到的正确有序的数据插入data队列，准备交付
			//dataQueue.add(recvPack.getTcpS().getData());
		}
		else {
			//GBN版本
			//reply(ackPack);
			//SR版本：do nothing
		}

	}

	@Override
	//交付数据（将数据写入文件）；不需要修改
	public void deliver_data() {
		//检查dataQueue，将数据写入文件
		File fw = new File("recvData.txt");
		BufferedWriter writer;


		try {
			writer = new BufferedWriter(new FileWriter(fw, true));

			//循环检查data队列中是否有新交付数据
			while(!dataQueue.isEmpty()) {
				int[] data = dataQueue.poll();

				if (count == 0 ){
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
					String date = df.format(new Date());// new Date()为获取当前系统时间，也可使用当前时间戳
					writer.write("start: "+date+"\n");

				}

				//将数据写入文件
				for(int i = 0; i < data.length; i++) {
					writer.write(data[i] + "\n");
				}
				count = count + data.length;

				if (count==100000){
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
					String date = df.format(new Date());// new Date()为获取当前系统时间，也可使用当前时间戳
					writer.write("end: "+date+"\n");

				}


				writer.flush();		//清空输出缓存
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	//回复ACK报文段,不需要修改
	public void reply(TCP_PACKET replyPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte)7);	//eFlag=0，信道无错误

		//发送数据报
		client.send(replyPack);
	}


}