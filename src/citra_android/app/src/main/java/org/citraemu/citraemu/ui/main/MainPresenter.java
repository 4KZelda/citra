package org.citraemu.citraemu.ui.main;


import android.database.Cursor;

import org.citraemu.citraemu.CitraApplication;
import org.citraemu.citraemu.NativeLibrary;
import org.citraemu.citraemu.R;
import org.citraemu.citraemu.model.GameDatabase;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public final class MainPresenter
{
	public static final int REQUEST_ADD_DIRECTORY = 1;
	public static final int REQUEST_EMULATE_GAME = 2;

	private final MainView mView;

	public MainPresenter(MainView view)
	{
		mView = view;
	}

	public void onCreate()
	{
		// TODO Rather than calling into native code, this should use the commented line below.
		// String versionName = BuildConfig.VERSION_NAME;
		String versionName = NativeLibrary.GetVersionString();
		mView.setVersionString(versionName);
	}

	public void onFabClick()
	{
		mView.launchFileListActivity();
	}

	public boolean handleOptionSelection(int itemId)
	{
		switch (itemId)
		{
			case R.id.menu_settings:
				mView.launchSettingsActivity();
				return true;

			case R.id.menu_refresh:
				GameDatabase databaseHelper = CitraApplication.databaseHelper;
				databaseHelper.scanLibrary(databaseHelper.getWritableDatabase());
				mView.refresh();
				return true;

			case R.id.button_add_directory:
				mView.launchFileListActivity();
				return true;
		}

		return false;
	}

	public void handleActivityResult(int requestCode, int resultCode)
	{
		switch (requestCode)
		{
			case REQUEST_ADD_DIRECTORY:
				// If the user picked a file, as opposed to just backing out.
				if (resultCode == MainActivity.RESULT_OK)
				{
					mView.refresh();
				}
				break;

			case REQUEST_EMULATE_GAME:
				break;
		}
	}

	public void loadGames(final int platformIndex)
	{
		GameDatabase databaseHelper = CitraApplication.databaseHelper;

		databaseHelper.getGamesForPlatform(platformIndex)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<Cursor>()
						   {
							   @Override
							   public void call(Cursor games)
							   {
								   mView.showGames(platformIndex, games);
							   }
						   }
				);
	}
}
