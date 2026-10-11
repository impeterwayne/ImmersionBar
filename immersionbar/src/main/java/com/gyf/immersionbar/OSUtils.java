package com.gyf.immersionbar;

import android.annotation.SuppressLint;

import com.hjq.device.compat.DeviceBrand;
import com.hjq.device.compat.DeviceOs;

import java.lang.reflect.Method;

/**
 * Operating system and device brand detection utility.
 *
 * @author geyifeng
 * @date 2017/4/18
 */
public class OSUtils {

    /**
     * Whether the device is manufactured by Xiaomi or running MIUI / HyperOS.
     *
     * @return true if Xiaomi device or OS, false otherwise
     */
    public static boolean isXiaoMi() {
        return DeviceBrand.isXiaoMi() || DeviceBrand.isRedMi() || DeviceOs.isMiui() || DeviceOs.isHyperOs();
    }

    /**
     * Whether the device is manufactured by Huawei / Honor or running EMUI / HarmonyOS / MagicOS.
     *
     * @return true if Huawei device or OS, false otherwise
     */
    public static boolean isHuaWei() {
        return DeviceBrand.isHuaWei() || DeviceBrand.isHonor() || DeviceOs.isEmui() || DeviceOs.isHarmonyOs() || DeviceOs.isMagicOs();
    }

    /**
     * Whether the device is manufactured by Oppo / OnePlus / Realme or running ColorOS / Realme UI.
     *
     * @return true if Oppo device or OS, false otherwise
     */
    public static boolean isOppo() {
        return DeviceBrand.isOppo() || DeviceBrand.isOnePlus() || DeviceBrand.isRealMe() || DeviceOs.isColorOs() || DeviceOs.isRealmeUi();
    }

    /**
     * Whether the device is manufactured by Vivo or running OriginOS / Funtouch OS.
     *
     * @return true if Vivo device or OS, false otherwise
     */
    public static boolean isVivo() {
        return DeviceBrand.isVivo() || DeviceOs.isOriginOs() || DeviceOs.isFuntouchOs();
    }

    /**
     * Whether the device is manufactured by Samsung or running One UI.
     *
     * @return true if Samsung device or OS, false otherwise
     */
    public static boolean isSamsung() {
        return DeviceBrand.isSamsung() || DeviceOs.isOneUi();
    }

    /**
     * Whether the device is manufactured by Lenovo.
     *
     * @return true if Lenovo device, false otherwise
     */
    public static boolean isLenovo() {
        return DeviceBrand.isLenovo();
    }

    /**
     * Whether the device is manufactured by Meizu or running Flyme.
     *
     * @return true if Meizu device or OS, false otherwise
     */
    public static boolean isMeizu() {
        return DeviceBrand.isMeiZu() || DeviceOs.isFlyme();
    }

    /**
     * Checks whether the OS is MIUI or HyperOS.
     *
     * @return true if MIUI or HyperOS, false otherwise
     */
    public static boolean isMIUI() {
        return DeviceOs.isMiui() || DeviceOs.isHyperOs();
    }

    /**
     * Checks whether the MIUI version is 6 or higher, or running HyperOS.
     *
     * @return true if MIUI 6 or later, or HyperOS, false otherwise
     */
    public static boolean isMIUI6Later() {
        return DeviceOs.isHyperOs() || (DeviceOs.isMiui() && DeviceOs.getOsBigVersionCode() >= 6);
    }

    /**
     * Gets the MIUI version string.
     *
     * @return the MIUI version name, or empty string
     */
    public static String getMIUIVersion() {
        return isMIUI() ? DeviceOs.getOsVersionName() : "";
    }

    /**
     * Checks whether the OS is EMUI.
     *
     * @return true if EMUI, false otherwise
     */
    public static boolean isEMUI() {
        return DeviceOs.isEmui();
    }

    /**
     * Gets the EMUI version string.
     *
     * @return the EMUI version name, or empty string
     */
    public static String getEMUIVersion() {
        return isEMUI() ? DeviceOs.getOsVersionName() : "";
    }

    /**
     * Checks whether the OS is EMUI 3.1.
     *
     * @return true if EMUI 3.1, false otherwise
     */
    public static boolean isEMUI3_1() {
        return "EmotionUI 3".equals(getEMUIVersion()) || getEMUIVersion().contains("EmotionUI_3.1") || (DeviceOs.isEmui() && DeviceOs.getOsVersionName().contains("3.1"));
    }

    /**
     * Checks whether the OS is EMUI 3.0.
     *
     * @return true if EMUI 3.0, false otherwise
     */
    public static boolean isEMUI3_0() {
        return getEMUIVersion().contains("EmotionUI_3.0") || (DeviceOs.isEmui() && DeviceOs.getOsVersionName().contains("3.0"));
    }

    /**
     * Checks whether the OS is EMUI 3.x.
     *
     * @return true if EMUI 3.x, false otherwise
     */
    public static boolean isEMUI3_x() {
        return DeviceOs.isEmui() && (DeviceOs.getOsBigVersionCode() == 3 || isEMUI3_0() || isEMUI3_1());
    }

    /**
     * Checks whether the OS is ColorOS.
     *
     * @return true if ColorOS, false otherwise
     */
    public static boolean isColorOs() {
        return DeviceOs.isColorOs();
    }

    /**
     * Checks whether the OS is Funtouch OS or OriginOS.
     *
     * @return true if Funtouch OS or OriginOS, false otherwise
     */
    public static boolean isFuntouchOrOriginOs() {
        return DeviceOs.isFuntouchOs() || DeviceOs.isOriginOs();
    }

    /**
     * Checks whether the OS is Flyme OS.
     *
     * @return true if Flyme OS, false otherwise
     */
    public static boolean isFlymeOS() {
        return DeviceOs.isFlyme();
    }

    /**
     * Checks whether the Flyme OS version is 4 or higher.
     *
     * @return true if Flyme OS 4 or later, false otherwise
     */
    public static boolean isFlymeOS4Later() {
        return DeviceOs.isFlyme() && DeviceOs.getOsBigVersionCode() >= 4;
    }

    /**
     * Checks whether the Flyme OS version is 5.
     *
     * @return true if Flyme OS 5, false otherwise
     */
    public static boolean isFlymeOS5() {
        return DeviceOs.isFlyme() && DeviceOs.getOsBigVersionCode() == 5;
    }

    /**
     * Gets the Flyme OS version string.
     *
     * @return the Flyme OS version name, or empty string
     */
    public static String getFlymeOSVersion() {
        return isFlymeOS() ? DeviceOs.getOsVersionName() : "";
    }

    @SuppressLint("PrivateApi")
    private static String getSystemProperty(String key) {
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method method = clz.getMethod("get", String.class, String.class);
            return (String) method.invoke(clz, key, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
