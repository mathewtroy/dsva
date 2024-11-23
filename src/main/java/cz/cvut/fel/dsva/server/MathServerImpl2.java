package cz.cvut.fel.dsva.server;

import cz.cvut.fel.dsva.compute.MathServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MathServerImpl2 extends UnicastRemoteObject implements MathServer {

    public MathServerImpl2() throws RemoteException {
        super();
    }

    @Override
    public int add(int a, int b) throws RemoteException {
        int result;
        result=a+b;
        System.out.println("Implementation.2.add: " + a + " + " + b + " = " + result);
        return result;
    }

    @Override
    public int sub(int a, int b) throws RemoteException {
        int result;
        result=a-b;
        System.out.println("Implementation.2.sub: " + a + " - " + b + " = " + result);
        return result;
    }
}