//package com.ouc.tcp.test;
//
//import com.ouc.tcp.client.Client;
//import com.ouc.tcp.client.UDT_Timer;
//import com.ouc.tcp.message.TCP_PACKET;
//
//import java.util.TimerTask;
//
//public class GBNWindow extends TimerTask {
//    public int size = 10;                                //窗口大小
//    public Client client;		                        //客户端
//    public TCP_PACKET[] array = new TCP_PACKET[size];
//    public volatile int base = 0;                       //滑动窗口最左端
//    public volatile int nextseqnum = 0;                 //当前要发送谁
//    public volatile int end = size - 1;                 //滑动窗口最右端
//    public int sequence = 1;                            //期待收到的序号
//    public UDT_Timer timer;
//
//    public GBNWindow(Client client) {
//        this.client = client;
//    }
//
//    public void addPacket(TCP_PACKET packet) {
//        array[end++] = packet;
//    }
//
//    @Override
//    public void run() {
//        //窗口内所有已发送的内容全部都重新发送一次
//        for(int i = 0; i < nextseqnum; i++) {
//            client.send(array[i]);
//        }
//    }
//
//    public boolean isFull() {
//        return nextseqnum == size - 1;              //待发送指针指向了末尾
//    }
//
//    public void sendPacket(TCP_PACKET packet) {
//        //使用窗口来发送一个包
//        int index = nextseqnum % size;
//        array[index] = packet;
//        timer = new UDT_Timer();
//        RetransmitTask task = new RetransmitTask();
//        timer.schedule(task, 3000, 3000);
//        nextseqnum++;
//        client.send(packet);
//    }
//
//    public void ackPacket(TCP_PACKET packet) {
//        int seq = packet.getTcpH().getTh_ack();
//        if(seq == sequence) {
//            int index = (seq/100) % size;
//            if(array [index]!= null) {
//                timers[index].cancel();
//                begin++;
//                end++;
//                sequence += 100;
//            }
//        }
//    }
//
//    class RetransmitTask extends TimerTask {
//        public RetransmitTask() {
//            super();
//        }
//
//        //执行重传
//        @Override
//        public void run() {
//            for (int i = base; i < nextseqnum; i++) {
//                if(array[i % size]!=null) {
//                    try {
//                        timer = new UDT_Timer();
//                        client.send(array[i % size].clone());                   //copy
//                    } catch (CloneNotSupportedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//    }
//
//}
