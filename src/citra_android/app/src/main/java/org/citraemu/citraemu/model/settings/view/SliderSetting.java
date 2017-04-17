package org.citraemu.citraemu.model.settings.view;

import org.citraemu.citraemu.model.settings.FloatSetting;
import org.citraemu.citraemu.model.settings.IntSetting;
import org.citraemu.citraemu.model.settings.Setting;
import org.citraemu.citraemu.utils.Log;
import org.citraemu.citraemu.utils.SettingsFile;

public final class SliderSetting extends SettingsItem
{
	private int mMax;
	private int mDefaultValue;

	private String mUnits;

	public SliderSetting(String key, String section, int file, int titleId, int descriptionId, int max, String units, int defaultValue, Setting setting)
	{
		super(key, section, file, setting, titleId, descriptionId);
		mMax = max;
		mUnits = units;
		mDefaultValue = defaultValue;
	}

	public int getMax()
	{
		return mMax;
	}

	/**
	 * Write a value to the backing int. If that int was previously null,
	 * initializes a new one and returns it, so it can be added to the Hashmap.
	 *
	 * @param selection New value of the int.
	 * @return null if overwritten successfully otherwise; a newly created IntSetting.
	 */
	public IntSetting setSelectedValue(int selection)
	{
		if (getSetting() == null)
		{
			IntSetting setting = new IntSetting(getKey(), getSection(), selection);
			setSetting(setting);
			return setting;
		}
		else
		{
			IntSetting setting = (IntSetting) getSetting();
			setting.setValue(selection);
			return null;
		}
	}

	/**
	 * Write a value to the backing float. If that float was previously null,
	 * initializes a new one and returns it, so it can be added to the Hashmap.
	 *
	 * @param selection New value of the float.
	 * @return null if overwritten successfully otherwise; a newly created FloatSetting.
	 */
	public FloatSetting setSelectedValue(float selection)
	{
		if (getSetting() == null)
		{
			FloatSetting setting = new FloatSetting(getKey(), getSection(), selection);
			setSetting(setting);
			return setting;
		}
		else
		{
			FloatSetting setting = (FloatSetting) getSetting();
			setting.setValue(selection);
			return null;
		}
	}

	public String getUnits()
	{
		return mUnits;
	}

	@Override
	public int getType()
	{
		return TYPE_SLIDER;
	}
}
