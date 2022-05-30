package com.aa.pacer;

import com.aa.pacer.IPacerCallback;

interface IPacerService
{
	void start(in long interval, in long maxCount);
	boolean stop();
	boolean pause();
	boolean resume();
	int getState();
	long getLastTickTime();
	long getLastCount();
	void setRingtoneSound(in int ringtone, in int duration);                                        
	void setVibrate(boolean value);                                        
	void setVoice(boolean value);                                        
	void setRingtone(boolean value);    
	void setPauseOnCall(boolean value);                                
	void setCallback(in IPacerCallback callback);                                       
}

