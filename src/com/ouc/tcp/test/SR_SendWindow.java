package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;

public class SR_SendWindow extends Window{
    private UDT_Timer[] timers = new UDT_Timer[size];

    public SR_SendWindow(Client client) {
        super(client);
    }

    public void sendPacket(TCP_PACKET packet) {
        System.out.println(packet.getTcpH().getTh_seq());
        //在窗口中初始化这个包的相关数据
        int index = nextseqnum % size;
        packets[index] = packet;
        isAck[index] = false;
        timers[index] = new UDT_Timer();
        UDT_RetransTask task = new UDT_RetransTask(client, packet);
        timers[index].schedule(task, 3000, 3000);

        nextseqnum++;
        packet.getTcpH().setTh_eflag((byte)4);
        client.send(packet);

    }

    public void recvPacket(TCP_PACKET packet) {
        int ack = packet.getTcpH().getTh_ack();             //ack相当于是序号
        System.out.println("ackkkk=" + ack);
        if(ack / 100 >= base && ack / 100 <= base + size) {
            int index = ack / 100 % size;
            if(timers[index] != null)
                timers[index].cancel();
            isAck[index] = true;
            System.out.print("index = " + index);
            System.out.print(" base = " + base);
            System.out.print(" nextseqnum = " + nextseqnum);
            System.out.println(" end = " + end);
            if(ack / 100 == base) {
                //收到的包是窗口的第一个包，将窗口下沿向前推到一个unAckd seq#
                int i;
                for(i = base; i <= nextseqnum && isAck[i % size]; i++) {
                    packets[i % size] = null;
                    isAck[i % size] = false;
                    if(timers[i % size] != null) {
                        timers[i % size].cancel();
                        timers[i % size] = null;
                    }
                }
                base = Math.min(i, nextseqnum);
                System.out.println("base2 = " + base);
                end = base + size - 1;
                sequence = base * 100 + 1;
            }
        }
    }

    class RetransmitTask extends TimerTask {

        public RetransmitTask() {
            super();
        }

        @Override
        public void run() {
            //执行重传
            for (int i = base; i < nextseqnum; i++) {
                int index = i % size;
                if(packets[index]!=null && !isAck[index]) {
                    try {
                        timers[index].cancel();
						timers[index] = new UDT_Timer();
						RetransmitTask task = new RetransmitTask();
						timers[index].schedule(task, 3000, 3000);
                        client.send(packets[index].clone());
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
