package com.supinfo.supchain.helpers;

public class SpinnerCLI extends Thread {
    private String context;
    public boolean showProgress = true;

    public SpinnerCLI(String context) {
        this.context = context;
    }

    public void run() {
        String anim= "|/-\\";
        int x = 0;
        while (showProgress) {
            System.out.print("\r"+ context + anim.charAt(x++ % anim.length())+ "\r");
            try { Thread.sleep(100); }
            catch (Exception e) {};
        }
    }
}
