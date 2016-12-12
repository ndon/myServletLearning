package com.brainysoftware.pyrmont.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MyServer {
	
	public static void main(String[] args) throws IOException {  
        ServerSocket server = new ServerSocket(8800);  
        Socket client = server.accept();  
        //从client读
        BufferedReader in = new BufferedReader(new InputStreamReader(client  
                .getInputStream()));  
        //写到client
        PrintWriter out = new PrintWriter(client.getOutputStream());  
        while (true) {  
            String str = in.readLine();  
            System.out.println(str); // 读取客户端，写入服务器端  
            out.println("has receive....");// 写入客户端  
            out.flush();  
            if (str.equals("end"))  
                break;  
        }  
        client.close();  
    }

}
