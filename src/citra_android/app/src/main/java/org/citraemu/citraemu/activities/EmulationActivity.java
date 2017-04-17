package org.citraemu.citraemu.activities;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.citraemu.citraemu.NativeLibrary;
import org.citraemu.citraemu.R;
import org.citraemu.citraemu.fragments.EmulationFragment;
import org.citraemu.citraemu.fragments.MenuFragment;
import org.citraemu.citraemu.ui.main.MainPresenter;
import org.citraemu.citraemu.utils.Animations;
import org.citraemu.citraemu.utils.Log;

import java.lang.annotation.Retention;
import java.util.List;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public final class EmulationActivity extends AppCompatActivity
{
	private View mDecorView;
	private ImageView mImageView;

	private FrameLayout mFrameEmulation;
	private LinearLayout mMenuLayout;

	private String mSubmenuFragmentTag;

	private SharedPreferences mPreferences;

	// So that MainActivity knows which view to invalidate before the return animation.
	private int mPosition;

	private boolean mDeviceHasTouchScreen;
	private boolean mSystemUiVisible;
	private boolean mMenuVisible;

	/**
	 * Handlers are a way to pass a message to an Activity telling it to do something
	 * on the UI thread. This Handler responds to any message, even blank ones, by
	 * hiding the system UI.
	 */
	private Handler mSystemUiHider = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			hideSystemUI();
		}
	};
	private FrameLayout mFrameContent;
	private String mSelectedTitle;

	@Retention(SOURCE)
	@IntDef({MENU_ACTION_EDIT_CONTROLS_PLACEMENT, MENU_ACTION_TOGGLE_CONTROLS, MENU_ACTION_ADJUST_SCALE,
			MENU_ACTION_EXIT})
	public @interface MenuAction {
	}

	public static final int MENU_ACTION_EDIT_CONTROLS_PLACEMENT = 0;
	public static final int MENU_ACTION_TOGGLE_CONTROLS = 1;
	public static final int MENU_ACTION_ADJUST_SCALE = 2;
	public static final int MENU_ACTION_EXIT = 22;


	private static SparseIntArray buttonsActionsMap = new SparseIntArray();
	static {
		buttonsActionsMap.append(R.id.menu_emulation_edit_layout, EmulationActivity.MENU_ACTION_EDIT_CONTROLS_PLACEMENT);
		buttonsActionsMap.append(R.id.menu_emulation_toggle_controls, EmulationActivity.MENU_ACTION_TOGGLE_CONTROLS);
		buttonsActionsMap.append(R.id.menu_emulation_adjust_scale, EmulationActivity.MENU_ACTION_ADJUST_SCALE);

		buttonsActionsMap.append(R.id.menu_exit, EmulationActivity.MENU_ACTION_EXIT);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		mDeviceHasTouchScreen = getPackageManager().hasSystemFeature("android.hardware.touchscreen");

		int themeId;
		if (mDeviceHasTouchScreen)
		{
			themeId = R.style.CitraEmulationBase;

			// Get a handle to the Window containing the UI.
			mDecorView = getWindow().getDecorView();

			// Set these options now so that the SurfaceView the game renders into is the right size.
			mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
					View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

			// Set the ActionBar to follow the navigation/status bar's visibility changes.
			mDecorView.setOnSystemUiVisibilityChangeListener(
					new View.OnSystemUiVisibilityChangeListener()
					{
						@Override
						public void onSystemUiVisibilityChange(int flags)
						{
							mSystemUiVisible = (flags & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;

							if (mSystemUiVisible)
							{
								getSupportActionBar().show();
								hideSystemUiAfterDelay();
							}
							else
							{
								getSupportActionBar().hide();
							}
						}
					});
		}
		else
		{
			themeId = R.style.CitraEmulationTvBase;
		}

		setTheme(themeId);
		super.onCreate(savedInstanceState);

		// Picasso will take a while to load these big-ass screenshots. So don't run
		// the animation until we say so.
		postponeEnterTransition();

		setContentView(R.layout.activity_emulation);

		mImageView = (ImageView) findViewById(R.id.image_screenshot);
		mFrameContent = (FrameLayout) findViewById(R.id.frame_content);
		mFrameEmulation = (FrameLayout) findViewById(R.id.frame_emulation_fragment);
		mMenuLayout = (LinearLayout) findViewById(R.id.layout_ingame_menu);

		Intent gameToEmulate = getIntent();
		String path = gameToEmulate.getStringExtra("SelectedGame");
		mSelectedTitle = gameToEmulate.getStringExtra("SelectedTitle");
		mPosition = gameToEmulate.getIntExtra("GridPosition", -1);

		if (savedInstanceState == null)
		{
			Animations.fadeViewOut(mImageView)
					.setStartDelay(2000)
					.withStartAction(new Runnable()
					{
						@Override
						public void run()
						{
							mFrameEmulation.setVisibility(View.VISIBLE);
						}
					})
					.withEndAction(new Runnable()
					{
						@Override
						public void run()
						{
							mImageView.setVisibility(View.GONE);
						}
					});

			// Instantiate an EmulationFragment.
			EmulationFragment emulationFragment = EmulationFragment.newInstance(path);

			// Add fragment to the activity - this triggers all its lifecycle callbacks.
			getFragmentManager().beginTransaction()
					.add(R.id.frame_emulation_fragment, emulationFragment, EmulationFragment.FRAGMENT_TAG)
					.commit();
		}
		else
		{
			mImageView.setVisibility(View.GONE);
			mFrameEmulation.setVisibility(View.VISIBLE);
		}

		if (mDeviceHasTouchScreen)
		{
			setTitle(mSelectedTitle);
		}
		else
		{
			MenuFragment menuFragment = (MenuFragment) getFragmentManager()
					.findFragmentById(R.id.fragment_menu);

			if (menuFragment != null)
			{
				menuFragment.setTitleText(mSelectedTitle);
			}
		}

		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		Log.debug("[EmulationActivity] EmulationActivity starting.");
		NativeLibrary.setEmulationActivity(this);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		Log.debug("[EmulationActivity] EmulationActivity stopping.");

		NativeLibrary.setEmulationActivity(null);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);

		if (mDeviceHasTouchScreen)
		{
			// Give the user a few seconds to see what the controls look like, then hide them.
			hideSystemUiAfterDelay();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);

		if (mDeviceHasTouchScreen)
		{
			if (hasFocus)
			{
				hideSystemUiAfterDelay();
			}
			else
			{
				// If the window loses focus (i.e. a dialog box, or a popup menu is on screen
				// stop hiding the UI.
				mSystemUiHider.removeMessages(0);
			}
		}
	}

	@Override
	public void onBackPressed()
	{
		if (!mDeviceHasTouchScreen)
		{
			if (mSubmenuFragmentTag != null)
			{
				removeSubMenu();
			}
			else
			{
				toggleMenu();
			}
		}
		else
		{
			stopEmulation();
		}
	}

	private void toggleMenu()
	{
		if (mMenuVisible)
		{
			mMenuVisible = false;

			Animations.fadeViewOutToLeft(mMenuLayout)
					.withEndAction(new Runnable()
					{
						@Override
						public void run()
						{
							if (mMenuVisible)
							{
								mMenuLayout.setVisibility(View.GONE);
							}
						}
					});
		}
		else
		{
			mMenuVisible = true;
			Animations.fadeViewInFromLeft(mMenuLayout);
		}
	}

	private void stopEmulation()
	{
		EmulationFragment fragment = (EmulationFragment) getFragmentManager()
				.findFragmentByTag(EmulationFragment.FRAGMENT_TAG);
		fragment.notifyEmulationStopped();

		NativeLibrary.StopEmulation();
	}

	private Runnable afterShowingScreenshot = new Runnable()
	{
		@Override
		public void run()
		{
			mFrameContent.removeView(mFrameEmulation);
			setResult(mPosition);
			finishAfterTransition();
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_emulation, menu);
		return true;
	}

	@SuppressWarnings("WrongConstant")
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		handleMenuAction(buttonsActionsMap.get(item.getItemId()));
		return true;
	}

	public void handleMenuAction(@MenuAction int menuAction)
	{
		switch (menuAction)
		{
			// Edit the placement of the controls
			case MENU_ACTION_EDIT_CONTROLS_PLACEMENT:
				editControlsPlacement();
				break;

			// Enable/Disable specific buttons or the entire input overlay.
			case MENU_ACTION_TOGGLE_CONTROLS:
				toggleControls();
				return;

			case MENU_ACTION_EXIT:
				toggleMenu();
				stopEmulation();
				return;
		}
	}


	private void editControlsPlacement() {
		EmulationFragment emulationFragment = (EmulationFragment) getFragmentManager()
				.findFragmentById(R.id.frame_emulation_fragment);
		if (emulationFragment.isConfiguringControls()) {
			emulationFragment.stopConfiguringControls();
		} else {
			emulationFragment.startConfiguringControls();
		}
	}

	// Gets button presses
	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (mMenuVisible)
		{
			return super.dispatchKeyEvent(event);
		}

		int action;

		switch (event.getAction())
		{
			case KeyEvent.ACTION_DOWN:
				// Handling the case where the back button is pressed.
				if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
				{
					onBackPressed();
					return true;
				}

				// Normal key events.
				action = NativeLibrary.ButtonState.PRESSED;
				break;
			case KeyEvent.ACTION_UP:
				action = NativeLibrary.ButtonState.RELEASED;
				break;
			default:
				return false;
		}
		InputDevice input = event.getDevice();
		return NativeLibrary.onGamePadEvent(input.getDescriptor(), event.getKeyCode(), action);
	}

	private void toggleControls() {
		final SharedPreferences.Editor editor = mPreferences.edit();
		boolean[] enabledButtons = new boolean[14];
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.emulation_toggle_controls);
		for (int i = 0; i < enabledButtons.length; i++) {
			enabledButtons[i] = mPreferences.getBoolean("buttonToggleGc" + i, true);
		}
		builder.setNeutralButton(getString(R.string.emulation_toggle_all), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				EmulationFragment emulationFragment = (EmulationFragment) getFragmentManager()
						.findFragmentByTag(EmulationFragment.FRAGMENT_TAG);
				emulationFragment.toggleInputOverlayVisibility();
			}
		});
		builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				editor.apply();

				EmulationFragment emulationFragment = (EmulationFragment) getFragmentManager()
						.findFragmentByTag(EmulationFragment.FRAGMENT_TAG);
				emulationFragment.refreshInputOverlay();
			}
		});

		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event)
	{
		if (mMenuVisible)
		{
			return false;
		}

		if (((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) == 0))
		{
			return super.dispatchGenericMotionEvent(event);
		}

		// Don't attempt to do anything if we are disconnecting a device.
		if (event.getActionMasked() == MotionEvent.ACTION_CANCEL)
			return true;

		InputDevice input = event.getDevice();
		List<InputDevice.MotionRange> motions = input.getMotionRanges();

		for (InputDevice.MotionRange range : motions)
		{
			NativeLibrary.onGamePadMoveEvent(input.getDescriptor(), range.getAxis(), event.getAxisValue(range.getAxis()));
		}

		return true;
	}

	private void hideSystemUiAfterDelay()
	{
		// Clear any pending hide events.
		mSystemUiHider.removeMessages(0);

		// Add a new hide event, to occur 3 seconds from now.
		mSystemUiHider.sendEmptyMessageDelayed(0, 3000);
	}

	private void hideSystemUI()
	{
		mSystemUiVisible = false;

		mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_FULLSCREEN |
				View.SYSTEM_UI_FLAG_IMMERSIVE);
	}

	private void showSystemUI()
	{
		mSystemUiVisible = true;

		mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

		hideSystemUiAfterDelay();
	}


	private void scheduleStartPostponedTransition(final View sharedElement)
	{
		sharedElement.getViewTreeObserver().addOnPreDrawListener(
				new ViewTreeObserver.OnPreDrawListener()
				{
					@Override
					public boolean onPreDraw()
					{
						sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
						startPostponedEnterTransition();
						return true;
					}
				});
	}

	private void removeSubMenu()
	{
		if (mSubmenuFragmentTag != null)
		{
			final Fragment fragment = getFragmentManager().findFragmentByTag(mSubmenuFragmentTag);

			if (fragment != null)
			{
				// When removing a fragment without replacement, its animation must be done
				// manually beforehand.
				Animations.fadeViewOutToRight(fragment.getView())
						.withEndAction(new Runnable()
						{
							@Override
							public void run()
							{
								if (mMenuVisible)
								{
									getFragmentManager().beginTransaction()
											.remove(fragment)
											.commit();
								}
							}
						});
			}
			else
			{
				Log.error("[EmulationActivity] Fragment not found, can't remove.");
			}

			mSubmenuFragmentTag = null;
		}
		else
		{
			Log.error("[EmulationActivity] Fragment Tag empty.");
		}
	}

	public String getSelectedTitle()
	{
		return mSelectedTitle;
	}

	public static void launch(Activity activity, String path, String title, int position, View sharedView)
	{
		Intent launcher = new Intent(activity, EmulationActivity.class);

		launcher.putExtra("SelectedGame", path);
		launcher.putExtra("SelectedTitle", title);
		launcher.putExtra("GridPosition", position);

		ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
				activity,
				sharedView,
				"image_game_screenshot");

		activity.startActivityForResult(launcher, MainPresenter.REQUEST_EMULATE_GAME, options.toBundle());
	}
}
