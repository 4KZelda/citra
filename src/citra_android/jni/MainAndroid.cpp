// Copyright 2003 Dolphin Emulator Project
// Copyright 2017 Citra Emulator Project
// Licensed under GPLv2+
// Refer to the license.txt file included.

#include <EGL/egl.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <cinttypes>
#include <cstdio>
#include <cstdlib>
#include <memory>
#include <mutex>
#include <thread>
#include <algorithm>
#include <string>
#include <vector>
#include "common/common_types.h"
#include "common/file_util.h"
#include "ButtonManager.h"

#include "common/scm_rev.h"
#include "common/string_util.h"

#include "citra/emu_window/emu_window_sdl2.h"
#include "core/core.h"
#include "core/loader/loader.h"
#include "core/settings.h"
#include "core/loader/smdh.h"
#include <clocale>

#include "input_common/keyboard.h"
#include "video_core/debug_utils/debug_utils.h"

ANativeWindow* surf;
std::string g_filename;
std::string g_set_userpath = "";

JavaVM* g_java_vm;
jclass g_jni_class;
jmethodID g_jni_method_alert;
jmethodID g_jni_method_end;

#define CITRA_TAG "CitraEmuNative"

/*
 * Cache the JavaVM so that we can call into it later.
 */
jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
  g_java_vm = vm;

  return JNI_VERSION_1_6;
}

void Host_NotifyMapLoaded()
{
}
void Host_RefreshDSPDebuggerWindow()
{
}

// The Core only supports using a single Host thread.
// If multiple threads want to call host functions then they need to queue
// sequentially for access.
static std::mutex s_host_identity_lock;
static bool s_have_wm_user_stop = false;
//auto emu_thread;
void Host_Message(int Id)
{
//  if (Id == WM_USER_JOB_DISPATCH)
//  {
//    updateMainFrameEvent.Set();
//  }
//  else if (Id == WM_USER_STOP)
//  {
//    s_have_wm_user_stop = true;
//    if (Core::IsRunning())
//      Core::QueueHostJob(&Core::Stop);
//  }
}

void* Host_GetRenderHandle()
{
  return surf;
}

void Host_UpdateTitle(const std::string& title)
{
  __android_log_write(ANDROID_LOG_INFO, CITRA_TAG, title.c_str());
}

void Host_UpdateDisasmDialog()
{
}

void Host_UpdateMainFrame()
{
}

void Host_RequestRenderWindowSize(int width, int height)
{
}

void Host_SetStartupDebuggingParameters()
{
}

bool Host_UIHasFocus()
{
  return true;
}

bool Host_RendererHasFocus()
{
  return true;
}

bool Host_RendererIsFullscreen()
{
  return false;
}

void Host_ShowVideoConfig(void*, const std::string&)
{
}

void Host_YieldToUI()
{
}

static bool MsgAlert(const char* caption, const char* text, bool yes_no, int /*Style*/)
{
  __android_log_print(ANDROID_LOG_ERROR, CITRA_TAG, "%s:%s", caption, text);

  // Associate the current Thread with the Java VM.
  JNIEnv* env;
  g_java_vm->AttachCurrentThread(&env, NULL);

  // Execute the Java method.
  env->CallStaticVoidMethod(g_jni_class, g_jni_method_alert, env->NewStringUTF(text));

  // Must be called before the current thread exits; might as well do it here.
  g_java_vm->DetachCurrentThread();

  return false;
}

#define SMALL_BANNER_HEIGHT_WIDTH 24
#define LARGE_BANNER_HEIGHT_WIDTH SMALL_BANNER_HEIGHT_WIDTH * 2

static inline u32 Average32(u32 a, u32 b)
{
  return ((a >> 1) & 0x7f7f7f7f) + ((b >> 1) & 0x7f7f7f7f);
}

static inline u32 GetPixel(u32* buffer, unsigned int x, unsigned int y)
{
  // thanks to unsignedness, these also check for <0 automatically.
  if (x > 191)
    return 0;
  if (y > 63)
    return 0;
  return buffer[y * 192 + x];
}


static std::string GetFileType(std::string filename)
{
    __android_log_print(ANDROID_LOG_WARN, CITRA_TAG, "Getting FileType for file: %s",
                        filename.c_str());

    std::unique_ptr<Loader::AppLoader> loader = Loader::GetLoader(filename);
    return Loader::GetFileTypeString(loader->GetFileType());
}

std::string GetStringFromU16Array(u16 *in, int size) {
    char* str = (char *) (intptr_t) in;

    std::string out;
    for (int i =0; i< size; i++) {
        out += str++;
    }
    return out;
}

Loader::SMDH GetSMDH(std::string filename) {
    std::unique_ptr<Loader::AppLoader> loader = Loader::GetLoader(filename);
//    if (!loader)
// FIXME       return nullptr;

    std::vector<u8> smdh_data;
    loader->ReadIcon(smdh_data);
//    if (!Loader::IsValidSMDH(smdh_data)) {
// FIXME       return nullptr;
//    }

    Loader::SMDH smdh;
    memcpy(&smdh, smdh_data.data(), sizeof(Loader::SMDH));

    return smdh;
}

static std::string GetShortTitle(std::string filename)
{
  __android_log_print(ANDROID_LOG_WARN, CITRA_TAG, "Getting ShortTitle for file: %s",
                      filename.c_str());

    Loader::SMDH::TitleLanguage language = Loader::SMDH::TitleLanguage::English;

    Loader::SMDH smdh = GetSMDH(filename);

    std::array<u16, 0x40> shortTitle = smdh.GetShortTitle(language);

    return GetStringFromU16Array(shortTitle.data(), shortTitle.size());
}

static std::string GetLongTitle(std::string filename)
{
    __android_log_print(ANDROID_LOG_WARN, CITRA_TAG, "Getting LongTitle for file: %s",
                        filename.c_str());

    Loader::SMDH::TitleLanguage language = Loader::SMDH::TitleLanguage::English;

    Loader::SMDH smdh = GetSMDH(filename);

    std::array<u16, 0x80> longTitle = smdh.GetLongTitle(language);

    return GetStringFromU16Array(longTitle.data(), longTitle.size());
}

static std::string GetProgramId(std::string filename)
{
  __android_log_print(ANDROID_LOG_WARN, CITRA_TAG, "Getting ID for file: %s", filename.c_str());

  std::unique_ptr<Loader::AppLoader> loader = Loader::GetLoader(filename);
  if (!loader)
      return std::string("");

  u64 program_id = 0;
  loader->ReadProgramId(program_id);

  return std::to_string(program_id);
}

static std::string GetPublisher(std::string filename)
{
  __android_log_print(ANDROID_LOG_WARN, CITRA_TAG, "Getting Publisher for file: %s",
                      filename.c_str());

  Loader::SMDH::TitleLanguage language = Loader::SMDH::TitleLanguage::English;

  Loader::SMDH smdh = GetSMDH(filename);
  std::array<u16, 0x40> publisher = smdh.GetPublisher(language);

  return GetStringFromU16Array(publisher.data(), publisher.size());
}

static std::string GetJString(JNIEnv* env, jstring jstr)
{
  std::string result = "";
  if (!jstr)
    return result;

  const char* s = env->GetStringUTFChars(jstr, nullptr);
  result = s;
  env->ReleaseStringUTFChars(jstr, s);
  return result;
}

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_UnPauseEmulation(JNIEnv* env,
                                                                                     jobject obj);
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_PauseEmulation(JNIEnv* env,
                                                                                   jobject obj);
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_StopEmulation(JNIEnv* env,
                                                                                  jobject obj);
JNIEXPORT jboolean JNICALL Java_org_citraemu_citraemu_NativeLibrary_onGamePadEvent(
    JNIEnv* env, jobject obj, jstring jDevice, jint Button, jint Action);
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_onGamePadMoveEvent(
    JNIEnv* env, jobject obj, jstring jDevice, jint Axis, jfloat Value);
JNIEXPORT jintArray JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetIcon(JNIEnv* env,
                                                                                   jobject obj,
                                                                                   jstring jFile,
                                                                                   jboolean jLarge);
JNIEXPORT jstring JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetShortTitle(JNIEnv* env,
                                                                                jobject obj,
                                                                                jstring jFilename);
JNIEXPORT jstring JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetLongTitle(JNIEnv* env,
                                                                                 jobject obj,
                                                                                 jstring jFilename);
JNIEXPORT jstring JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetProgramId(JNIEnv* env,
                                                                                 jobject obj,
                                                                                 jstring jFilename);
JNIEXPORT jstring JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetPublisher(
    JNIEnv* env, jobject obj, jstring jFilename);
JNIEXPORT jstring JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetFileType(JNIEnv* env,
                                                                                jobject obj,
                                                                                jstring jFilename);
JNIEXPORT jstring JNICALL
Java_org_citraemu_citraemu_NativeLibrary_GetVersionString(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_eglBindAPI(JNIEnv* env,
                                                                               jobject obj,
                                                                               jint api);
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_SetFilename(JNIEnv* env,
                                                                                jobject obj,
                                                                                jstring jFile);
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_CreateUserFolders(JNIEnv* env,
                                                                                      jobject obj);
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_SetUserDirectory(
    JNIEnv* env, jobject obj, jstring jDirectory);
JNIEXPORT jstring JNICALL
Java_org_citraemu_citraemu_NativeLibrary_GetUserDirectory(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL
Java_org_citraemu_citraemu_NativeLibrary_CacheClassesAndMethods(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_Run(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_SurfaceChanged(JNIEnv* env,
                                                                                   jobject obj,
                                                                                   jobject _surf);
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_SurfaceDestroyed(JNIEnv* env,
                                                                                     jobject obj);

JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_UnPauseEmulation(JNIEnv* env,
                                                                                     jobject obj)
{
  std::lock_guard<std::mutex> guard(s_host_identity_lock);
//  emu_thread->SetRunning(true);
}
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_PauseEmulation(JNIEnv* env,
                                                                                   jobject obj)
{
  std::lock_guard<std::mutex> guard(s_host_identity_lock);
//  emu_thread->SetRunning(false);
}

JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_StopEmulation(JNIEnv* env,
                                                                                  jobject obj)
{
  std::lock_guard<std::mutex> guard(s_host_identity_lock);
//  emu_thread->RequestStop();

  // Wait for emulation thread to complete and delete it
//  emu_thread->wait();
//  emu_thread = nullptr;
}
JNIEXPORT jboolean JNICALL Java_org_citraemu_citraemu_NativeLibrary_onGamePadEvent(
    JNIEnv* env, jobject obj, jstring jDevice, jint Button, jint Action)
{
  return ButtonManager::GamepadEvent(GetJString(env, jDevice), Button, Action);
}
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_onGamePadMoveEvent(
    JNIEnv* env, jobject obj, jstring jDevice, jint Axis, jfloat Value)
{
  ButtonManager::GamepadAxisEvent(GetJString(env, jDevice), Axis, Value);
}

JNIEXPORT jintArray JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetIcon(JNIEnv* env,
                                                                                   jobject obj,
                                                                                   jstring jFilename,
                                                                                   jboolean jLarge)
{
  std::string filename = GetJString(env, jFilename);
  bool large = (bool)(large == JNI_TRUE);
  Loader::SMDH smdh = GetSMDH(filename);

  std::vector<u16> icon_data = smdh.GetIcon(large); //FIXME copy this to Banner
  jintArray Banner = env->NewIntArray(LARGE_BANNER_HEIGHT_WIDTH * LARGE_BANNER_HEIGHT_WIDTH);
  return Banner;
}

JNIEXPORT jstring JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetShortTitle(JNIEnv* env,
                                                                                jobject obj,
                                                                                jstring jFilename)
{
  std::string filename = GetJString(env, jFilename);
  std::string name = GetShortTitle(filename);
  return env->NewStringUTF(name.c_str());
}

JNIEXPORT jstring JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetLongTitle(JNIEnv* env,
                                                                                 jobject obj,
                                                                                 jstring jFilename)
{
    std::string filename = GetJString(env, jFilename);
    std::string name = GetLongTitle(filename);
    return env->NewStringUTF(name.c_str());
}

JNIEXPORT jstring JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetProgramId(JNIEnv* env,
                                                                                 jobject obj,
                                                                                 jstring jFilename)
{
  std::string filename = GetJString(env, jFilename);
  std::string id = GetProgramId(filename);
  return env->NewStringUTF(id.c_str());
}

JNIEXPORT jstring JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetPublisher(JNIEnv* env,
                                                                                  jobject obj,
                                                                                  jstring jFilename)
{
  std::string filename = GetJString(env, jFilename);
  std::string company = GetPublisher(filename);
  return env->NewStringUTF(company.c_str());
}

JNIEXPORT jstring JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetFileType(JNIEnv* env,
                                                                                jobject obj,
                                                                                jstring jFilename)
{
    std::string filename = GetJString(env, jFilename);
    std::string name = GetFileType(filename);
    return env->NewStringUTF(name.c_str());
}

JNIEXPORT jstring JNICALL Java_org_citraemu_citraemu_NativeLibrary_GetVersionString(JNIEnv* env,
                                                                                        jobject obj)
{
  char buffer [100];
  snprintf(buffer, 100, "%s| %s-%s",Common::g_build_name, Common::g_scm_branch, Common::g_scm_desc);
  return env->NewStringUTF(buffer);
}

JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_eglBindAPI(JNIEnv* env,
                                                                               jobject obj,
                                                                               jint api)
{
 // eglBindAPI(api);
}

JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_CreateUserFolders(JNIEnv* env,
                                                                                      jobject obj)
{
  FileUtil::CreateFullPath(FileUtil::GetUserPath(D_CONFIG_IDX));
  FileUtil::CreateFullPath(FileUtil::GetUserPath(D_CACHE_IDX));
  FileUtil::CreateFullPath(FileUtil::GetUserPath(D_SDMC_IDX));
  FileUtil::CreateFullPath(FileUtil::GetUserPath(D_NAND_IDX));
  FileUtil::CreateFullPath(FileUtil::GetUserPath(D_SYSDATA_IDX));
  FileUtil::CreateFullPath(FileUtil::GetUserPath(D_LOGS_IDX));
}

JNIEXPORT void JNICALL
Java_org_citraemu_citraemu_NativeLibrary_CacheClassesAndMethods(JNIEnv* env, jobject obj)
{
  // This class reference is only valid for the lifetime of this method.
  jclass localClass = env->FindClass("org/citraemu/citraemu/NativeLibrary");

  // This reference, however, is valid until we delete it.
  g_jni_class = reinterpret_cast<jclass>(env->NewGlobalRef(localClass));

  // TODO Find a place for this.
  // So we don't leak a reference to NativeLibrary.class.
  // env->DeleteGlobalRef(g_jni_class);

  // Method signature taken from javap -s
  // Source/Android/app/build/intermediates/classes/arm/debug/org/citraemu/citraemu/NativeLibrary.class
  g_jni_method_alert =
      env->GetStaticMethodID(g_jni_class, "displayAlertMsg", "(Ljava/lang/String;)V");
  g_jni_method_end = env->GetStaticMethodID(g_jni_class, "endEmulationActivity", "()V");
}

// Surface Handling
JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_SurfaceChanged(JNIEnv* env,
                                                                                   jobject obj,
                                                                                   jobject _surf)
{
  surf = ANativeWindow_fromSurface(env, _surf);
  if (surf == nullptr)
    __android_log_print(ANDROID_LOG_ERROR, CITRA_TAG, "Error: Surface is null.");

//  if (g_renderer)
//    g_renderer->ChangeSurface(surf);
}

JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_SurfaceDestroyed(JNIEnv* env,
                                                                                     jobject obj)
{
//  if (g_renderer)
//    g_renderer->ChangeSurface(nullptr);

  if (surf)
  {
    ANativeWindow_release(surf);
    surf = nullptr;
  }
}

JNIEXPORT void JNICALL Java_org_citraemu_citraemu_NativeLibrary_Run(JNIEnv* env, jobject obj)
{
  __android_log_print(ANDROID_LOG_INFO, CITRA_TAG, "Running : %s", g_filename.c_str());

  // Install our callbacks
//  OSD::AddCallback(OSD::CallbackType::Initialization, ButtonManager::Init);
//  OSD::AddCallback(OSD::CallbackType::Shutdown, ButtonManager::Shutdown);

  std::unique_lock<std::mutex> guard(s_host_identity_lock);

  // Create and start the emulation thread
//FIXME  emu_thread = std::make_unique<EmuThread>(nullptr);

  //emu_thread->start();

  guard.unlock();

  if (surf)
  {
    ANativeWindow_release(surf);
    surf = nullptr;
  }

  // Execute the Java method.
  env->CallStaticVoidMethod(g_jni_class, g_jni_method_end);
}

#ifdef __cplusplus
}
#endif
