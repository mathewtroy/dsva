package cz.cvut.fel.dsva.compute;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MathServer extends Remote {
    public int add(int a, int b) throws RemoteException;
    public int sub(int a, int b) throws RemoteException;

//	public int mul(int a, int b) throws RemoteException;
//	public int div(int a, int b) throws RemoteException;
//	public float divF(float a, float b) throws RemoteException;
}
