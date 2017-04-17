package org.citraemu.citraemu.model.settings.view;

import org.citraemu.citraemu.model.settings.Setting;

public final class HeaderSetting extends SettingsItem
{
	public HeaderSetting(String key, Setting setting, int titleId, int descriptionId)
	{
		super(key, null, 0, setting, titleId, descriptionId);
	}

	@Override
	public int getType()
	{
		return SettingsItem.TYPE_HEADER;
	}
}
