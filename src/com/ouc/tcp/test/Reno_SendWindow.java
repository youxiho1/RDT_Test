package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.HashMap;
import java.util.TimerTask;

public class Reno_SendWindow extends SR_SendWindow{
    private int ssthresh;
    private int wrongAckNum;
    private int status;         //status=0代表慢启动，status=1代表拥塞避免, status=2代表快速恢复
    private int tempAdd = 1;
    private HashMap<Integer, Integer> hashMap = new HashMap<>();

    public Reno_SendWindow(Client client) {
        super(client);
        size = 1;
        ssthresh = Integer.MAX_VALUE;
        wrongAckNum = 0;
        status = 0;
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
        boolean flag = false;                       //这是一个延迟标志，如果函数结束时标志为true，则切换状态
        if(status == 2 && isAck[ack]) {
            //快速恢复状态，一个重复的ACK到达
            if(size + tempAdd <= 0) {
                size = Integer.MAX_VALUE/2;
            } else {
                size = Math.min(size+tempAdd, Integer.MAX_VALUE/2);
            }
            if(tempAdd * 2 <= 0) {
                //处理整型溢出问题
                tempAdd = Integer.MAX_VALUE/2;
            } else {
                tempAdd = tempAdd * 2;
            }
            System.out.println("快速恢复状态，一个重复的ACK到达");
        }  else if(status == 2 && !isAck[ack]) {
            //快速恢复状态，一个新的ACK到达
            size = ssthresh;
            flag = true;
            System.out.println("快速恢复状态，一个新的ACK到达，进入拥塞避免状态");
        }
        if(ack > base) {
            wrongAckNum++;
            if(wrongAckNum > 3) {
                if(status == 0) {
                    ssthresh = size / 2;
                    size = ssthresh + 3;
                    status = 2;
                    tempAdd = 1;
                    System.out.println("慢启动/拥塞避免状态执行快速重传，窗口大小已置为" + size + "，已进入快速恢复状态");
                }
                wrongAckNum = 0;
                if(timers[base] != null) {
                    timers[base].cancel();
                    timers[base] = new UDT_Timer();
                    try {
                        Taho_RetransmitTask task = new Taho_RetransmitTask(client, packets[base].clone());
                        timers[base].schedule(task, 3000, 3000);
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    client.send(packets[base].clone());
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (ack >= base) {
            System.out.print("size： " + size);
            if (status == 0 && size < ssthresh) {
                if(size * 2 <= 0) {
                    //处理整型溢出现象
                    size = Integer.MAX_VALUE/2;
                } else {
                    size = Math.min(Integer.MAX_VALUE/2, size * 2);
                }
                if(status == 0 && size >= ssthresh) {
                    status = 1;
                }
            } else if(status == 0){
                if(size + 1 <= 0) {
                    //处理整型溢出现象
                    size = Integer.MAX_VALUE/2;
                } else {
                    size = Math.min(Integer.MAX_VALUE/2, size + 1);
                }
            }
            System.out.println(" --> " + size);
        }
        if(ack >= base) {
            int index = ack;
            if(timers[index] != null) {
                timers[index].cancel();
                timers[index] = null;
            }

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
        if(flag) {
            status = 0;
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
            ssthresh = Math.max(size / 2, 1);
            size = 1;
            if(status == 0) {
                System.out.println("慢启动状态超时, size已置成1, ssthresh = " + ssthresh);
            } else if(status == 2) {
                status = 0;
                System.out.println("快速恢复状态超时, size已置成1, ssthresh = " + ssthresh);
            }
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
