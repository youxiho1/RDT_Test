package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.HashMap;
import java.util.TimerTask;

public class Taho_SendWindow extends SR_SendWindow{
    private int ssthresh;
    private int wrongAckNum;
    private int status;         //status=0代表慢启动，status=1代表拥塞避免
    private HashMap<Integer, Integer> hashMap = new HashMap<>();

    public Taho_SendWindow(Client client) {
        super(client);
        size = 1;
        ssthresh = Integer.MAX_VALUE;
        wrongAckNum = 0;
    }

    @Override
    public void sendPacket(TCP_PACKET packet) {
        System.out.println(packet.getTcpH().getTh_seq());
        //在窗口中初始化这个包的相关数据
        int index = packet.getTcpH().getTh_seq();
        packets[index] = packet;
        isAck[index] = false;
        timers[index] = new UDT_Timer();
        hashMap.put(nextseqnum, index);
//        UDT_RetransTask task = new UDT_RetransTask(client, packet);
        Taho_RetransmitTask task = null;
        try {
            task = new Taho_RetransmitTask(client, packet.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        timers[index].schedule(task, 3000, 3000);

        nextseqnum++;
        packet.getTcpH().setTh_eflag((byte)7);
        client.send(packet);
    }

    @Override
    public void recvPacket(TCP_PACKET packet) {
        int ack = packet.getTcpH().getTh_ack();
        System.out.println("\nTaho_SenderWindow\n接收到了ack包，ack号为" + ack);
        if (ack >= base) {
            System.out.print("size： " + size);
            if (size < ssthresh) {
                if(size * 2 <= 0) {
                    //处理整型溢出现象
                    size = Integer.MAX_VALUE;
                } else {
                    size = Math.min(Integer.MAX_VALUE, size * 2);
                }
            } else {
                if(size + 1 <= 0) {
                    //处理整型溢出现象
                    size = Integer.MAX_VALUE;
                } else {
                    size = Math.min(Integer.MAX_VALUE, size + 1);
                }
            }
            System.out.println(" --> " + size);
        }
        if(ack >= base) {
            int index = ack;
            if(timers[index] != null)
                timers[index].cancel();
            isAck[index] = true;
            if(ack == base) {
                //收到的包是窗口的第一个包，将窗口下沿向前推到一个unAckd seq#
                int i;
                for(i = base; i <= nextseqnum && isAck[i]; i++) {
                    packets[i] = null;
                    isAck[i] = false;
                    if(timers[i] != null) {
                        timers[i].cancel();
                        timers[i] = null;
                    }
                }
                base = Math.min(i, nextseqnum);
                System.out.println("base2 = " + base);
                end = base + size - 1;
            }
        }
        System.out.print("index = " + ack);
        System.out.print(" base = " + base);
        System.out.print(" nextseqnum = " + nextseqnum);
        System.out.println(" end = " + end);
    }

    class Taho_RetransmitTask extends RetransmitTask {
        int number;
        TCP_PACKET packet;
        public Taho_RetransmitTask(Client client, TCP_PACKET packet) {
            super(client, packet);
            number = packet.getTcpH().getTh_seq();
            this.packet = packet;
        }

        @Override
        public void run() {
//            if(number > base + size) {
//                System.out.println("number = " + number);
//                System.out.println("base + size = " + (base+size));
//                //超出部分不做处理
//                if(timers[number] != null) {
//                    timers[number].cancel();
//                    timers[number] = null;
//                }
//                timers[number] = new UDT_Timer();
//                Taho_RetransmitTask task = new Taho_RetransmitTask(client, packet);
//                timers[number].schedule(task, 3000, 3000);
//                return;
//            }
            System.out.println("执行重传，size已置成1");
            ssthresh = Math.max(size / 2, 1);
            size = 1;
            super.run();
            if(timers[number] != null) {
                timers[number].cancel();
                timers[number] = null;
            }
            timers[number] = new UDT_Timer();
            Taho_RetransmitTask task = new Taho_RetransmitTask(client, packet);
            timers[number].schedule(task, 3000, 3000);
        }
    }
}
