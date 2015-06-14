package model;

/**
 * SystemBus class that holds all of the CPUs and memories, and responds to requests made by the system components.
 *
 * @author Alex Glass, Vitaliy Radchishin, Andy Tran, Tru Truong
 * @version 1.0
 */
public class SystemBus {

    public int issueReadRequest(int address, CPU caller) {

        return 0;
    }

    public int issueWriteRequest(int address, CPU caller) {
        return 0;
    }

    public void issueRequestForOwnership(int address, CPU caller) {

    }
}
