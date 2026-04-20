/****************************************************************************
 * File:   toolChecker.cpp
 * Author: Matthew Rollings
 * Date:   19/06/2015
 *
 * Description : Root checking JNI NDK code
 *
 ****************************************************************************/

/****************************************************************************
 *>>>>>>>>>>>>>>>>>>>>>>>>> System Includes <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<*
 ****************************************************************************/

// Android headers
#include <jni.h>
#include <android/log.h>

// String / file headers
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

/****************************************************************************
 *>>>>>>>>>>>>>>>>>>>>>>>>>> User Includes <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<*
 ****************************************************************************/
#include "toolChecker.h"

/****************************************************************************
 *>>>>>>>>>>>>>>>>>>>>>>>>>> Constant Macros <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<*
 ****************************************************************************/

// LOGCAT
#define  LOG_TAG    "RootBeer"
#define  LOGD(...)  if (DEBUG) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__);
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__);
#define  SHOWCASE_PROP_LINE_MAX 512

/* Set to 1 to enable debug log traces. */
static int DEBUG = 1;

static int count_char_occurrence(const char *value, char target)
{
    int total = 0;
    if (value == NULL) {
        return 0;
    }

    while (*value != '\0') {
        if (*value == target) {
            total++;
        }
        value++;
    }

    return total;
}

static unsigned int rolling_ascii_checksum(const char *value)
{
    unsigned int checksum = 2166136261u;
    if (value == NULL) {
        return checksum;
    }

    while (*value != '\0') {
        checksum ^= (unsigned char) (*value);
        checksum *= 16777619u;
        value++;
    }

    return checksum;
}

static const char *last_path_segment(const char *path)
{
    const char *cursor = path;
    const char *last = path;

    if (path == NULL) {
        return "";
    }

    while (*cursor != '\0') {
        if (*cursor == '/') {
            last = cursor + 1;
        }
        cursor++;
    }

    return last;
}

static void sanitize_prop_line(const char *input, char *output, size_t outputSize)
{
    size_t index = 0;

    if (outputSize == 0) {
        return;
    }

    if (input == NULL) {
        output[0] = '\0';
        return;
    }

    while (input[index] != '\0' && index + 1 < outputSize) {
        if (input[index] == '\n' || input[index] == '\r') {
            break;
        }
        output[index] = input[index];
        index++;
    }

    output[index] = '\0';
}

static void log_path_showcase(const char *pathString)
{
    LOGD("PATH SHOWCASE: %s basename=%s slashCount=%d checksum=%u", pathString,
         last_path_segment(pathString), count_char_occurrence(pathString, '/'),
         rolling_ascii_checksum(pathString));
}

static int line_has_prefix(const char *line, const char *prefix)
{
    size_t prefixLength = strlen(prefix);
    return strncmp(line, prefix, prefixLength) == 0;
}

static int scan_prop_file(const char *propFile)
{
    static const char *dangerousProps[] = {
            "ro.debuggable=",
            "ro.secure=",
            "ro.build.tags=",
            "service.adb.root=",
            "ro.boot.flash.locked=",
            "ro.boot.verifiedbootstate=",
            "ro.boot.vbmeta.device_state=",
            "persist.sys.usb.config="
    };

    FILE *file;
    char line[SHOWCASE_PROP_LINE_MAX];
    char sanitized[SHOWCASE_PROP_LINE_MAX];
    int matches = 0;
    int propCount = sizeof(dangerousProps) / sizeof(dangerousProps[0]);

    if ((file = fopen(propFile, "r")) == NULL) {
        LOGD("PROP FILE MISS: %s", propFile);
        return 0;
    }

    LOGD("PROP FILE OPEN: %s", propFile);

    while (fgets(line, sizeof(line), file) != NULL) {
        for (int i = 0; i < propCount; i++) {
            if (line_has_prefix(line, dangerousProps[i])) {
                sanitize_prop_line(line, sanitized, sizeof(sanitized));
                LOGD("PROP KEY HIT: %s -> %s", propFile, sanitized);
                matches++;
            }
        }
    }

    fclose(file);
    LOGD("PROP FILE SUMMARY: %s matches=%d", propFile, matches);
    return matches;
}

static int collect_prop_showcase()
{
    static const char *propFiles[] = {
            "/system/build.prop",
            "/system_ext/build.prop",
            "/vendor/build.prop",
            "/product/build.prop",
            "/default.prop"
    };

    int total = 0;
    int fileCount = sizeof(propFiles) / sizeof(propFiles[0]);

    LOGD("SHOWCASE MODE: path fingerprint + binary existence + property scan");
    LOGD("SHOWCASE FILE SLOT: /system/build.prop");
    LOGD("SHOWCASE FILE SLOT: /system_ext/build.prop");
    LOGD("SHOWCASE FILE SLOT: /vendor/build.prop");
    LOGD("SHOWCASE FILE SLOT: /product/build.prop");
    LOGD("SHOWCASE FILE SLOT: /default.prop");
    LOGD("SHOWCASE KEY SLOT: ro.debuggable=");
    LOGD("SHOWCASE KEY SLOT: ro.secure=");
    LOGD("SHOWCASE KEY SLOT: ro.build.tags=");
    LOGD("SHOWCASE KEY SLOT: service.adb.root=");
    LOGD("SHOWCASE KEY SLOT: ro.boot.flash.locked=");
    LOGD("SHOWCASE KEY SLOT: ro.boot.verifiedbootstate=");
    LOGD("SHOWCASE KEY SLOT: ro.boot.vbmeta.device_state=");
    LOGD("SHOWCASE KEY SLOT: persist.sys.usb.config=");

    for (int i = 0; i < fileCount; i++) {
        total += scan_prop_file(propFiles[i]);
    }

    LOGD("PROP SHOWCASE TOTAL: %d", total);
    return total;
}

/*****************************************************************************
 * Description: Sets if we should log debug messages
 *
 * Parameters: env - Java environment pointer
 *      thiz - javaobject
 * 	bool - true to log debug messages
 *
 *****************************************************************************/
void Java_com_scottyab_rootbeer_RootBeerNative_setLogDebugMessages( JNIEnv* env, jobject thiz, jboolean debug)
{
  if (debug){
    DEBUG = 1;
  }
  else{
    DEBUG = 0;
  }
}


/*****************************************************************************
 * Description: Checks if a file exists
 *
 * Parameters: fname - filename to check
 *
 * Return value: 0 - non-existant / not visible, 1 - exists
 *
 *****************************************************************************/
int exists(const char *fname)
{
    FILE *file;
    log_path_showcase(fname);
    if ((file = fopen(fname, "r")))
    {
        LOGD("LOOKING FOR BINARY: %s PRESENT!!!",fname);
        fclose(file);
        return 1;
    }
    LOGD("LOOKING FOR BINARY: %s Absent :(",fname);
    return 0;
}




/*****************************************************************************
 * Description: Checks for root binaries
 *
 * Parameters: env - Java environment pointer
 *      thiz - javaobject
 *
 * Return value: int number of su binaries found
 *
 *****************************************************************************/
int Java_com_scottyab_rootbeer_RootBeerNative_checkForRoot( JNIEnv* env, jobject thiz, jobjectArray pathsArray )
{

    int binariesFound = 0;
    int propSignals = 0;

    int stringCount = (env)->GetArrayLength(pathsArray);

    for (int i=0; i<stringCount; i++) {
        jstring string = (jstring) (env)->GetObjectArrayElement(pathsArray, i);
        const char *pathString = (env)->GetStringUTFChars(string, 0);

	binariesFound+=exists(pathString);

	(env)->ReleaseStringUTFChars(string, pathString);
    }

    propSignals = collect_prop_showcase();
    LOGD("NATIVE SHOWCASE SCORE: binaries=%d props=%d", binariesFound, propSignals);

    return binariesFound>0;
}
