package cz.cvut.fel.dsva.server;

import cz.cvut.fel.dsva.compute.MathServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {

    public static void main(String[] args) {

//		if (System.getSecurityManager() == null)
//			System.setSecurityManager(new SecurityManager());
//		System.setProperty("java.security.policy","file:///tmp/test.policy");
//		System.setProperty("java.rmi.server.hostname","127.0.0.1");

        // Name of our "service"
        String name = "MathD";
        String name2 = "MathD2";

        try {
            // Create instance of remote object and its skeleton
            MathServer msi = new MathServerImpl();

            MathServer skeleton =
                    (MathServer) UnicastRemoteObject.exportObject(msi, 50000);

            MathServer msi2 = new MathServerImpl2();

            // Create registry and (re)register object name and skeleton in it
            Registry registry = LocateRegistry.createRegistry(2010);
            registry.rebind(name, skeleton);
            registry.rebind(name2, msi2);
        }
        catch (Exception e) {
            // Something is wrong ...
            System.err.println("Server - something is wrong: " + e.getMessage());
        }
        System.out.println("Server is started ...");
    }
}