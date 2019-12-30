package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Vector;

public class SR_ReceiveWindow extends Window {

    public SR_ReceiveWindow(Client client) {
        super(client);
    }

    public Vector<TCP_PACKET> recvPacket(TCP_PACKET packet) {
        Vector<TCP_PACKET> vector = new Vector<>();
        int seq = packet.getTcpH().getTh_seq();
        int index = seq % size;
        System.out.println("ReceiveWindow信息如下：");
        System.out.print("seq = " + seq);
        System.out.print("index = " + index);
        System.out.print(" base = " + base);
        System.out.print(" nextseqnum = " + nextseqnum);
        System.out.println(" end = " + end);
        if(index >= 0) {
            isAck[index] = true;
            packets[index] = packet;
//            client.send(packet);
            if(seq == base) {          //收到的包是窗口的第一个包
                int i;
                for(i = base; i <= end && isAck[i % size]; i++) {
                    vector.addElement(packets[i % size]);
                    isAck[i % size] = false;
                    packets[i % size] = null;
                }
                base = i;               //移动窗口位置
                end = base + size - 1;
                //sequence = packets[(base-1) % size].getTcpH().getTh_seq();
            }
        }
        return vector;
    }
}
