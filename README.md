---
typora-copy-images-to: images
---

# 计算机网络大作业报告

**学号：  姓名：    专业：  年级：**

1. 结合代码和LOG文件分析针对每个项目举例说明解决效果。（1-10分）

2. 未完全完成的项目，说明完成中遇到的关键困难，以及可能的解决方式。（2分）

3. 说明在实验过程中采用迭代开发的优点或问题。(优点或问题合理：1分)
4. 总结完成大作业过程中已经解决的主要问题和自己采取的相应解决方法(1分)

5. 对于实验系统提出问题或建议(1分)

## 1. 结合代码和LOG文件分析针对每个项目举例说明解决效果。

### RDT1.0

**对应Log日志：Log 1.0.txt，接收文件recvData 1.0.txt**

RDT1.0版本是在可靠信道上进行可靠的数据传输，因此没有过多的内容需要说明，发送方Log日志如下：

![1578026599124](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578026599124.png)

接收方Log日志如下：

![1578026769915](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578026769915.png)

从发送方和接收方的发送数据报的数量我们就可以看出信道是没有出任何错的，双方也正常完成了全部内容传输

### RDT2.0

**对应Log日志：Log 2.0.txt，接收文件recvData 2.0.txt**

RDT2.0版本是在可能出现位错的信道上进行传输，只需要在1.0的基础上做出如下几点更改即可：

①添加校验和Checksum的计算，代码如下：

```java
package com.ouc.tcp.test;

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
```

这里我的校验和的计算方式是仿照了UDP的校验和计算方式，但是老师提供的代码中的Receiver类中的校验和的判断是这么写的：

```java
if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum())
```

如果严格按照UDP计算校验和的方法，上述if语句的左边就会计算出来0，而if语句的右边给出的值应该不是0，那么这个if语句就不成立了，而应该改为：

```java
if(CheckSum.computeChkSum(recvPack) == 0)
```

为了少更改Receiver类中的已有代码，这里我在计算校验和的时候没有把sum代入进来，只计算了seq，ack，以及TCP数据字段

②在Sender的recv函数中加入对于ACK包的ack字段的检测：如果检测到NACK，重发，代码如下：

```java
if(recvPack.getTcpH().getTh_ack() == -1) {			//2.0版本检测NACK
	udt_send(tcpPack);
	return;
}
```

③调整Receiver的代码，在检测到corrupt之后返回NACK，代码如下：

```java
if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			//校验通过，这里代码省略了
} else {
			//校验未通过
			System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));
			System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
			System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
			tcpH.setTh_ack(-1);
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
}
```

运行程序，得到发送方Log日志如下：

![1578027749098](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578027749098.png)

由于2.0版本的假设，我们可以知道只有发送方会出现错误，接收方不会出现错误，因此发送方的eFlag设置成1，接受方的eFlag设置成0

在上图中，我们可以看到，发送方共犯了13个错误，因此有13个包需要重发，共计1013个包，数字是对的

在发送方的日志中我们也可以实际地看到这种犯错误并重发来修正的过程，下面以Log日志中的两处作为例子：

![1578027869018](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578027869018.png)

![1578027907862](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578027907862.png)

同时，我们可以去接收方查看一下接收方对应处的日志，来检查接收方的ACK/NACK机制是否正常运行了：

![1578027982371](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578027982371.png)

![1578027999488](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578027999488.png)

可以看到，我们的接收方在6001的正常ack之前，以及24601的正常ack之前，都先给发送方回了一个NACK包，因此我们可以得出发送方与接收方都在正常工作的结论。

### RDT2.1

**对应Log日志：Log 2.1.txt，接收文件：recvData 2.1.txt，控制台日志：consoleLog 2.1.txt**

①RDT2.1是在RDT2.0的基础上解决ack/nack包会出错的问题，我们在发送方的recv()函数的代码中做如下更改：

```java
if(CheckSum.computeChkSum(recvPack) != recvPack.getTcpH().getTh_sum()) {		//2.1版本检测corrupt
			System.out.println("corrupt");
			udt_send(tcpPack);														
			return;
}
```

②将Receiver中的rdt_recv()函数修改如下：

```java
int seqInPack = recvPack.getTcpH().getTh_seq();
System.out.println("seqInPack = " + seqInPack);
//2.0版本：检查校验码，生成ACK
//2.1版本，加入对seqInPack的判断（使用序号判断来代替书中0和1两个状态）
if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum() && seqInPack == sequence) {
		//校验通过，并且是我期待的包
    	//代码省略
} else if(seqInPack == sequence){
        //2.0版本 NAK
        System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));
        System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
        System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
        tcpH.setTh_ack(-1);
        ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
        tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
        //回复ACK报文段
        System.out.println("ack包序号为" + ackPack.getTcpH().getTh_seq());
        reply(ackPack);
} else {
        //2.0版本 重复
        System.out.println("重复");
        //seqInPack != sequence，说明该数据报我已经接收过了
        tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
        ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
        tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
        //回复ACK报文段
        System.out.println("ack包序号为" + ackPack.getTcpH().getTh_seq());
        reply(ackPack);
}
```

由于2.1版本的假设，发送方和接收方都有可能出现错误，因此双方的eFlag都应该改成1，运行程序，得到发送方日志如下：

![1578028715961](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578028715961.png)

接收方的日志如下：

![1578028737434](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578028737434.png)

我们可以从这个Log的数据中看出来：发送方犯了13个错误，因此这13个错误都需要重传；接收方犯了16个错误，对于这16个错误的ack包，发送方不知道接收方是否ack了，因此也需要重传，所以发送方共计发送了1000+13+16=1029个数据包

发送方错误举例（上图为发送方日志，下图为接收方日志）：

![1578028852064](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578028852064.png)

![1578028863044](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578028863044.png)

可以看到，发送方犯了错，于是接收方回了NACK，发送方进行重传，这个重传的包被正常ack

接收方错误举例（上图为发送方日志，下图为接收方日志）：

![1578028972586](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578028972586.png)

![1578028941584](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578028941584.png)

可以看到，发送方没有犯错，但是包也没有正常ack，原因是接收方的ack出现了错误，因此发送方重传了该包，并正常地收到ack了

由此，我们可以得出发送方与接收方都在正常工作的结论

### RDT2.2

**对应Log日志：Log 2.2.txt，对应接收文件：recvData 2.2.txt，对应控制台日志：consoleLog 2.2.txt**

RDT2.2版本与RDT2.1版本的功能是相同的，唯一区别只是不再使用ack/nack的确认方式，而是统一使用ack，如果接收方检测到包的corrupt，那么返回一个过期的ack即可，这里我还是使用序号的方式来进行检测，即：

如果接收方接收到了一个正常的包，就正常返回这个包的序号作为ack

如果接收方接收到了一个corrupt的包，或者一个过期的包，就返回上一个包的序号作为ack

（该算法的合理性论证如下：正常的发送与接收就不说明了；说明一下接收方收到一个过期的包的情况：由于现在是停止等待协议，因此如果接收方接收到了一个过期的包，它只可能是上一个包，因此我们应该返回上一个包的序号作为ack来告诉发送方我们正常接收了这个包，虽然接收方实际上是不需要这个包的）

对代码更改如下：

①更改Sender的recv()函数的最开始对包的检测部分如下：

```java
//注：前面的代码中要将2.0版本的检测NACK隐去

if(CheckSum.computeChkSum(recvPack) != recvPack.getTcpH().getTh_sum()) {		
		//2.1版本检测corrupt
		System.out.println("corrupt");
		udt_send(tcpPack);													
		return;
}
if(recvPack.getTcpH().getTh_seq() < sequence) {													//2.2版本，无NAK
        System.out.println("ack报文编号" + recvPack.getTcpH().getTh_seq() + "已重复收到");
        System.out.println("想要的报文编号是" + sequence);
        //该ack报文我已经收到过了
        udt_send(tcpPack);
        return;
}

//注：后面的代码中有 接收到一个正常包之后更新sequence的值的功能
```

②更改Receiver的rdt_recv()函数中的校验和不正确但包编号是对的的情况的代码：

```java
	else if(seqInPack == sequence){
			//2.0版本 NAK
//			System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));
//			System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
//			System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
//			tcpH.setTh_ack(-1);
//			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
//			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));

			//2.2版本 无NAK，改用序号不足的ack来充当NAK
			System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));
			System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
			System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
			//回复ACK报文段
			System.out.println("ack包序号为" + ackPack.getTcpH().getTh_seq());
			reply(ackPack);
		}
```

运行代码结果可见Log 2.2.txt，由于功能上与RDT2.1完全一致，这里不再赘述

### RDT3.0

**对应Log日志：Log 3.0 -1.txt、Log 3.0 -2.txt，接收文件：recvData 3.0 -1.txt、recvData 3.0 -2.txt**

**注：后缀带1的是发送方会错会丢包，接收方只会错；后缀带2的是发送方与接收方都是会错会丢包**

RDT3.0的最大进步是可以处理包的Loss了，从2.2上到3.0版本只需要更改发送方代码即可，发送方的状态机如下：

![1578030154399](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578030154399.png)

我们对照上图来修改代码（其实是2.2上到3.0非常简单，所以我现在已经记不全3.0都改了什么了）

①首先在Sender类中加入一个私有变量UDT_Timer:

```java
private UDT_Timer timer;	//3.0版本，计时器
```

②在发送方的rdt_Send()函数中加入如下代码：

```java
		//用于3.0版本：设置计时器和超时重传任务
		timer = new UDT_Timer();
		UDT_RetransTask reTrans = new UDT_RetransTask(client, tcpPack);

		//每隔3秒执行重传，直到收到ACK
		timer.schedule(reTrans, 3000, 3000);
```

③在发送方的waitACK()函数中加入如下代码：

![1578030572143](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578030572143.png)

④这里我是严格按照状态机来写的，因此我去除了发送方收到corrupt的ack包以及序号不对的ack包之后的重发，相当于是不管发生什么，都等到超时事件被触发的时候才重发

将发送方和接收方的eFlag都调整成4，运行代码（每运行一次3.0版本都要经历一次漫长的等待，太太太太太慢了）

以下日志分析我采用发送方和接收方都会错会丢包的日志2来进行分析：

发送方日志如下：

![1578031202244](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578031202244.png)

接收方日志如下：

![1578031177600](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578031177600.png)

从整体来看，可以得到1015=1000+12+3，1006=1000+6的正确结论，接下来我们再从细节上看一下我们的系统是否在正常工作：

1. 发送方Wrong：

   ![1578031366470](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578031366470.png)

2. 发送方Loss：

   ![1578031340542](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578031340542.png)

3. 接收方Wrong（上为发送方日志，下为接收方日志）：

   ![1578031501145](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578031501145.png)

   ![1578031476487](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578031476487.png)

4. 接收方Loss（上为发送方日志，下为接收方日志）：

   ![1578031437067](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578031437067.png)

   ![1578031420272](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578031420272.png)

综上，我们可以看出我们的RDT3.0正常运行了（但是太太太太太慢了），不过令人高兴的是，这是我们最后一次使用停止等待协议了，接下来我们就全面迈进流水线协议时代了

### 选择响应协议

**对应Log日志：Log SR.txt，接收文件：recvData SR.txt**

![1578031691888](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578031691888.png)

选择响应协议是一个变化比较大的版本，工作量也非常多，在我的github记录中，这也是第一次推了两个子版本的协议（第一个版本我的发送方采用的是选择响应协议，接收方采用的是Go-Back-N协议，其结果就是……跑一次需要大概10分钟QAQ；第二个版本是双方采用选择响应协议，效率一下子就上去了）

主要工作如下：

①构建所有窗口的父类：Window类（窗口大小设的15）：

```java
package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;

public class Window  {
    public Client client;
    public int size = 15;
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
        return nextseqnum == end;
    }
}

```

注：为什么要加个volatile呢？这是痛苦地debug并且各种百度了一天之后的成果（心痛），不加volatile会出现各种各样的奇奇怪怪的问题

②构建接收窗口：

```java
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
            }
        }
        return vector;
    }
}

```

③构建发送窗口：

```java
package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;

public class SR_SendWindow extends Window{
    public UDT_Timer[] timers = new UDT_Timer[size];

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
        int ack = packet.getTcpH().getTh_ack();             
        //System.out.println("接收到了ack包，ack号为" + ack);
        if(ack >= base && ack <= base + size) {
            int index = ack % size;
            if(timers[index] != null)
                timers[index].cancel();
            isAck[index] = true;
            System.out.print("index = " + index);
            System.out.print(" base = " + base);
            System.out.print(" nextseqnum = " + nextseqnum);
            System.out.println(" end = " + end);
            if(ack == base) {
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
            }
        }
    }

}
```

④将Sender中的工作更改成为交给SendWindow来做

```java
public void rdt_send(int dataIndex, int[] appData) {
		//生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
		tcpH = new TCP_HEADER();
		tcpS = new TCP_SEGMENT();
		tcpH.setTh_seq(dataIndex);//包序号设置为字节流号：
		tcpS.setData(appData);
		tcpH.setTh_sum((short)0);						//需要初始化校验和以进行计算
		tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
		tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);
		while(window.isFull());
		TCP_PACKET packet = new TCP_PACKET(tcpH, tcpS, destinAddr);
		try {
			window.sendPacket(packet.clone());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
}
```

```java
public void recv(TCP_PACKET recvPack) {
		if(CheckSum.computeChkSum(recvPack) != recvPack.getTcpH().getTh_sum()) {					//2.1版本检测corrupt并作出处理
			System.out.println("corrupt");
			//udt_send(tcpPack);																	//GBN版本 corrupt不需处理
			return;
		}
		window.recvPacket(recvPack);								//使用窗口来处理ack
		System.out.println("Receive ACK Number： "+ recvPack.getTcpH().getTh_ack());
		ackQueue.add(recvPack.getTcpH().getTh_ack());
		System.out.println();   
}
```

⑤将Receiver中的回复ack包以外的工作交给ReceiverWindow来完成

```java
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
		}
		else if(seqInPack < window.base && seqInPack > window.base - window.size) {
			//收到了一个序号小于我的包
			//SR版本：收到了一个窗口以外的包
			System.out.println("该包在窗口以外");
			tcpH.setTh_ack(seqInPack);
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
			//回复ACK报文段
			reply(ackPack);
		}
		else {
			//GBN版本
			//reply(ackPack);
			//SR版本：do nothing
		}

	}
```

⑥注：在实现GBN-SR版本升级到SR版本的过程中，我把我的系统的包的序号体系修改了一下，由原来的1,101,201,301,401……改成了0,1,2,3,4……，修改之后大幅降低了思考难度与编码难度（不然在维护窗口的时候要时刻想清楚要不要把包的序号整除一个100）

运行代码，对日志进行分析：

![1578033172891](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578033172891.png)

由上面这一段发送方日志我们可以看出来我们现在确实是流水线协议，而不是停止等待协议（19号的重发与19的第一次发并不挨着）

以下两张图片，第一张是发送方日志，第二张是接收方日志

![1578033360036](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578033360036.png)

![1578033538781](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578033538781.png)

由这一段发送方日志我们可以看出来发送方窗口的大小限制了发送方的窗口继续往前推进（窗口满了，所以新包不能再发送，只能等着旧包超时重传）

同时，我们也可以看出我们的重传是谁超时重传谁，而不是像GBN版本一样整个窗口全都重传

我们还可以从这里的接收方日志中看出虽然184号包出现了问题，但是没有影响接收方对185 186 187等后续的包的接收，这也说明了我们的SR版本的正确性

### 拥塞控制Taho

**对应Log日志：Log Taho2.txt，接收文件：recvData Taho2.txt，控制台日志：consoleLog Taho2.txt**

**注：Log Taho.txt、recvData Taho.txt、consoleLog Taho.txt所对应的Taho版本存在潜在的整型溢出问题，因此不是Taho的最终版本**

![1578034388339](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578034388339.png)

**以下内容按照Taho Fixed版本进行描述：**

Taho版本的有限状态机（来自《计算机网络教程：自顶向下方法》）

![1578033872454](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578033872454.png)

Taho版本要解决的一个最重大的问题就是要改变发送方窗口的大小，接收方不用做什么改变

①我们对发送方的窗口做出如下改变：

```java
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
                    size = Integer.MAX_VALUE/2;
                } else {
                    size = Math.min(Integer.MAX_VALUE/2, size * 2);
                }
            } else {
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

```

注：该类继承自上一个SR版本的发送窗口

注2：这一版本进入拥塞避免的条件只有超时这一条件

②修改Window类的isFull方法，使其可以同时应用于旧版本和Taho及以上版本

```java
public boolean isFull() {
        return nextseqnum >= end;
}
```

运行程序，观察发送方日志：

![1578034844204](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578034844204.png)

可以看到虽然210号出了问题，但是一直没有重传（窗口没满，并且计时器没到）

210号重发的时候已经是432号发完了，这时会引起一次超时重传，因此窗口大小会骤降为1

![1578034931977](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578034931977.png)

对应的命令行日志如下：

![1578035103427](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578035103427.png)

这里size变成了1，因此窗口会被判定成满的，于是新的包发不了，只能等待旧包重发，于是就有了以下的现象：

![1578035179443](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578035179443.png)

由于窗口太小，因此只能等到把前面的未ack的包全都重发了并且ack了，才有可能发新的包

类似的例子还有这里：

![1578035488780](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578035488780.png)

504的重发导致窗口缩减成尺寸为1，因此只能等到520的重发完成才能继续往前推进

同时，这两部分的日志联合起来，我们也可以得知在这200个包的发送过程中，我们的窗口又再次慢慢变大了

### 拥塞控制Reno

**对应Log日志：Log Reno.txt，接收文件：recvData Reno.txt，控制台日志：consoleLog Reno.txt**

Reno版本的有限状态机（来自《计算机网络教程：自顶向下方法》）

![1578033938981](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578033938981.png)

从Taho版本上到Reno版本吗，我做了这么几件事情：

①在发送方加入了冗余ack的判断，当收到冗余ack的次数达到3次的时候，执行快速重传

②加入了快速恢复阶段

③将3次冗余ack也变成了切换状态的条件之一

④将窗口尺寸变化改成了Reno版本的形式(/2 + 3)

这一个版本更改过多（这一个版本也是让我在git上面上传了多个子版本的一个版本，工作量着实不小），代码如下：

```java
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

```

将发送方和接收方的eFlag改成7，运行代码，分析日志文件中的错误、延迟、丢失三种情况：

**案例1：**

发送方的包延迟了，于是在4次冗余ack之后，发送方进行了快速重传

![1578036743533](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578036743533.png)

![1578037369170](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578037369170.png)

在执行快速重传之后，进入了快速恢复状态

![1578037505213](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578037505213.png)

在收到了一个新的ACK包之后，进入拥塞避免状态，size直接变成ssthresh的值

由于此时size已经>=ssthresh了（拥塞避免状态），因此size在每次收到ack的时候会+1

![1578037617573](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578037617573.png)

![1578037647635](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578037647635.png)

↑出现这种情况的原因应该是线程的并发性，由于我没有把我的函数封装成原子操作，我的Taho_SenderWindow的输出与Receiver的输出处于并发，在我的输出未完全输出之前，系统调用了另外一个线程令其输出内容，不过无伤大雅，我们可以看到size从16438变成了16439（是增加了1）

**案例2：**

发送方的包丢失了，于是在4次冗余ack之后，发送方进行了快速重传

![1578036791055](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578036791055.png)

**案例3：**

发送方的包出错了，于是在4次冗余ack之后，发送方进行了快速重传

![1578036855458](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578036855458.png)

**案例4：**

接收方的包丢失了，于是在4次冗余ack之后，发送方进行了快速重传（上方为接收方日志，下方为发送方日志）

![1578037911655](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578037911655.png)

![1578037957713](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578037957713.png)

**注：快速重传机制基本上保证了根本不会超时（笑），只要不是接收方的所有包都delay了，基本上就不会出现发送方重传的问题（毕竟我这是在选择响应协议的基础上做的），纵览整个日志，也确实没看到有超时重发的例子……**

## 2. 说明在实验过程中采用迭代开发的优点或问题。

这次实验让我对迭代式开发有了非常深刻的体会，我觉得迭代开发优缺点都相对比较明显

### **我认为迭代开发主要有以下优点：**

①每一个迭代版本的目标非常明确，这与连续开发是不同的，我清楚我做到什么地步，要实现什么样的效果就算是完成了这样的一个迭代版本，也相当于是对于自己的项目进度有一个比较明确的进度条（有一个进度条能让我对自己的项目有一个更好的把控）。软件工程中也学到过，直接估计一个项目的总工作量是很难的，但是如果我们采用迭代开发的话，目标就相对明确，工作量也就随之相对明确了

②完成每一个迭代版本我可以向github上推一个版本，这样我在做下一个迭代版本的开发时，一旦出现一个非常严重的问题，我可以直接回退回上一个大的迭代版本，重新来过；如果不采用迭代开发，就只能凭借推git的时候提交的简短的summary和description来勉强记忆这个git commit已经完成到什么程度了，这样一旦需要回滚，需要把代码整个过一遍来确定我做了哪些内容没做哪些内容（这些内容很难在提交commit的时候精确描述）

③迭代式开发的焦点与重点非常明确，不至于出现开发大型项目的时候容易出现的项目太大下不去手的问题

④针对于这个项目而言，这样的迭代式开发能够让我真切地体会到每个版本的优缺点（3.0版本和GBN版本让我印象非常深刻），并且在实验结束后的现在，可以说我对于每一个版本都非常非常熟悉了，如果直接开发最后的版本，那么这些中间过程我是不能了解到的，自然也不会对整个tcp版本的发展历史有所了解有所掌握

### **我认为迭代开发主要有以下缺点：**

①这个项目中从要求上来说共分为1.0 2.0 2.1 2.2 3.0 GBN/SR 拥塞控制这么几个大的迭代版本，但是实际上我在做的时候大的迭代版本数远远不止于此：

我实际在做的时候迭代版本是以下的：

Initial Commit-> RDT 1.0 -> RDT 2.0 -> RDT 2.1 -> RDT 2.2 -> RDT SR-GBN（发送方SR，接收方GBN） -> RDT SR -> RDT Taho -> RDT Taho Fixed -> RDT FR（快速重传） -> RDT Reno

由于我在开发的时候没有把之前的代码删掉，而是把他们注释掉了，并且我在编程的时候会写明这个代码是哪一个版本进行添加/修改的，因此我可以比较明确地看到我哪个版本做了什么（除非有多个版本连续修改同一块代码），但是现在全部写完了再回头看，其实最开始的代码（或者可以说3.0版本之前的代码），没剩多少了……我觉得在某种意义上来说，这也算是增添了比较多的工作量

②这个项目相当于是老师为我们规定了迭代的版本，如果是其他的项目（如软件工程项目），由开发者自行规定迭代版本，很可能出现迭代版本安排设置不合理的情况，从而极大地影响开发效率，我猜测：如果迭代版本安排过小，就失去了它的意义；如果迭代版本安排过大，就与不采用迭代开发没有本质区别了。因此，迭代开发会受到制定迭代计划的好坏的影响

## 3. 总结完成大作业过程中已经解决的主要问题和自己采取的相应解决方法

### ① recvData输出不完整（SR-GBN版本升到SR版本过程中出现）

![1578039550364](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578039550364.png)

这个recvData文件我特意存储了下来，可以看到其他的文件都是694KB，只有这个文件是680KB，我百思不得其解==（多线程的程序很难直接调试）

最后把接收方的代码分成了非常多的小的功能模块，然后每一个功能模块都用System.out.println来进行输出日志，来进行详细地查看，仍然没有找到问题所在

输出日志仍然没有解决我的问题，于是我在纸上手动执行了一次代码，大概执行了两趟，我就发现了问题所在，问题出在一段我从来没有修改过，甚至可以说从来没有注意过的代码上：

```java
//交付数据（每20组数据交付一次）
if(dataQueue.size() >= 20)
	deliver_data();
```

这一个简单的if语句与我的SR版本写的缓冲区的交付数据不搭配，因此就会造成整个数据的最后一小段还在dataQueue中放着，不够20，因此没有交付，所以接收文件中少一段

心得体会：多线程真的非常难以debug，并且用输出法来检查问题也非常麻烦（命令行的日志过多，找我自己的日志也很麻烦），有的时候手动执行以下代码是不错的选择；再有就是，细致地了解自己的代码，对他们要有完全的掌控，不然不定什么时候就会出现错误

### ② 对于后续迭代版本出现的窗口有点难以下手（3.0版本升到SR版本过程中出现）

我先仔细整理了一下后续版本要实现的功能或方法，以及需要的成员变量，然后打了一个UML图的草稿，类似下图：

![1578040123304](C:\Users\RiddleLi\Documents\GitHub\RDT_Test\images\1578040123304.png)

把继承关系理顺了之后，把函数名以及相关的注释都标在了上面，接下来将每一个函数需要做什么明确地记录在纸上，然后每一个迭代版本按照自己的草稿逐步填入进去就可以了

### ③最开始的序号系统是0,101,201……给我的思考和编码都带来了极大的麻烦（3.0版本升到GBN版本过程中出现）

在构建窗口的时候，必须时刻小心，这里是不是需要把序号除100，那里是不是不应该把序号除100，带来了很多根本毫无意义的思考与提防，但是一直没狠下心来把所有的代码中的序号修改一遍，于是就只能在已有的基础上继续啰里啰嗦地往后写，然后越写越难写，越写越难写（就好比在一个三层的危房上再建个同样是危房的第四层），最后下定决心把所有的编号都改成了0,1,2,3……才算彻底摆脱这一苦恼

心得体会：长痛不如短痛……有些基础性的问题就应该尽早全力解决，如果我在3.0版本之前就把这个问题改掉了，我的SR版本也不会卡这么多天都上不去

## 4. 对于实验系统提出问题或建议

①如果实验系统使用最新版的Java也能跑起来。感觉会更方便一点（不过现在这个实验系统Java8是可以运行的，好评！）

②实验系统中命令行的日志过多（最早的部分日志会爆出范围），建议将命令行的日志直接写到文件中；或者对外提供一个写自定义日志的接口？感觉会更方便一点