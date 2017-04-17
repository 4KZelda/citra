package org.citraemu.citraemu;

import android.app.Application;

import org.citraemu.citraemu.model.GameDatabase;

public class CitraApplication extends Application
{
	public static GameDatabase databaseHelper;

	@Override
	public void onCreate()
	{
		super.onCreate();

		databaseHelper = new GameDatabase(this);
	}
}
