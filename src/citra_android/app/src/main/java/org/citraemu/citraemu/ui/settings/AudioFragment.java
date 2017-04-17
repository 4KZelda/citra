package org.citraemu.citraemu.ui.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import org.citraemu.citraemu.R;

public class AudioFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.audio);
    }
}