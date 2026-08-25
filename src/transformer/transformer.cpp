// Copyright (c) Lefraudeur. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2

#include "transformer.hpp"
#include "../logger/logger.hpp"
#include <memory>
#include <fstream>
#include <set>

static void ClassFileLoadHook_callback(jvmtiEnv* jvmti_env, JNIEnv* jni_env,
	jclass class_being_redefined, jobject loader, const char* name,
	jobject protection_domain,
	jint class_data_len, const unsigned char* class_data,
	jint* new_class_data_len, unsigned char** new_class_data)
{
	if (!name) name = "null";
	logger::log(std::string("CFLH enter: ") + name);

	jni::set_thread_env(jni_env);

	jni::frame frame{};

	maps::PatcherHelper PatcherHelperClass{};

	maps::ClassModifier classModifier = PatcherHelperClass.getClassModifier(maps::Class(class_being_redefined));
	if (!classModifier)
	{
		logger::log(std::string("CFLH no modifier: ") + name);
		return;
	}
	logger::log(std::string("CFLH modifier found: ") + name);

	jni::array<jbyte> class_bytes = jni::array<jbyte>::create(std::vector<jbyte>((jbyte*)class_data, (jbyte*)(class_data)+class_data_len));
	if (!class_bytes)
	{
		logger::error("failed to create original class byte array");
		return;
	}

	jni::array<jbyte> transformed_class_bytes = classModifier.patch(class_bytes);
	if (jni_env->ExceptionCheck())
	{
		jni_env->ExceptionDescribe();
		jni_env->ExceptionClear();
	}
	if (!transformed_class_bytes)
	{
		logger::error(std::string("failed to patch class bytes: ") + name);
		return;
	}
	logger::log(std::string("CFLH patch success: ") + name);

	std::vector<jbyte> transformed = transformed_class_bytes.to_vector();
	
	unsigned char* transformed_class_bytes_jvmti = nullptr;
	jvmti_env->Allocate(transformed.size(), &transformed_class_bytes_jvmti);
	memcpy(transformed_class_bytes_jvmti, transformed.data(), transformed.size());

	*new_class_data_len = transformed.size();
	*new_class_data = transformed_class_bytes_jvmti;
	return;
}

static bool retransform_classes(jvmtiEnv* env)
{
	jni::frame frame{};

	maps::PatcherHelper PatcherHelperClass{};

	std::vector<maps::Class> to_retransform = PatcherHelperClass.getClassesToTransform().to_vector();
	std::unique_ptr<jclass[]> to_retransform_jclasses = std::make_unique<jclass[]>(to_retransform.size());
	for (int i = 0; i < to_retransform.size(); ++i)
		to_retransform_jclasses[i] = jclass(to_retransform[i]);

	jvmtiError status = env->RetransformClasses(to_retransform.size(), to_retransform_jclasses.get());
	if (status != JVMTI_ERROR_NONE)
	{
		const char* error = "jvmti unknown error";
		env->GetErrorName(status, (char**)&error);
		logger::error(error);
		return false;
	}

	return true;
}

bool transformer::init(const jvmti& jvmti_instance, const maps::MemoryJarClassLoader& classLoader)
{
	// we need to manually give meta jni jclass instances, and they have to be global, because they will be used from a different thread
	jclass PatcherHelper_jclass = classLoader.loadClass(maps::String::create(jni::to_dot<maps::PatcherHelper::get_name()>()));
	if (!PatcherHelper_jclass)
	{
		logger::error("failed to load PatcherHelperClass");
		return false;
	}
	PatcherHelper_jclass = (jclass)jni::get_env()->NewGlobalRef(PatcherHelper_jclass);
	jni::jclass_cache<maps::PatcherHelper>::value = PatcherHelper_jclass;

	jclass ClassModifier_jclass = classLoader.loadClass(maps::String::create(jni::to_dot<maps::ClassModifier::get_name()>()));
	if (!ClassModifier_jclass)
	{
		logger::error("failed to load ClassModifier");
		return false;
	}
	ClassModifier_jclass = (jclass)jni::get_env()->NewGlobalRef(ClassModifier_jclass);
	jni::jclass_cache<maps::ClassModifier>::value = ClassModifier_jclass;

	maps::PatcherHelper PatcherHelperClass{};
	if (!PatcherHelperClass.init())
	{
		logger::error("failed to init PatcherHelper");
		return false;
	}

	// Pre-define helper classes (Canceler, Thrower, EventDispatcher) into all
	// target classloaders using JNI DefineClass, which bypasses Java module
	// restrictions that block reflective ClassLoader.defineClass on JDK 17+.
	{
		jni::array<jni::array<jbyte>> bytesList = PatcherHelperClass.getHelperClassBytes();
		jni::array<maps::String> names = PatcherHelperClass.getHelperClassNames();
		jni::array<maps::ClassLoader> loaders = PatcherHelperClass.getTargetClassLoaders();

		if (bytesList && names && loaders)
		{
			std::vector<jni::array<jbyte>> bytesVec = bytesList.to_vector();
			std::vector<maps::String> nameVec = names.to_vector();
			std::vector<maps::ClassLoader> loaderVec = loaders.to_vector();

			std::set<jobject> seen;
			for (auto& loader : loaderVec)
			{
				if (!loader || !seen.emplace((jobject)loader).second) continue;
				for (size_t i = 0; i < bytesVec.size() && i < nameVec.size(); i++)
				{
					if (!bytesVec[i]) { logger::error(std::string("predef null bytes: ") + nameVec[i].to_string()); continue; }

					std::vector<jbyte> byteVec = bytesVec[i].to_vector();
					if (byteVec.empty()) { logger::error(std::string("predef empty bytes: ") + nameVec[i].to_string()); continue; }

					std::string jniName = nameVec[i].to_string();
					for (auto& c : jniName) if (c == '.') c = '/';

					jclass defined = jni::get_env()->DefineClass(jniName.c_str(), loader, (jbyte*)byteVec.data(), (jsize)byteVec.size());
					if (jni::get_env()->ExceptionCheck())
					{
						logger::error(std::string("predef failed: ") + jniName);
						jni::get_env()->ExceptionClear();
					}
					else if (defined)
					{
						logger::log(std::string("predef success: ") + jniName);
					}
					else
					{
						logger::error(std::string("predef null result: ") + jniName);
					}
				}
			}
			// Verify EventDispatcher is resolvable via the target classloader
			if (!loaderVec.empty()) {
				maps::ClassLoader firstLoader = loaderVec[0];
				maps::String edName = maps::String::create("com.emberinjector.framework.client.EventDispatcher");
				maps::Class edClass = firstLoader.loadClass(edName);
				if (edClass) {
					logger::log("loader.loadClass EventDispatcher OK");
				} else {
					logger::error("loader.loadClass EventDispatcher FAILED");
				}
			}
		}
	}

	jvmtiEnv* env = jvmti_instance.get_env();

	jvmtiCapabilities cap{};
	cap.can_retransform_classes = JVMTI_ENABLE;
	if (env->AddCapabilities(&cap) != JVMTI_ERROR_NONE)
	{
		logger::error("Retransform classes not supported");
		return false;
	}

	jvmtiEventCallbacks callbacks{};
	callbacks.ClassFileLoadHook = ClassFileLoadHook_callback;
	if (env->SetEventCallbacks(&callbacks, sizeof(jvmtiEventCallbacks)) != JVMTI_ERROR_NONE)
	{
		logger::error("could not set event callback");
		return false;
	}

	if (env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, nullptr) != JVMTI_ERROR_NONE)
	{
		logger::error("failed to enable event");
		return false;
	}

	if (!retransform_classes(env))
		return false;

	return true;
}

void transformer::shutdown(const jvmti& jvmti_instance)
{
	jvmtiEnv* env = jvmti_instance.get_env();

	env->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, nullptr);
	jvmtiEventCallbacks callbacks{};
	env->SetEventCallbacks(&callbacks, sizeof(jvmtiEventCallbacks));

	retransform_classes(env);

	jni::get_env()->DeleteGlobalRef(jni::jclass_cache<maps::PatcherHelper>::value);
	jni::get_env()->DeleteGlobalRef(jni::jclass_cache<maps::ClassModifier>::value);

	jvmtiCapabilities cap{};
	cap.can_retransform_classes = JVMTI_ENABLE;
	env->RelinquishCapabilities(&cap);
	return;
}