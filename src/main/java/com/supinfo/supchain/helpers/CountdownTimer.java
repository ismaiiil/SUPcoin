package com.supinfo.supchain.helpers;

public class CountdownTimer extends Thread{
    private int time;
    public boolean isOver;

    public CountdownTimer(int time){
        this.time = time;
        new Thread(() -> {
            try {
                sleep(time);
                isOver = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();


    }

    private void setIsOver() {
        this.isOver = true;
    }

    private void setTime(int time) {
        this.time = time;
    }


}
