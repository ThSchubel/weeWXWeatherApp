package com.odiousapps.weewxweather;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash_screen);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					// Sleep needed to stop frames dropping while loading
					Thread.sleep(500);
				} catch (Exception e) {
					e.printStackTrace();
				}

				startApp();
				finish();
			}
		}).start();
	}

	private void startApp()
	{
		Intent intent = new Intent(SplashScreen.this, MainActivity.class);
		startActivity(intent);
	}
}