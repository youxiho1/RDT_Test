package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;

public class Window  {
    public Client client;
    public int size = 500000;
    public TCP_PACKET[] packets = new TCP_PACKET[size];
    public volatile int base = 0;
    public volatile int nextseqnum = 0;
    public volatile int end = size - 1;
    public volatile int sequence = 1;
    public boolean[] isAck = new boolean[size];

    public Window(Client client) {
        this.client = client;
    }

    public boolean isFull() {
        return nextseqnum >= end;
    }
}
