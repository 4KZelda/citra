package org.citraemu.citraemu.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.citraemu.citraemu.BuildConfig;
import org.citraemu.citraemu.R;
import org.citraemu.citraemu.activities.EmulationActivity;

import de.hdodenhof.circleimageview.CircleImageView;

public final class GameDetailsDialog extends DialogFragment
{
	public static final String ARGUMENT_GAME_TITLE = BuildConfig.APPLICATION_ID + ".game_title";
	public static final String ARGUMENT_GAME_DESCRIPTION = BuildConfig.APPLICATION_ID + ".game_description";
	public static final String ARGUMENT_GAME_COUNTRY = BuildConfig.APPLICATION_ID + ".game_country";
	public static final String ARGUMENT_GAME_DATE = BuildConfig.APPLICATION_ID + ".game_date";
	public static final String ARGUMENT_GAME_PATH = BuildConfig.APPLICATION_ID + ".game_path";

	// TODO Add all of this to the Loader in GameActivity.java
	public static GameDetailsDialog newInstance(String title, String description, int country, String company, String path, String screenshotPath)
	{
		GameDetailsDialog fragment = new GameDetailsDialog();

		Bundle arguments = new Bundle();
		arguments.putString(ARGUMENT_GAME_TITLE, title);
		arguments.putString(ARGUMENT_GAME_DESCRIPTION, description);
		arguments.putInt(ARGUMENT_GAME_COUNTRY, country);
		arguments.putString(ARGUMENT_GAME_DATE, company);
		arguments.putString(ARGUMENT_GAME_PATH, path);
		fragment.setArguments(arguments);

		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		ViewGroup contents = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.dialog_game_details, null);

		final ImageView imageGameScreen = (ImageView) contents.findViewById(R.id.image_game_screen);
		CircleImageView circleBanner = (CircleImageView) contents.findViewById(R.id.circle_banner);

		TextView textTitle = (TextView) contents.findViewById(R.id.text_game_title);
		TextView textDescription = (TextView) contents.findViewById(R.id.text_company);

		TextView textCountry = (TextView) contents.findViewById(R.id.text_country);
		TextView textDate = (TextView) contents.findViewById(R.id.text_date);

		FloatingActionButton buttonLaunch = (FloatingActionButton) contents.findViewById(R.id.button_launch);

		int countryIndex = getArguments().getInt(ARGUMENT_GAME_COUNTRY);
		String country = getResources().getStringArray(R.array.countryNames)[countryIndex];

		textTitle.setText(getArguments().getString(ARGUMENT_GAME_TITLE));
		textDescription.setText(getArguments().getString(ARGUMENT_GAME_DESCRIPTION));
		textCountry.setText(country);
		textDate.setText(getArguments().getString(ARGUMENT_GAME_DATE));

		buttonLaunch.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				// Start the emulation activity and send the path of the clicked ROM to it.
				EmulationActivity.launch(getActivity(),
						getArguments().getString(ARGUMENT_GAME_PATH),
						getArguments().getString(ARGUMENT_GAME_TITLE),
						-1,
						imageGameScreen);
			}
		});

		builder.setView(contents);
		return builder.create();
	}
}
