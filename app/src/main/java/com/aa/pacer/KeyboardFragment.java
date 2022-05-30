package com.aa.pacer;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.TextView;

public class KeyboardFragment extends Fragment
{
	TextView	mK0;
	TextView	mK1;
	TextView	mK2;
	TextView	mK3;
	TextView	mK4;
	TextView	mK5;
	TextView	mK6;
	TextView	mK7;
	TextView	mK8;
	TextView	mK9;
	TextView	mClear;
	TextView	mDone;

	EditText mCurrentEdit; 
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_keyboard, container, false);
		view.setBackgroundColor(Color.GRAY);
		
		mK0 = (TextView) view.findViewById(R.id.k0);
		mK1 = (TextView) view.findViewById(R.id.k1);
		mK2 = (TextView) view.findViewById(R.id.k2);
		mK3 = (TextView) view.findViewById(R.id.k3);
		mK4 = (TextView) view.findViewById(R.id.k4);
		mK5 = (TextView) view.findViewById(R.id.k5);
		mK6 = (TextView) view.findViewById(R.id.k6);
		mK7 = (TextView) view.findViewById(R.id.k7);
		mK8 = (TextView) view.findViewById(R.id.k8);
		mK9 = (TextView) view.findViewById(R.id.k9);
		mClear = (TextView) view.findViewById(R.id.clear);
		mDone = (TextView) view.findViewById(R.id.done);
		
        mK0.setOnTouchListener(mKeypadListener);
        mK1.setOnTouchListener(mKeypadListener);
        mK2.setOnTouchListener(mKeypadListener);
        mK3.setOnTouchListener(mKeypadListener);
        mK4.setOnTouchListener(mKeypadListener);
        mK5.setOnTouchListener(mKeypadListener);
        mK6.setOnTouchListener(mKeypadListener);
        mK7.setOnTouchListener(mKeypadListener);
        mK8.setOnTouchListener(mKeypadListener);
        mK9.setOnTouchListener(mKeypadListener);
        mClear.setOnTouchListener(mKeypadListener);
        mDone.setOnTouchListener(mKeypadListener);
        
        
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if( keyCode == KeyEvent.KEYCODE_BACK ) {
                	((PacerUI)getActivity()).removeKeyboard();
                    return true;
                } else {
                    return false;
                }
            }
        });
        
		return view;
	}
	
    OnTouchListener mKeypadListener = new OnTouchListener()
    {
		public boolean onTouch(View v, MotionEvent event) 
		{
			if (event.getAction() == MotionEvent.ACTION_DOWN)
			{
				if (v.getId() == R.id.done)
				{
					((PacerUI)getActivity()).removeKeyboard();
				}
				if (v.getId() == R.id.clear)
				{
					((PacerUI) getActivity()).getMCurrentEdit().setText("");
				}
				else
				{
					String c;
					switch (v.getId())
					{
						case R.id.k0:
							c = "0";
							break;
						case R.id.k1:
							c = "1";
							break;
						case R.id.k2:
							c = "2";
							break;
						case R.id.k3:
							c = "3";
							break;
						case R.id.k4:
							c = "4";
							break;
						case R.id.k5:
							c = "5";
							break;
						case R.id.k6:
							c = "6";
							break;
						case R.id.k7:
							c = "7";
							break;
						case R.id.k8:
							c = "8";
							break;
						case R.id.k9:
							c = "9";
							break;
						default:
							c = "";
					}
					
					EditText currentEdit = ((PacerUI) getActivity()).getMCurrentEdit();
					currentEdit.setText(currentEdit.getText() + c);
				}
			}
			return false;
		}
    };
}
