package com.odiousapps.weewxweather;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.pes.androidmaterialcolorpickerdialog.ColorPickerCallback;

import org.json.JSONObject;

import java.io.File;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{
    private TabLayout tabLayout;
    private Common common;
    private DrawerLayout mDrawerLayout;
	private EditText settingsURL;
	private EditText customURL;
	private EditText fgColour;
	private EditText bgColour;
	private Button b1;
	private Button b2;
	private Button b3;
	private boolean showSettings = true;
	private Spinner s1;
	private SwitchCompat show_indoor, metric_forecasts, dark_theme;
	private TextView tv;

	private ProgressDialog dialog;

	private static int pos;
	private static String[] paths;

	@SuppressLint("WrongConstant")
	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        common = new Common(this);

	    mDrawerLayout = findViewById(R.id.drawer_layout);

        tabLayout = findViewById(R.id.tabs);

	    if(!common.GetBoolPref("radarforecast", true))
		    Objects.requireNonNull(tabLayout.getTabAt(2)).setText(R.string.radar);

        try
        {
            if(common.GetStringPref("BASE_URL", "").equals(""))
	            mDrawerLayout.openDrawer(Gravity.START);
        } catch (Exception e) {
            e.printStackTrace();
        }

	    settingsURL = findViewById(R.id.settings);
	    customURL = findViewById(R.id.customURL);
	    s1 = findViewById(R.id.spinner1);

	    metric_forecasts = findViewById(R.id.metric_forecasts);
	    show_indoor = findViewById(R.id.show_indoor);
	    dark_theme = findViewById(R.id.dark_theme);

	    b1 = findViewById(R.id.button);
	    b2 = findViewById(R.id.deleteData);
	    b3 = findViewById(R.id.aboutButton);

	    fgColour = findViewById(R.id.fgPicker);
	    bgColour = findViewById(R.id.bgPicker);

	    tv = findViewById(R.id.aboutText);

	    Thread t = new Thread(new Runnable()
	    {
		    @Override
		    public void run()
		    {
			    try
			    {
				    // Sleep needed to stop frames dropping while loading
				    Thread.sleep(500);
			    } catch (Exception e) {
				    e.printStackTrace();
			    }

			    Handler mHandler = new Handler(Looper.getMainLooper());
			    mHandler.post(new Runnable()
			    {
				    @Override
				    public void run()
				    {
					    doSettings();
				    }
			    });

			    common.setAlarm("MainActivity");
		    }
	    });

	    t.start();
    }

	private void showUpdateAvailable()
	{
		final AlertDialog.Builder d = new AlertDialog.Builder(this);
		d.setTitle("weeWX Weather App");
		d.setMessage("This app has been updated but the server you are connecting to hasn't updated the Inigo Plugin for weeWX. Fields may not show up properly until weeWX is updated.");
		d.setPositiveButton("OK", null);
		d.setIcon(R.drawable.ic_launcher_foreground);
		d.show();
	}

    private void doSettings()
    {
    	paths = new String[]
	    {
            getString(R.string.manual_update),
		    getString(R.string.every_5_minutes),
		    getString(R.string.every_10_minutes),
		    getString(R.string.every_15_minutes),
		    getString(R.string.every_30_minutes),
		    getString(R.string.every_hour),
	    };
	    ArrayAdapter<String> adapter = new ArrayAdapter<>(common.context, R.layout.spinner_layout, paths);
	    adapter.setDropDownViewResource(R.layout.spinner_layout);
	    s1.setAdapter(adapter);
	    s1.setOnItemSelectedListener(this);
	    pos = common.GetIntPref("updateInterval", 1);
	    s1.setSelection(pos);

	    SwitchCompat wifi_only = findViewById(R.id.wifi_only);
	    wifi_only.setChecked(common.GetBoolPref("onlyWIFI", false));
	    SwitchCompat use_icons = findViewById(R.id.use_icons);
	    use_icons.setChecked(common.GetBoolPref("use_icons", false));
	    metric_forecasts.setChecked(common.GetBoolPref("metric", true));
	    show_indoor.setChecked(common.GetBoolPref("showIndoor", false));
	    dark_theme.setChecked(common.GetBoolPref("dark_theme", false));

	    RadioButton showRadar = findViewById(R.id.showRadar);
	    showRadar.setChecked(common.GetBoolPref("radarforecast", true));
	    RadioButton showForecast = findViewById(R.id.showForecast);
	    showForecast.setChecked(!common.GetBoolPref("radarforecast", true));

	    SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
	    ViewPager mViewPager = findViewById(R.id.container);
	    mViewPager.setAdapter(mSectionsPagerAdapter);
	    mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
	    tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

	    b1.setOnClickListener(new View.OnClickListener()
	    {
		    public void onClick(View arg0)
		    {
			    b1.setEnabled(false);
			    b2.setEnabled(false);
			    InputMethodManager mgr = (InputMethodManager)common.context.getSystemService(Context.INPUT_METHOD_SERVICE);
			    if(mgr != null)
			    {
				    mgr.hideSoftInputFromWindow(settingsURL.getWindowToken(), 0);
				    mgr.hideSoftInputFromWindow(customURL.getWindowToken(), 0);
			    }

			    Common.LogMessage("show dialog");
			    dialog = ProgressDialog.show(common.context, "Testing submitted URLs", "Please wait while we verify the URL you submitted.", false);
			    dialog.show();

			    processSettings();
		    }
	    });

	    b2.setOnClickListener(new View.OnClickListener()
	    {
		    public void onClick(View arg0)
		    {
			    checkReally();
		    }
	    });

	    b3.setOnClickListener(new View.OnClickListener()
	    {
		    public void onClick(View arg0)
		    {
			    if(showSettings)
			    {
				    showSettings = false;
				    b1.setVisibility(View.INVISIBLE);
				    b2.setVisibility(View.INVISIBLE);
				    b3.setText(R.string.settings2);

				    LinearLayout settingsLayout = findViewById(R.id.settingsLayout);
				    settingsLayout.setVisibility(View.GONE);
				    LinearLayout aboutLayout = findViewById(R.id.aboutLayout);
				    aboutLayout.setVisibility(View.VISIBLE);
			    } else {
				    showSettings = true;
				    b1.setVisibility(View.VISIBLE);
				    b2.setVisibility(View.VISIBLE);
				    b3.setText(R.string.about2);

				    LinearLayout aboutLayout = findViewById(R.id.aboutLayout);
				    aboutLayout.setVisibility(View.GONE);
				    LinearLayout settingsLayout = findViewById(R.id.settingsLayout);
				    settingsLayout.setVisibility(View.VISIBLE);
			    }

		    }
	    });

	    settingsURL.setText(common.GetStringPref("SETTINGS_URL", "https://example.com/weewx/inigo-settings.txt"));
	    settingsURL.setOnFocusChangeListener(new View.OnFocusChangeListener()
	    {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus)
		    {
			    if (!hasFocus)
				    hideKeyboard(v);
		    }
	    });

	    customURL.setText(common.GetStringPref("custom_url", ""));
	    customURL.setOnFocusChangeListener(new View.OnFocusChangeListener()
	    {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus)
		    {
			    if (!hasFocus)
				    hideKeyboard(v);
		    }
	    });

	    LinearLayout settingsLayout = findViewById(R.id.settingsLayout);
	    settingsLayout.setVisibility(View.VISIBLE);
	    LinearLayout aboutLayout = findViewById(R.id.aboutLayout);
	    aboutLayout.setVisibility(View.GONE);

	    String lines = "<html><body>Big thanks to the <a href='http://weewx.com'>weeWX project</a>, as this app " +
			    "wouldn't be possible otherwise.<br><br>" +
			    "Weather Icons from <a href='https://www.flaticon.com/'>FlatIcon</a> and " +
			    "is licensed under <a href='http://creativecommons.org/licenses/by/3.0/'>CC 3.0 BY</a> and " +
			    "<a href='https://github.com/erikflowers/weather-icons'>Weather Font</a> by Erik Flowers" +
			    "<br><br>" +
			    "Forecasts by" +
			    "<a href='https://www.yahoo.com/?ilc=401'>Yahoo!</a>, " +
			    "<a href='https://weatherzone.com.au'>weatherzone</a>, " +
			    "<a href='https://hjelp.yr.no/hc/en-us/articles/360001940793-Free-weather-data-service-from-Yr'>yr.no</a>, " +
			    "<a href='https://bom.gov.au'>Bureau of Meteorology</a>, " +
			    "<a href='https://www.weather.gov'>Weather.gov</a>, " +
			    "<a href='https://worldweather.wmo.int/en/home.html'>World Meteorology Organisation</a>, " +
			    "<a href='https://weather.gc.ca'>Environment Canada</a>, " +
			    "<a href='https://www.metoffice.gov.uk'>UK Met Office</a>, " +
			    "<a href='https://www.aemet.es'>La Agencia Estatal de Meteorología</a>, " +
			    "<a href='https://www.dwd.de'>Deutscher Wetterdienst</a>, " +
			    "<a href='https://metservice.com'>MetService.com</a>, " +
			    "<a href='https://meteofrance.com'>MeteoFrance.com</a>, " +
			    "<a href='https://darksky.net'>DarkSky.net</a>" +
			    "<br><br>" +
			    "weeWX Weather App v" + common.getAppversion() + " is by <a href='https://odiousapps.com'>OdiousApps</a>.</body</html>";

	    tv.setText(Html.fromHtml(lines));
	    tv.setMovementMethod(LinkMovementMethod.getInstance());

	    // https://github.com/Pes8/android-material-color-picker-dialog

	    String hex = "#" + Integer.toHexString(common.GetIntPref("fgColour", 0xFF000000)).toUpperCase();
	    fgColour.setText(hex);
	    fgColour.setOnClickListener(new View.OnClickListener()
	    {
		    @Override
		    public void onClick(View v)
		    {
			    showPicker(common.GetIntPref("fgColour", 0xFF000000),true);
		    }
	    });

	    hex = "#" + Integer.toHexString(common.GetIntPref("bgColour", 0xFFFFFFFF)).toUpperCase();
	    bgColour.setText(hex);
	    bgColour.setOnClickListener(new View.OnClickListener()
	    {
		    @Override
		    public void onClick(View v)
		    {
			    showPicker(common.GetIntPref("bgColour", 0xFFFFFFFF),false);
		    }
	    });
    }

	private void showPicker(int col, final boolean fgColour)
    {
	    final ColorPicker cp = new ColorPicker(MainActivity.this, col >> 24 & 255, col >> 16 & 255, col >> 8 & 255, col & 255);

	    cp.setCallback(new ColorPickerCallback()
	    {
		    @Override
		    public void onColorChosen(@ColorInt int colour)
		    {
			    Common.LogMessage("Pure Hex" + Integer.toHexString(colour));

			    if(fgColour)
				    common.SetIntPref("fgColour", colour);
			    else
				    common.SetIntPref("bgColour", colour);

			    common.SendIntents();

			    cp.dismiss();
		    }
	    });

	    cp.show();
    }

	private void hideKeyboard(View view)
	{
		InputMethodManager inputMethodManager = (InputMethodManager)common.context.getSystemService(Activity.INPUT_METHOD_SERVICE);
		if(inputMethodManager != null)
			inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
	{
		pos = position;
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) { }

	private void checkReally()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(common.context);
		builder.setMessage("Are you sure you want to remove all data?").setCancelable(false)
				.setPositiveButton("Yes", new android.content.DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialoginterface, int i)
					{
						Common.LogMessage("trash all data");

						//common.RemovePref("SETTINGS_URL");
						common.RemovePref("updateInterval");
						common.RemovePref("BASE_URL");
						common.RemovePref("radtype");
						common.RemovePref("RADAR_URL");
						common.RemovePref("FORECAST_URL");
						common.RemovePref("fctype");
						common.RemovePref("WEBCAM_URL");
						common.RemovePref("CUSTOM_URL");
						common.RemovePref("custom_url");
						common.RemovePref("metric");
						common.RemovePref("showIndoor");
						common.RemovePref("rssCheck");
						common.RemovePref("forecastData");
						common.RemovePref("LastDownload");
						common.RemovePref("LastDownloadTime");
						common.RemovePref("radarforecast");
						common.RemovePref("seekBar");
						common.RemovePref("fgColour");
						common.RemovePref("bgColour");
						common.RemovePref("bomtown");
						common.RemovePref("metierev");
						common.RemovePref("dark_theme");
						common.RemovePref("use_icons");
						common.commit();

						File file = new File(common.context.getFilesDir(), "webcam.jpg");
						if(file.exists() && file.canWrite())
							if(!file.delete())
								Common.LogMessage("couldn't delete webcam.jpg");

						file = new File(common.context.getFilesDir(), "radar.gif");
						if(file.exists() && file.canWrite())
							if(!file.delete())
								Common.LogMessage("couldn't delete radar.gif");

						RemoteViews remoteViews = common.buildUpdate(common.context);
						ComponentName thisWidget = new ComponentName(common.context, WidgetProvider.class);
						AppWidgetManager manager = AppWidgetManager.getInstance(common.context);
						manager.updateAppWidget(thisWidget, remoteViews);
						Common.LogMessage("widget intent broadcasted");

						dialoginterface.cancel();

						System.exit(0);
					}
				}).setNegativeButton("No", new android.content.DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialoginterface, int i)
			{
				dialoginterface.cancel();
			}
		});

		builder.create().show();

	}

	private void processSettings()
	{
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				boolean validURL = false;
				boolean validURL1 = false;
				boolean validURL2 = false;
				boolean validURL3 = false;
				boolean validURL5 = false;

				common.SetStringPref("lastError", "");

				String olddata = common.GetStringPref("BASE_URL", "");
				String oldradar = common.GetStringPref("RADAR_URL", "");
				String oldforecast = common.GetStringPref("FORECAST_URL", "");
				String oldwebcam = common.GetStringPref("WEBCAM_URL", "");
				String oldcustom = common.GetStringPref("CUSTOM_URL", "");
				String oldcustom_url = common.GetStringPref("custom_url", "");

				String data = "", radtype = "", radar = "", forecast = "", webcam = "", custom = "", custom_url, fctype = "", bomtown = "", metierev;

				SwitchCompat metric_forecasts = findViewById(R.id.metric_forecasts);
				SwitchCompat show_indoor = findViewById(R.id.show_indoor);
				SwitchCompat wifi_only = findViewById(R.id.wifi_only);
				SwitchCompat use_icons = findViewById(R.id.use_icons);

				RadioButton showRadar = findViewById(R.id.showRadar);
				long curtime = Math.round(System.currentTimeMillis() / 1000.0);

				if(use_icons.isChecked() && (common.GetLongPref("icon_version", 0) < Common.icon_version || !common.checkForImages()))
				{
					try
					{
						if (!common.downloadIcons())
						{
							common.SetStringPref("lastError", "Icons failed to download fully, you will need to retry.");
							handlerForecastIcons.sendEmptyMessage(1);
							return;
						}
					} catch (Exception e) {
						common.SetStringPref("lastError", e.toString());
						handlerForecastIcons.sendEmptyMessage(1);
						return;
					}

					common.SetLongPref("icon_version", Common.icon_version);
				}

				if (settingsURL.getText().toString().equals("https://example.com/weewx/inigo-settings.txt") || settingsURL.getText().toString().equals(""))
				{
					common.SetStringPref("lastError", "URL was set to the default or was empty");
					handlerSettings.sendEmptyMessage(0);
					return;
				}

				try
				{
					String[] bits = common.downloadSettings(settingsURL.getText().toString()).split("\\n");
					for (String bit : bits)
					{
						String[] mb = bit.split("=", 2);
						mb[0] = mb[0].trim().toLowerCase();
						if (mb[0].equals("data"))
							data = mb[1];
						if (mb[0].equals("radtype"))
							radtype = mb[1].toLowerCase();
						if (mb[0].equals("radar"))
							radar = mb[1];
						if (mb[0].equals("fctype"))
							fctype = mb[1].toLowerCase();
						if (mb[0].equals("forecast"))
							forecast = mb[1];
						if (mb[0].equals("webcam"))
							webcam = mb[1];
						if (mb[0].equals("custom"))
							custom = mb[1];
					}

					if(fctype.equals(""))
						fctype = "Yahoo";

					if(radtype.equals(""))
						radtype = "image";

					validURL = true;
				} catch (Exception e) {
					common.SetStringPref("lastError", e.toString());
					e.printStackTrace();
				}

				if (!validURL)
				{
					handlerSettings.sendEmptyMessage(0);
					return;
				}

				Common.LogMessage("data == " + data);

				if (data.equals(""))
				{
					common.SetStringPref("lastError", "Data url was blank");
					handlerDATA.sendEmptyMessage(0);
					return;
				}

				if (!data.equals(olddata))
				{
					try
					{
						common.reallyGetWeather(data);
						validURL1 = true;
					} catch (Exception e) {
						common.SetStringPref("lastError", e.toString());
						e.printStackTrace();
					}
				} else
					validURL1 = true;

				if (!validURL1)
				{
					handlerDATA.sendEmptyMessage(0);
					return;
				}

				if (!radar.equals("") && !radar.equals(oldradar))
				{
					try
					{
						if(radtype.equals("image"))
						{
							File file = new File(getFilesDir(), "/radar.gif.tmp");
							File f = common.downloadJSOUP(file, radar);
							validURL2 = f.exists();
						} else if(radtype.equals("webpage")) {
							validURL2 = common.checkURL(radar);
						}
					} catch (Exception e) {
						common.SetStringPref("lastError", e.toString());
						e.printStackTrace();
					}

					if (!validURL2)
					{
						handlerRADAR.sendEmptyMessage(0);
						return;
					}
				}

				if(!forecast.equals(""))
				{
					try
					{
						switch (fctype.toLowerCase())
						{
							case "yahoo":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								if(!forecast.startsWith("http"))
								{
									common.SetStringPref("lastError", "Yahoo API recently changed, you need to update your settings.");
									handlerForecast.sendEmptyMessage(0);
									return;
								}
								break;
							case "weatherzone":
								forecast = "https://rss.weatherzone.com.au/?u=12994-1285&lt=aploc&lc=" + forecast + "&obs=0&fc=1&warn=0";
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "yr.no":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "met.no":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "bom.gov.au":
								bomtown = forecast.split(",")[1].trim();
								common.SetStringPref("bomtown", bomtown);
								forecast = "ftp://ftp.bom.gov.au/anon/gen/fwo/" + forecast.split(",")[0].trim() + ".xml";
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								Common.LogMessage("bomtown=" + bomtown);
								break;
							case "wmo.int":
								if(!forecast.startsWith("http"))
									forecast = "https://worldweather.wmo.int/en/json/" + forecast.trim() + "_en.xml";
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "weather.gov":
								String lat = "", lon = "";

								if(forecast.contains("?"))
									forecast = forecast.split("\\?", 2)[1].trim();

								if(forecast.contains("lat") && forecast.contains("lon"))
								{
									String[] tmp = forecast.split("&");
									for(String line : tmp)
									{
										if(line.split("=", 2)[0].equals("lat"))
											lat = line.split("=", 2)[1].trim();
										if(line.split("=", 2)[0].equals("lon"))
											lon = line.split("=", 2)[1].trim();
									}
								} else {
									lat = forecast.split(",")[0].trim();
									lon = forecast.split(",")[1].trim();
								}

								forecast = "https://forecast.weather.gov/MapClick.php?lat=" + lat + "&lon=" + lon + "&unit=0&lg=english&FcstType=json";
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "weather.gc.ca":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "weather.gc.ca-fr":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "metoffice.gov.uk":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "bom2":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "aemet.es":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "dwd.de":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "metservice.com":
								forecast = "https://www.metservice.com/publicData/localForecast" + forecast;
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "meteofrance.com":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "darksky.net":
								forecast += "?exclude=currently,minutely,hourly,alerts,flags";
								forecast += "&lang=" + Locale.getDefault().getLanguage();
								if(metric_forecasts.isChecked())
									forecast += "&units=ca";
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "openweathermap.org":
								if(metric_forecasts.isChecked())
									forecast += "&units=metric";
								else
									forecast += "&units=imperial";
								forecast += "&lang=" + Locale.getDefault().getLanguage();
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "apixu.com":
								forecast += "&days=10";
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "weather.com":
								forecast = "https://api.weather.com/v3/wx/forecast/daily/5day?geocode=" + forecast + "&format=json&apiKey=d522aa97197fd864d36b418f39ebb323";
								//forecast = "https://api.weather.com/v2/turbo/vt1dailyForecast?apiKey=d522aa97197fd864d36b418f39ebb323&format=json&geocode=" + forecast + "&language=en-US";
								if(metric_forecasts.isChecked())
									forecast += "&units=m";
								else
									forecast += "&units=e";
								forecast += "&language=" + Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "met.ie":
								metierev = "https://prodapi.metweb.ie/location/reverse/" + forecast.replaceAll(",", "/");
								forecast = "https://prodapi.metweb.ie/weather/daily/" + forecast.replaceAll(",", "/") + "/10";
								if(common.GetStringPref("metierev", "").equals("") || !forecast.equals(oldforecast))
								{
									metierev = common.downloadForecast(fctype, metierev, null);
									JSONObject jobj = new JSONObject(metierev);
									metierev = jobj.getString("city") + ", Ireland";
									common.SetStringPref("metierev", metierev);
								}
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								Common.LogMessage("metierev=" + metierev);
								break;
							case "ilmeteo.it":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							case "tempoitalia.it":
								Common.LogMessage("forecast=" + forecast);
								Common.LogMessage("fctype=" + fctype);
								break;
							default:
								common.SetStringPref("lastError", "forecast type " + fctype + " is invalid, check your settings file and try again.");
								handlerForecast.sendEmptyMessage(0);
								return;
						}
					} catch (Exception e) {
						common.SetStringPref("lastError", e.toString());
						e.printStackTrace();
					}
				}

				Common.LogMessage("line 742");

				if((fctype.equals("weather.gov") || fctype.equals("yahoo")) && !common.checkForImages() && !use_icons.isChecked())
				{
					common.SetStringPref("lastError", "Forecast type '" + fctype + "' needs to have icons available, Please switch to using icons and try again.");
					handlerForecastIcons.sendEmptyMessage(0);
					return;
				}

				if (!forecast.equals("") && !oldforecast.equals(forecast))
				{
					Common.LogMessage("forecast checking: " + forecast);

					try
					{
						Common.LogMessage("checking: " + forecast);
						String tmp = common.downloadForecast(fctype, forecast, bomtown);
						if(tmp != null)
						{
							validURL3 = true;
							Common.LogMessage("updating rss cache");
							common.SetLongPref("rssCheck", curtime);
							common.SetStringPref("forecastData", tmp);
						}
					} catch (Exception e) {
						common.SetStringPref("lastError", e.toString());
						e.printStackTrace();
					}

					if (!validURL3)
					{
						handlerForecast.sendEmptyMessage(0);
						return;
					}
				}

				if (!webcam.equals("") && !webcam.equals(oldwebcam))
				{
					Common.LogMessage("checking: " + webcam);

					if (!Webcam.downloadWebcam(webcam, common.context.getFilesDir()))
					{
						handlerWEBCAM.sendEmptyMessage(0);
						return;
					}
				}

				custom_url = customURL.getText().toString();

				if(custom_url.equals(""))
				{
					if (!custom.equals("") && !custom.equals("https://example.com/mobile.html") && !custom.equals(oldcustom))
					{
						try
						{
							if(common.checkURL(custom))
								validURL5 = true;
							else
								common.RemovePref("custom_url");
						} catch (Exception e) {
							common.SetStringPref("lastError", e.toString());
							e.printStackTrace();
						}

						if (!validURL5)
						{
							handlerCUSTOM.sendEmptyMessage(0);
							return;
						}
					}
				} else {
					if (!custom_url.equals(oldcustom_url))
					{
						try
						{
							if(common.checkURL(custom_url))
								validURL5 = true;
						} catch (Exception e) {
							common.SetStringPref("lastError", e.toString());
							e.printStackTrace();
						}

						if (!validURL5)
						{
							handlerCUSTOM_URL.sendEmptyMessage(0);
							return;
						}
					}
				}

				if(forecast.equals(""))
				{
					common.SetLongPref("rssCheck", 0);
					common.SetStringPref("forecastData", "");
				}

				common.SetStringPref("SETTINGS_URL", settingsURL.getText().toString());
				common.SetIntPref("updateInterval", pos);
				common.SetStringPref("BASE_URL", data);
				common.SetStringPref("radtype", radtype);
				common.SetStringPref("RADAR_URL", radar);
				common.SetStringPref("FORECAST_URL", forecast);
				common.SetStringPref("fctype", fctype);
				common.SetStringPref("WEBCAM_URL", webcam);
				common.SetStringPref("CUSTOM_URL", custom);
				common.SetStringPref("custom_url", custom_url);
				common.SetBoolPref("radarforecast", showRadar.isChecked());

				common.SetBoolPref("metric", metric_forecasts.isChecked());
				common.SetBoolPref("showIndoor", show_indoor.isChecked());
				common.SetBoolPref("dark_theme", dark_theme.isChecked());
				common.SetBoolPref("onlyWIFI", wifi_only.isChecked());
				common.SetBoolPref("use_icons", use_icons.isChecked());

				common.SendRefresh();
				handlerDone.sendEmptyMessage(0);
			}
		});

		t.start();
	}

	@SuppressLint("HandlerLeak")
	private final Handler handlerDone = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			if(!common.GetBoolPref("radarforecast", true))
				//noinspection ConstantConditions
				tabLayout.getTabAt(2).setText(R.string.radar);
			else
				//noinspection ConstantConditions
				tabLayout.getTabAt(2).setText(R.string.forecast2);

			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			mDrawerLayout.closeDrawer(GravityCompat.START);
		}
	};

	@SuppressLint("HandlerLeak")
	private final Handler handlerSettings = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download the settings from your server")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private final Handler handlerDATA = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download data.txt from your server")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private final Handler handlerRADAR = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download radar= image from the internet")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private final Handler handlerForecast = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download the forecast.")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private final Handler handlerForecastIcons = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to detect forecast icons.")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private final Handler handlerWEBCAM = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download a webcam image from your server")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private final Handler handlerCUSTOM = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download from the custom URL specified")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@SuppressLint("HandlerLeak")
	private final Handler handlerCUSTOM_URL = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			b1.setEnabled(true);
			b2.setEnabled(true);
			dialog.dismiss();
			new AlertDialog.Builder(common.context)
					.setTitle("Wasn't able to connect or download from the custom URL specified")
					.setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
					.setPositiveButton("I'll Fix It and Try Again", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					}).show();
		}
	};

	@Override
    public void onBackPressed()
    {
	    if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
	    {
		    mDrawerLayout.closeDrawer(GravityCompat.START);
	    } else {
		    super.onBackPressed();
		    Common.LogMessage("finishing up.");
		    finish();
	    }
    }

    @Override
    public void onPause()
    {
	    unregisterReceiver(serviceReceiver);
	    super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

	    IntentFilter filter = new IntentFilter();
	    filter.addAction(Common.UPDATE_INTENT);
	    filter.addAction(Common.FAILED_INTENT);
	    filter.addAction(Common.TAB0_INTENT);
	    filter.addAction(Common.INIGO_INTENT);
	    registerReceiver(serviceReceiver, filter);

	    Common.LogMessage("resuming app updates");
	    common.SendIntents();
    }

    public void getWeather()
    {
        common.getWeather();
    }

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                Common.LogMessage("We have a hit, so we should probably update the screen.");
                String action = intent.getAction();
                if(action != null && action.equals(Common.TAB0_INTENT))
                {
                    getWeather();

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
	                        //noinspection ConstantConditions
	                        tabLayout.getTabAt(0).select();
                        }
                    });
                }

	            if(action != null && action.equals(Common.UPDATE_INTENT))
	            {
		            String hex = "#" + Integer.toHexString(common.GetIntPref("fgColour", 0xFF000000)).toUpperCase();
		            fgColour.setText(hex);
		            hex = "#" + Integer.toHexString(common.GetIntPref("bgColour", 0xFFFFFFFF)).toUpperCase();
		            bgColour.setText(hex);
	            }

	            if(action != null && action.equals(Common.INIGO_INTENT))
	            {
		            showUpdateAvailable();
	            }

	            if(action != null && action.equals(Common.FAILED_INTENT))
	            {
		            runOnUiThread(new Runnable()
		            {
			            @Override
			            public void run()
			            {
				            new AlertDialog
						            .Builder(common.context)
						            .setTitle("An error occurred while attempting to update usage")
						            .setMessage(common.GetStringPref("lastError", "Unknown error occurred"))
						            .setPositiveButton("Ok", new DialogInterface.OnClickListener()
						            {
							            @Override
							            public void onClick(DialogInterface dialog, int which)
							            {
							            }
						            }).show();
			            }
		            });

	            }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static class PlaceholderFragment extends Fragment
    {
        private static final String ARG_SECTION_NUMBER = "section_number";
        private int lastPos = 0;
        private Weather weather;
        private Stats stats;
        private Forecast forecast;
        private Webcam webcam;
        private Custom custom;

        public PlaceholderFragment() {}

        public static PlaceholderFragment newInstance(int sectionNumber)
        {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onPause()
        {
        	super.onPause();
	        switch(lastPos)
	        {
		        case 1:
			        weather.doPause();
			        break;
		        case 2:
			        stats.doPause();
			        break;
		        case 3:
			        forecast.doPause();
			        break;
		        case 4:
			        webcam.doPause();
			        break;
		        case 5:
			        custom.doPause();
			        break;
	        }

	        Common.LogMessage("onPause() has been called lastpos ="+lastPos);
        }

        @Override
	    public void onResume()
	    {
		    super.onResume();
		    switch(lastPos)
		    {
			    case 1:
				    weather.doResume();
				    break;
			    case 2:
				    stats.doResume();
				    break;
			    case 3:
				    forecast.doResume();
				    break;
			    case 4:
				    webcam.doResume();
				    break;
			    case 5:
				    custom.doResume();
				    break;
		    }

		    Common.LogMessage("onResume() has been called lastpos ="+lastPos);
	    }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
        	if(container == null)
        		return null;

	        Common common = new Common(getContext());

	        Bundle args = getArguments();

	        if(args != null)
	        {
		        lastPos = args.getInt(ARG_SECTION_NUMBER);

		        if (args.getInt(ARG_SECTION_NUMBER) == 1)
		        {
			        weather = new Weather(common);
			        return weather.myWeather(inflater, container);
		        } else if (args.getInt(ARG_SECTION_NUMBER) == 2) {
			        stats = new Stats(common);
			        return stats.myStats(inflater, container);
		        } else if (args.getInt(ARG_SECTION_NUMBER) == 3) {
			        forecast = new Forecast(common);
			        return forecast.myForecast(inflater, container);
		        } else if (args.getInt(ARG_SECTION_NUMBER) == 4) {
			        webcam = new Webcam(common);
			        return webcam.myWebcam(inflater, container);
		        } else if (args.getInt(ARG_SECTION_NUMBER) == 5) {
			        custom = new Custom(common);
			        return custom.myCustom(inflater, container);
		        }
	        }
            return null;
        }
    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position)
        {
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount()
        {
            return 5;
        }
    }
}