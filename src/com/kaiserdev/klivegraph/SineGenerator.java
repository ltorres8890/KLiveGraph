package com.kaiserdev.klivegraph;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class SineGenerator extends Thread {

	private boolean ended;
	private Handler hand;
	private Message msg;
	private Bundle b;
	private double oneDeg = 0.02;
	private double phase;
	
	
	public SineGenerator(Handler handler)
	{
		ended = false;
		msg = new Message();
		hand = handler;
	}
	
	public void end()
	{
		ended = true;
	}
	
	@Override
	public void run() {
		super.run();
		phase = 0;
		while(!ended)
		{
			b = new Bundle();
			b.putDouble("Value", Math.sin(phase));
			msg.setData(b);
			hand.sendMessage(msg);
			phase += oneDeg;
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();				
				ended = true;
			}
		}
	}
}
