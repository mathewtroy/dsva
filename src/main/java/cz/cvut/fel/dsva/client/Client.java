package cz.cvut.fel.dsva.client;

import cz.cvut.fel.dsva.compute.MathServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    public static void main(String[] args) {
        try {
            // lookup for remote object with specified name in RMI registry
            MathServer mth;
//			String name = "MathD";
            String name = "MathD2";
            Registry registry = LocateRegistry.getRegistry("localhost", 2010);
            mth = (MathServer) registry.lookup(name);
            // reading input data from commandline
            int a = Integer.parseInt(args[0]);
            int b = Integer.parseInt(args[1]);
            // Show the results
            System.out.println("Client: " + a + " + " + b + " = " + mth.add(a, b));
        }
        catch (Exception e) {
            // Something is wrong ...
            System.err.println("Client - something is wrong: ");
            e.printStackTrace();
        }
    }
}