package com.aa.pacer;


import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class AboutDlg extends AlertDialog
{

	public AboutDlg(Context context)
	{
		super(context);
		
		View view = LayoutInflater.from(context).inflate(R.layout.about, null);
		setView(view);
		
		TextView web = (TextView)view.findViewById(R.id.web);
		TextView email = (TextView)view.findViewById(R.id.email);
		TextView other = (TextView)view.findViewById(R.id.other);
		
		SpannableString str = SpannableString.valueOf("Web: www.eyeonweb.com");
		Linkify.addLinks(str, Linkify.ALL); 
	    web.append(str); 

	    SpannableString str1 = SpannableString.valueOf("Email: android@eyeonweb.com"); 
		Linkify.addLinks(str1, Linkify.ALL); 
	    email.append(str1); 

	    SpannableString str2 = SpannableString.valueOf("Please check our other   applications"); 
		Linkify.addLinks(str2, Pattern.compile("applications"), "");
		other.append(str2);
	    
	    web.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) 
			{
				try
				{
					Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.eyeonweb.com/android/Pacer.htm")); 
					viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getContext().startActivity(viewIntent);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}});
	
	    email.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) 
			{
			  Intent sendIntent = new Intent(Intent.ACTION_SEND); 
	          sendIntent.putExtra(Intent.EXTRA_EMAIL, new  String [] {"android@eyeonweb.com"}); 
	          sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Faves Plus"); 
	          sendIntent.setType("message/rfc822");
	          getContext().startActivity(sendIntent); 
			}});

	    other.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) 
			{
				try
				{
					Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:EyeOnWeb")); 
					viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getContext().startActivity(viewIntent);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}});
	}
}
