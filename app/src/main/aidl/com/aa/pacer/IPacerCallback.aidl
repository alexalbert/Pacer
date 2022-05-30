package com.aa.pacer;

interface IPacerCallback
{
	void tick(in long lastTickTime, in long lastCount); 
	void finish();
	void pause();
	void resume();
}

