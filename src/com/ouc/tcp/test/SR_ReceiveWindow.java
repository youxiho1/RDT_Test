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
        int index = packet.getTcpH().getTh_ack() / 100;
        if(index >= 0) {
            index = index % size;
            isAck[index] = true;
            packets[index] = packet;
            client.send(packet);
            if(index == base % size) {          //收到的包是窗口的第一个包
                int i;
                for(i = base; i <= end && isAck[i % size]; i++) {
                    vector.addElement(packets[i % size]);
                    isAck[i % size] = false;
                }
                base = i;               //移动窗口位置
                end = base + size - 1;
                //sequence = packets[(base-1) % size].getTcpH().getTh_seq();
            }
        }
        return vector;
    }
}
