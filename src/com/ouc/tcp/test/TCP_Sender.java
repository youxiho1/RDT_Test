/***************************2.1: ACK/NACK
**************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Sender extends TCP_Sender_ADT {
	int sequence = 0;			//2.2版本，数据报序号
	private TCP_PACKET tcpPack;	//待发送的TCP数据报
	private UDT_Timer timer;	//3.0版本，计时器

	/*构造函数*/
	public TCP_Sender() {
		super();	//调用超类构造函数
		super.initTCP_Sender(this);		//初始化TCP发送端
	}
	
	@Override
	//可靠发送（应用层调用）：封装应用层数据，产生TCP数据报；需要修改
	public void rdt_send(int dataIndex, int[] appData) {
				
		//生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
		tcpH.setTh_seq(dataIndex * appData.length + 1);//包序号设置为字节流号：
		tcpS.setData(appData);
		tcpH.setTh_sum((short)0);						//需要初始化校验和以进行计算
		tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
		tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);

		//发送TCP数据报
		udt_send(tcpPack);

		//用于3.0版本：设置计时器和超时重传任务
		timer = new UDT_Timer();
		UDT_RetransTask reTrans = new UDT_RetransTask(client, tcpPack);

		//每隔3秒执行重传，直到收到ACK
		timer.schedule(reTrans, 3000, 3000);
		
		//等待ACK报文
		waitACK();
		
	}
	
	@Override
	//不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送；仅需修改错误标志
	public void udt_send(TCP_PACKET stcpPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte)4);
		//System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());				
		//发送数据报
		client.send(stcpPack);
	}
	
	@Override
	//需要修改
	public void waitACK() {
		//循环检查ackQueue
		//循环检查确认号对列中是否有新收到的ACK
		while(true) {
			if(!ackQueue.isEmpty()){
				int currentAck=ackQueue.poll();
				System.out.println("CurrentAck: "+currentAck);
				if  (currentAck == tcpPack.getTcpH().getTh_seq()){
					System.out.println("Clear: "+tcpPack.getTcpH().getTh_seq());
					//用于3.0：
					timer.cancel();
					break;
				}else{
					System.out.println("Retransmit: "+tcpPack.getTcpH().getTh_seq());
					udt_send(tcpPack);
					//break;
				}
			}
		}
	}

	@Override
	//接收到ACK报文：检查校验和，将确认号插入ack队列;NACK的确认号为－1；3.0版本不需要修改
	public void recv(TCP_PACKET recvPack) {
//		if(recvPack.getTcpH().getTh_ack() == -1) {			//2.0版本检测NACK
//			udt_send(tcpPack);
//			return;
//		}
		if(CheckSum.computeChkSum(recvPack) != recvPack.getTcpH().getTh_sum()) {		//2.1版本检测corrupt
			System.out.println("corrupt");
			udt_send(tcpPack);
			return;
		}
		if(recvPack.getTcpH().getTh_seq() < sequence) {									//2.2版本，无NAK
			System.out.println("ack报文编号" + recvPack.getTcpH().getTh_seq() + "已重复收到");
			System.out.println("想要的报文编号是" + sequence);
			//该ack报文我已经收到过了
			udt_send(tcpPack);
			return;
		}

		//2.2版本，发送完成之后更新sequence的值
		System.out.print("sequence: " + sequence + "-> ");
		sequence = tcpPack.getTcpH().getTh_seq();
		System.out.println(sequence);
		System.out.println("Receive ACK Number： "+ recvPack.getTcpH().getTh_ack());
		ackQueue.add(recvPack.getTcpH().getTh_ack());
		System.out.println();

	   
	}
	
}
