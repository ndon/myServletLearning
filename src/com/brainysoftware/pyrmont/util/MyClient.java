package com.brainysoftware.pyrmont.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class MyClient {
	
	static Socket server;  
	  
    public static void main(String[] args) throws Exception {  
        server = new Socket(InetAddress.getLocalHost(), 8800);   //服务器的主机名（即ip地址）和端口号  
        //从服务器读
        BufferedReader in = new BufferedReader(new InputStreamReader(server  
                .getInputStream()));  
        //写到服务器
        PrintWriter out = new PrintWriter(server.getOutputStream());
        System.out.println("请输入：");
        //从控制台读
        BufferedReader wt = new BufferedReader(new InputStreamReader(System.in));  
        
        while (true) {  
            String str = wt.readLine();  
            out.println(str);// 写入服务端  
            out.flush();  
            if (str.equals("end")) {  
                break;  
            }  
            System.out.println(in.readLine()); // 读取服务器端，写入客户端，has receiving  
        }  
        server.close();  
    }

}
