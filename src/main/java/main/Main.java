package main;

import client.Connection;

public class Main {
    public static void main(String[] args) {
        if (args.length != 3) System.out.println("args: fileName serverIp serverPort");

        Connection connection = new Connection(args[1],Integer.valueOf(args[2]));
        connection.sendFile(args[0]);

    }
}
