package cn.nodemedia.mediaclient;

/**
 * Created by serge on 2016/1/13.
 */

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import cn.nodemedia.mediaclient.utils.SharedPreUtil;

public class FragmentPreferences extends Activity {
    // public static final String PREFS_NAME="MainSetting";
    static long pubId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragement()).commit();
    }


    public static class PrefsFragement extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            // TODO Auto-generated method stub
            super.onCreate(savedInstanceState);
            //getPreferenceManager().setSharedPreferencesName(PREFS_NAME);
            pubId = Long.parseLong((String) SharedPreUtil.get(this.getActivity(), "edittext_preference_ClientId", "0"));
            //无存储值
            if (pubId < 1) {
                pubId = Math.round((Math.random() * 10000 + 1000));
                SharedPreUtil.put(this.getActivity(), "edittext_preference_ClientId", Long.toString(pubId));

                String pubAddress = (String) SharedPreUtil.get(this.getActivity(), "edittext_preference_playAddress", getString(R.string.playAddress));
                SharedPreUtil.put(this.getActivity(), "edittext_preference_pubAddress", pubAddress + pubId);
            }
            //此语句会初始化一切preference控件，所有对数据的预修改需要放在此前
            addPreferencesFromResource(R.xml.preferences_main);
            //
            iniSummary();
            //注册监听
            // this.getActivity().getSharedPreferences(PREFS_NAME,0) .registerOnSharedPreferenceChangeListener(this);
            PreferenceManager.getDefaultSharedPreferences(this.getActivity()).registerOnSharedPreferenceChangeListener(this);
        }

        //更新summary
        public void iniSummary() {
            //       SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            //        SharedPreferences prefs = this.getActivity().getSharedPreferences(PREFS_NAME,0);

            Preference connectionPref = findPreference("edittext_preference_playAddress");
            connectionPref.setSummary(prefs.getString("edittext_preference_playAddress", ""));
            connectionPref = findPreference("edittext_preference_playBuffer");
            connectionPref.setSummary(prefs.getString("edittext_preference_playBuffer", ""));
            connectionPref = findPreference("edittext_preference_maxPlayBuffer");
            connectionPref.setSummary(prefs.getString("edittext_preference_maxPlayBuffer", ""));
//            connectionPref =findPreference("checkbox_preference_logShow");
//            connectionPref.setSummary(String.valueOf(prefs.getBoolean("checkbox_preference_logShow", false)));
//            connectionPref = findPreference("checkbox_preference_videoShow");
//            connectionPref.setSummary(String.valueOf(prefs.getBoolean("checkbox_preference_videoShow", false)));
//            connectionPref = findPreference("checkbox_preference_LowLatencyMode");
//            connectionPref.setSummary(String.valueOf(prefs.getBoolean("checkbox_preference_LowLatencyMode", false)));
            connectionPref = findPreference("edittext_preference_pubAddress");
            connectionPref.setSummary(prefs.getString("edittext_preference_pubAddress", ""));
            connectionPref = findPreference("edittext_preference_ChattingAddress");
            connectionPref.setSummary(prefs.getString("edittext_preference_ChattingAddress", ""));
            connectionPref = findPreference("edittext_preference_ClientName");
            connectionPref.setSummary(prefs.getString("edittext_preference_ClientName", ""));
            connectionPref = findPreference("edittext_preference_ClientId");
            connectionPref.setSummary(prefs.getString("edittext_preference_ClientId", ""));
            //remote control
            connectionPref = findPreference("edittext_preference_RobotIp");
            connectionPref.setSummary(prefs.getString("edittext_preference_RobotIp", ""));
            connectionPref = findPreference("edittext_preference_RobotPort");
            connectionPref.setSummary(prefs.getString("edittext_preference_RobotPort", ""));
        }

        //实时监听设置改变的处理
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            // 事件处理器. 根据数据的变化,对显示和行为作改变
            if (key.equals("edittext_preference_playAddress")) {
                Preference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                connectionPref.setSummary(prefs.getString(key, ""));
            } else if (key.equals("edittext_preference_playBuffer")) {
                Preference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                connectionPref.setSummary(prefs.getString(key, ""));
            } else if (key.equals("edittext_preference_maxPlayBuffer")) {
                Preference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                connectionPref.setSummary(prefs.getString(key, ""));
            }
//            else if (key.equals("checkbox_preference_logShow")) {
//                Preference connectionPref = findPreference(key);
//                // Set summary to be the user-description for the selected value
//                connectionPref.setSummary(String.valueOf(prefs.getBoolean(key, false)));
//            }
//            else if (key.equals("checkbox_preference_videoShow")) {
//                Preference connectionPref = findPreference(key);
//                // Set summary to be the user-description for the selected value
//                connectionPref.setSummary(String.valueOf(prefs.getBoolean(key, false)));
//            }
//            else if (key.equals("checkbox_preference_LowLatencyMode")) {
//                Preference connectionPref = findPreference(key);
//                // Set summary to be the user-description for the selected value
//                connectionPref.setSummary(String.valueOf(prefs.getBoolean(key, false)));
//            }
            else if (key.equals("edittext_preference_pubAddress")) {
                Preference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                connectionPref.setSummary(prefs.getString(key, ""));
            } else if (key.equals("edittext_preference_ChattingAddress")) {
                Preference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                connectionPref.setSummary(prefs.getString(key, ""));
            } else if (key.equals("edittext_preference_ClientName")) {
                Preference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                connectionPref.setSummary(prefs.getString(key, ""));
            } else if (key.equals("edittext_preference_ClientId")) {
                Preference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                connectionPref.setSummary(prefs.getString(key, ""));
            }
            else if (key.equals("edittext_preference_RobotIp")) {
                Preference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                connectionPref.setSummary(prefs.getString(key, ""));
            }
            else if (key.equals("edittext_preference_RobotPort")) {
                Preference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                connectionPref.setSummary(prefs.getString(key, ""));
            }

        }

        @Override
        public void onResume() {
            super.onResume();
            PreferenceManager.getDefaultSharedPreferences(this.getActivity()).registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            PreferenceManager.getDefaultSharedPreferences(this.getActivity())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            PreferenceManager.getDefaultSharedPreferences(this.getActivity())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
    }

}