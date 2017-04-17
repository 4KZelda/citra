package org.citraemu.citraemu.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.citraemu.citraemu.BuildConfig;
import org.citraemu.citraemu.R;
import org.citraemu.citraemu.activities.EmulationActivity;

public final class MenuFragment extends Fragment implements View.OnClickListener
{
	public static final String FRAGMENT_TAG = BuildConfig.APPLICATION_ID + ".ingame_menu";
	public static final int FRAGMENT_ID = R.layout.fragment_ingame_menu;
	private TextView mTitleText;
	private static SparseIntArray buttonsActionsMap = new SparseIntArray();
	static {
		buttonsActionsMap.append(R.id.menu_exit, EmulationActivity.MENU_ACTION_EXIT);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(FRAGMENT_ID, container, false);

		LinearLayout options = (LinearLayout) rootView.findViewById(R.id.layout_options);
		for (int childIndex = 0; childIndex < options.getChildCount(); childIndex++)
		{
			Button button = (Button) options.getChildAt(childIndex);

			button.setOnClickListener(this);
		}

		mTitleText = (TextView) rootView.findViewById(R.id.text_game_title);

		return rootView;
	}

	@SuppressWarnings("WrongConstant")
	@Override
	public void onClick(View button)
	{
		((EmulationActivity) getActivity()).handleMenuAction(buttonsActionsMap.get(button.getId()));
	}

	public void setTitleText(String title)
	{
		mTitleText.setText(title);
	}
}
