package org.citraemu.citraemu.utils;

import android.os.Environment;
import android.support.annotation.NonNull;

import org.citraemu.citraemu.model.settings.BooleanSetting;
import org.citraemu.citraemu.model.settings.FloatSetting;
import org.citraemu.citraemu.model.settings.IntSetting;
import org.citraemu.citraemu.model.settings.Setting;
import org.citraemu.citraemu.model.settings.SettingSection;
import org.citraemu.citraemu.model.settings.StringSetting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Contains static methods for interacting with .ini files in which settings are stored.
 */
public final class SettingsFile
{
	private static final String fileName = "android-config.ini";

	public static final String SECTION_CORE = "Core";
	public static final String KEY_CORE_USE_CPU_JIT = "use_cpu_jit";

	public static final String SECTION_GRAPHICS = "Graphics";
	public static final String KEY_GFX_USE_HW_RENDERER = "use_hw_renderer";
	public static final String KEY_GFX_USE_SHADER_JIT = "use_shader_jit";
	public static final String KEY_GFX_USE_VSYNC = "use_vsync";
	public static final String KEY_GFX_TOGGLE_FRAMELIMIT = "toggle_framelimit";
	public static final String KEY_GFX_RESOLUTION_FACTOR = "resolution_factor";

	public static final String SECTION_LAYOUT = "Layout";
	public static final String KEY_LAYOUT_LAYOUT_OPTION = "layout_option";
	public static final String KEY_LAYOUT_SWAP_SCREEN = "swap_screen";

	public static final String SECTION_AUDIO = "Audio";
	public static final String KEY_AUDIO_OUTPUT_ENGINE = "output_engine";
	public static final String KEY_AUDIO_ENABLE_AUDIO_STRETCHING = "enable_audio_stretching";

	public static final String SECTION_SYSTEM = "System";
	public static final String KEY_SYSTEM_IS_NEW_3DS = "is_new_3ds";
	public static final String KEY_SYSTEM_REGION_VALUE = "region_value";

	public static final String SECTION_PATHS = "Paths";
	public static final String KEY_PATHS_GAME_LIST_ROOT_DIR = "gameListRootDir";
	public static final String KEY_PATHS_GAME_LIST_DEEP_SCAN = "gameListDeepScan";

	public static final String SECTION_CONTROLS = "Controls";

	public static final String KEY_CONTROLS_PAD_A = "pad_a";
	public static final String KEY_CONTROLS_PAD_B = "pad_b";
	public static final String KEY_CONTROLS_PAD_X = "pad_x";
	public static final String KEY_CONTROLS_PAD_Y = "pad_y";
	public static final String KEY_CONTROLS_PAD_L = "pad_l";
	public static final String KEY_CONTROLS_PAD_R = "pad_r";
	public static final String KEY_CONTROLS_PAD_ZL = "pad_zl";
	public static final String KEY_CONTROLS_PAD_ZR = "pad_zr";
	public static final String KEY_CONTROLS_PAD_START = "pad_start";
	public static final String KEY_CONTROLS_PAD_SELECT = "pad_select";
	public static final String KEY_CONTROLS_PAD_HOME = "pad_home";
	public static final String KEY_CONTROLS_PAD_DUP = "pad_dup";
	public static final String KEY_CONTROLS_PAD_DDOWN = "pad_ddown";
	public static final String KEY_CONTROLS_PAD_DLEFT = "pad_dleft";
	public static final String KEY_CONTROLS_PAD_DRIGHT = "pad_dright";
	public static final String KEY_CONTROLS_PAD_SUP = "pad_sup";
	public static final String KEY_CONTROLS_PAD_SDOWN = "pad_sdown";
	public static final String KEY_CONTROLS_PAD_SLEFT = "pad_sleft";
	public static final String KEY_CONTROLS_PAD_SRIGHT = "pad_sright";
	public static final String KEY_CONTROLS_PAD_CUP = "pad_cup";
	public static final String KEY_CONTROLS_PAD_CDOWN = "pad_cdown";
	public static final String KEY_CONTROLS_PAD_CLEFT = "pad_cleft";
	public static final String KEY_CONTROLS_PAD_CRIGHT = "pad_cright";
	public static final String KEY_CONTROLS_BUTTON_A = "button_a";
	public static final String KEY_CONTROLS_BUTTON_B = "button_b";
	public static final String KEY_CONTROLS_BUTTON_X = "button_x";
	public static final String KEY_CONTROLS_BUTTON_Y = "button_y";
	public static final String KEY_CONTROLS_BUTTON_UP = "button_up";
	public static final String KEY_CONTROLS_BUTTON_DOWN = "button_down";
	public static final String KEY_CONTROLS_BUTTON_LEFT = "button_left";
	public static final String KEY_CONTROLS_BUTTON_RIGHT = "button_right";
	public static final String KEY_CONTROLS_BUTTON_L = "button_l";
	public static final String KEY_CONTROLS_BUTTON_R = "button_r";
	public static final String KEY_CONTROLS_BUTTON_START = "button_start";
	public static final String KEY_CONTROLS_BUTTON_SELECT = "button_select";
	public static final String KEY_CONTROLS_BUTTON_ZL = "button_zl";
	public static final String KEY_CONTROLS_BUTTON_ZR = "button_zr";
	public static final String KEY_CONTROLS_BUTTON_HOME = "button_home";
	public static final String KEY_CONTROLS_CIRCLE_PAD = "circle_pad";
	public static final String KEY_CONTROLS_C_STICK = "c_stick";
	public static final String KEY_CONTROLS_PAD_CIRCLE_UP = "pad_circle_up";
	public static final String KEY_CONTROLS_PAD_CIRCLE_DOWN = "pad_circle_down";
	public static final String KEY_CONTROLS_PAD_CIRCLE_LEFT = "pad_circle_left";
	public static final String KEY_CONTROLS_PAD_CIRCLE_RIGHT = "pad_circle_right";
	public static final String KEY_CONTROLS_PAD_CIRCLE_MODIFIER = "pad_circle_modifier";
	public static final String KEY_CONTROLS_PAD_CIRCLE_MODIFIER_SCALE = "pad_circle_modifier_scale";

	private SettingsFile()
	{
	}

	/**
	 * Reads a given .ini file from disk and returns it as a HashMap of SettingSections, themselves
	 * effectively a HashMap of key/value settings. If unsuccessful, outputs an error telling why it
	 * failed.
	 *
	 * @return An Observable that emits a HashMap of the file's contents, then completes.
	 */
	public static HashMap<String, SettingSection> readFile()
	{
		HashMap<String, SettingSection> sections = new HashMap<>();

		File ini = getSettingsFile();

		BufferedReader reader = null;

		try
		{
			reader = new BufferedReader(new FileReader(ini));

			SettingSection current = null;
			for (String line; (line = reader.readLine()) != null; )
			{
				if (line.startsWith("[") && line.endsWith("]"))
				{
					current = sectionFromLine(line);
					sections.put(current.getName(), current);
				}
				else if ((current != null) && line.contains(" = "))
				{
					Setting setting = settingFromLine(current, line, fileName);
					current.putSetting(setting);
				}
			}
		}
		catch (FileNotFoundException e)
		{
			Log.error("[SettingsFile] File not found: " + fileName + ".ini: " + e.getMessage());
		}
		catch (IOException e)
		{
			Log.error("[SettingsFile] Error reading from: " + fileName + ".ini: " + e.getMessage());
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					Log.error("[SettingsFile] Error closing: " + fileName + ".ini: " + e.getMessage());
				}
			}
		}

		return sections;
	}

	/**
	 * Saves a Settings HashMap to a given .ini file on disk. If unsuccessful, outputs an error
	 * telling why it failed.
	 *
	 * @param sections The HashMap containing the Settings we want to serialize.
	 * @return An Observable representing the operation.
	 */
	public static void saveFile(final HashMap<String, SettingSection> sections)
	{
		File ini = getSettingsFile();

		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(ini, "UTF-8");

			Set<String> keySet = sections.keySet();
			Set<String> sortedKeySet = new TreeSet<>(keySet);

			for (String key : sortedKeySet)
			{
				SettingSection section = sections.get(key);
				writeSection(writer, section);
			}
		}
		catch (FileNotFoundException e)
		{
			Log.error("[SettingsFile] File not found: " + fileName + ".ini: " + e.getMessage());
			// FIXME view.showToastMessage("Error saving " + fileName + ".ini: " + e.getMessage());
		}
		catch (UnsupportedEncodingException e)
		{
			Log.error("[SettingsFile] Bad encoding; please file a bug report: " + fileName + ".ini: " + e.getMessage());
			// FIXME view.showToastMessage("Error saving " + fileName + ".ini: " + e.getMessage());
		}
		finally
		{
			if (writer != null)
			{
				writer.close();
			}
		}
	}

	@NonNull
	private static File getSettingsFile()
	{
		String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
		return new File(storagePath + "/Citra/config/" + fileName);
	}

	private static SettingSection sectionFromLine(String line)
	{
		String sectionName = line.substring(1, line.length() - 1);
		return new SettingSection(sectionName);
	}

	/**
	 * For a line of text, determines what type of data is being represented, and returns
	 * a Setting object containing this data.
	 *
	 * @param current  The section currently being parsed by the consuming method.
	 * @param line     The line of text being parsed.
	 * @param fileName The name of the ini file the setting is in.
	 * @return A typed Setting containing the key/value contained in the line.
	 */
	private static Setting settingFromLine(SettingSection current, String line, String fileName)
	{
		String[] splitLine = line.split(" = ");

		String key = splitLine[0].trim();
		String value = splitLine[1].trim();

		try
		{
			int valueAsInt = Integer.valueOf(value);

			return new IntSetting(key, current.getName(), valueAsInt);
		}
		catch (NumberFormatException ex)
		{
		}

		try
		{
			float valueAsFloat = Float.valueOf(value);

			return new FloatSetting(key, current.getName(), valueAsFloat);
		}
		catch (NumberFormatException ex)
		{
		}

		switch (value)
		{
			case "True":
				return new BooleanSetting(key, current.getName(), true);
			case "False":
				return new BooleanSetting(key, current.getName(), false);
			default:
				return new StringSetting(key, current.getName(), value);
		}
	}

	/**
	 * Writes the contents of a Section HashMap to disk.
	 *
	 * @param writer A PrintWriter pointed at a file on disk.
	 * @param section A section containing settings to be written to the file.
	 */
	private static void writeSection(PrintWriter writer, SettingSection section)
	{
		// Write the section header.
		String header = sectionAsString(section);
		writer.println(header);

		// Write this section's values.
		HashMap<String, Setting> settings = section.getSettings();
		Set<String> keySet = settings.keySet();
		Set<String> sortedKeySet = new TreeSet<>(keySet);

		for (String key : sortedKeySet)
		{
			Setting setting = settings.get(key);
			String settingString = settingAsString(setting);

			writer.println(settingString);
		}
	}

	private static String sectionAsString(SettingSection section)
	{
		return "[" + section.getName() + "]";
	}

	private static String settingAsString(Setting setting)
	{
		return setting.getKey() + " = " + setting.getValueAsString();
	}
}
